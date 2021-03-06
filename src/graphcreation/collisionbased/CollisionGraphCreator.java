/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphcreation.collisionbased;

import graphcreation.GraphCreationListener;
import graphcreation.GraphCreator;
import graphcreation.collisionbased.message.CollisionResponseMessage;
import graphcreation.collisionbased.message.ConnectServicesMessage;
import graphcreation.collisionbased.message.DisconnectServicesMessage;
import graphcreation.collisionbased.message.ForwardMessage;
import graphcreation.collisionbased.message.RemovedServicesMessage;
import graphcreation.collisionbased.sdg.NonLocalServiceException;
import graphcreation.collisionbased.sdg.SDG;
import graphcreation.collisionbased.sdg.sdgtaxonomy.IndirectRoute;
import graphcreation.collisionbased.sdg.sdgtaxonomy.SDGTaxonomy;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.MulticastMessageListener;
import multicast.ParameterSearch;
import multicast.ParameterSearchListener;
import multicast.Util;
import multicast.search.ParameterSearchImpl;
import multicast.search.Route;
import multicast.search.message.SearchResponseMessage;
import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.peerid.PeerID;
import taxonomy.Taxonomy;
import taxonomy.parameter.Parameter;
import util.logger.Logger;
import dissemination.DistanceChange;
import dissemination.TableChangedListener;

/**
 * This class implements the graph creation layer. It detects collision of
 * parameters and connects the corresponding services creating a SDG.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class CollisionGraphCreator implements CommunicationLayer, ParameterSearchListener, GraphCreator, TableChangedListener {

	private static final int MAX_PATHS = 2;

	// the communication layer
	private final Peer peer;

	// the lower layer which provides routing functionality
	private final ParameterSearch pSearch;

	// parameter references count
	private final Map<Parameter, Integer> pReferences = new HashMap<Parameter, Integer>();

	// listener for upper layers
	private final MulticastMessageListener mMessageListener;

	// listener for upper layers
	private final GraphCreationListener graphCreationListener;
	
	private final CollisionNode collisionNode;
	
	private final Map<MessageID, Set<Service>> foundServices = new HashMap<MessageID, Set<Service>>();

	// the local service table
	private SDG sdg;
	
	private boolean enabled = true;

	private final Logger logger = Logger.getLogger(CollisionGraphCreator.class);

	/**
	 * Constructor of the class
	 * 
	 * @param peer
	 *            the communication peer
	 * 
	 */
	public CollisionGraphCreator(final ReliableBroadcastPeer peer, final MulticastMessageListener mMessageListener, final GraphCreationListener graphCreationListener, final GraphType graphType) {
		this.peer = peer;
		this.pSearch = new ParameterSearchImpl(peer, this, this);
		this.collisionNode = new CollisionNode(peer, this, graphCreationListener, graphType);
		this.mMessageListener = mMessageListener;
		this.graphCreationListener = graphCreationListener;

		try {
			peer.addCommunicationLayer(this, new HashSet<Class<? extends BroadcastMessage>>());
		} catch (final RegisterCommunicationLayerException e) {
			logger.error("Peer " + peer.getPeerID() + " had problem registering communication layer: " + e.getMessage());
		}
	}
	
	public void setDisabled() {
		enabled = false;
	}
	
	class DistanceComparator implements Comparator<Entry<PeerID, Integer>> {

		@Override
		public int compare(Entry<PeerID, Integer> entryA, Entry<PeerID, Integer> entryB) {
			return entryA.getValue().intValue() - entryB.getValue().intValue();
		}
	}
	
	private Map<PeerID, List<Route>> obtainMaxPaths(final Set<Service> destServices, final int maxPaths) {
		final Map<PeerID, List<Route>> routes = new HashMap<PeerID, List<Route>>();
		final Set<PeerID> destPeers = getPeers(destServices);
		synchronized (sdg) {
			for (final PeerID dest : destPeers) {
				final LinkedList<Route> lList = new LinkedList<Route>(sdg.getDirectRoutes(dest));
				lList.addAll(sdg.getIndirectRoutes(dest));
				Collections.sort(lList, Util.distanceComparator);
				while (lList.size() > maxPaths)
					lList.removeLast();
				
				if (!lList.isEmpty())
					routes.put(dest, lList);
			}
		}
		return routes;
	}
	
	private Set<PeerID> getPeers(final Set<Service> services) {
		final Set<PeerID> peers = new HashSet<PeerID>();
		for (final Service service : services)
			peers.add(service.getPeerID());
		return peers;
	}
	
	@Override
	public void forwardMessage(final BroadcastMessage payload, final Set<Service> destinations, final boolean directBroadcast, final boolean multiplePaths) {		
		logger.debug("Peer " + peer.getPeerID() + " forwarding " + payload.getType() + " to " + destinations);
	
		final int maxPaths = multiplePaths?MAX_PATHS:1;
		final Map<PeerID, List<Route>> routes = obtainMaxPaths(destinations, maxPaths);
		logger.trace("Peer " + peer.getPeerID() + " routes " + routes);
		for (final Entry<PeerID, List<Route>> entry : routes.entrySet()) {
			final PeerID dest = entry.getKey();
			for (final Route route : entry.getValue()) {
				if (route instanceof IndirectRoute) {
					final IndirectRoute indirectRoute = (IndirectRoute)route;
					logger.trace("Peer " + peer.getPeerID() + " forwarding message to " + dest + " through " + indirectRoute.getThrough());
					pSearch.sendMulticastMessage(Collections.singleton(indirectRoute.getThrough()), new ForwardMessage(peer.getPeerID(), payload, Collections.singleton(dest)), directBroadcast);
				} else {
					logger.trace("Peer " + peer.getPeerID() + " multicasting to " + route.getDest());
					pSearch.sendMulticastMessage(Collections.singleton(route.getDest()), payload, directBroadcast);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * graphcreation.collisiondetection.GCreation#manageLocalServices(graphcreation
	 * .services.ServiceList, graphcreation.services.ServiceList)
	 */
	@Override
	public void manageLocalServices(final ServiceList locallyAddedServices, final ServiceList locallyRemovedServices) {
		final Set<Service> addedServices = new HashSet<Service>(locallyAddedServices.getServiceList());
		final Set<Service> removedServices = new HashSet<Service>(locallyRemovedServices.getServiceList());
		
		addedServices.remove(removedServices);
			
		final Set<PeerID> collisionPeers = new HashSet<PeerID>();
	
		synchronized (sdg) {			
			// Obtain those remote services connected with the removed ones
			for (final Service service : removedServices) {
				for (final Entry<MessageID, Set<Service>> entry : foundServices.entrySet()) {
					if (entry.getValue().contains(service))
						collisionPeers.add(entry.getKey().getPeer());
				}
			}
		}

		try {
			commit(addedServices, removedServices);
		} catch (NonLocalServiceException e) {
			
		}
		
		if (!collisionPeers.isEmpty())
			sendRemoveServicesMessage(removedServices, collisionPeers);
	}
	
	private void commit(Set<Service> addedServices, Set<Service> removedServices) throws NonLocalServiceException{
		if (!addedServices.isEmpty())
			logger.debug("Peer " + peer.getPeerID() + " adding local services " + addedServices);
		
		if (!removedServices.isEmpty())
			logger.debug("Peer " + peer.getPeerID() + " removing local services " + removedServices);
		
		final Map<Service, Set<ServiceDistance>> newSuccessors = new HashMap<Service, Set<ServiceDistance>>();
		final Map<Service, Set<ServiceDistance>> newAncestors = new HashMap<Service, Set<ServiceDistance>>();
		
		for (final Service addedService : addedServices) {
			if (addedService.isLocal(peer.getPeerID()) && !sdg.contains(addedService)) {
				// Increment parameter references
				for (final Parameter parameter : addedService.getParameters()) {
					incReference(parameter);
	
					// Is the first addition of the parameter
					if (refCount(parameter) == 1)
						pSearch.addLocalParameter(parameter);
				}
	
				synchronized (sdg) {
					sdg.addLocalService(addedService);
					
					final Set<ServiceDistance> successors = sdg.getSuccessors(addedService);
					if (!successors.isEmpty())
						newSuccessors.put(addedService, successors);

					final Set<ServiceDistance> ancestors = sdg.getAncestors(addedService);
					if (!ancestors.isEmpty())
						newAncestors.put(addedService, ancestors);
					
					for (final ServiceDistance localSuccessor : sdg.getLocalSuccessors(addedService)) {
						if (!newAncestors.containsKey(localSuccessor.getService()))
							newAncestors.put(localSuccessor.getService(), new HashSet<ServiceDistance>());
						newAncestors.get(localSuccessor.getService()).add(new ServiceDistance(addedService, new Integer(0)));
					}
					
					for (final ServiceDistance localAncestor : sdg.getLocalAncestors(addedService)) {
						if (!newSuccessors.containsKey(localAncestor.getService()))
							newSuccessors.put(localAncestor.getService(), new HashSet<ServiceDistance>());
						newSuccessors.get(localAncestor.getService()).add(new ServiceDistance(addedService, new Integer(0)));
					}
				}
			}
		}
		
		synchronized (sdg) {
			for (final Service removedService : removedServices)
				if (containsLocalService(removedService)) {
					for (final Parameter parameter : removedService.getParameters()) {
						decReference(parameter);
	
						// Only remove parameter if reference count is 0
						if (refCount(parameter) == 0)
							pSearch.removeLocalParameter(parameter);
					}
	
					sdg.removeLocalService(removedService);
				}
		}
		
		pSearch.commit();
		
		if (!newSuccessors.isEmpty())
			graphCreationListener.newSuccessors(newSuccessors);
		
		if (!newAncestors.isEmpty())
			graphCreationListener.newAncestors(newAncestors);
	}

	private void incReference(final Parameter p) {
		if (!pReferences.containsKey(p))
			pReferences.put(p, Integer.valueOf(0));

		final Integer currentValue = pReferences.get(p);
		pReferences.put(p, Integer.valueOf(currentValue.intValue() + 1));
	}

	private int refCount(final Parameter p) {
		if (!(pReferences.containsKey(p)))
			return 0;

		return pReferences.get(p).intValue();
	}

	private void decReference(final Parameter p) {
		if (!pReferences.containsKey(p))
			return;

		final Integer currentValue = pReferences.get(p);
		pReferences.put(p, Integer.valueOf(currentValue.intValue() - 1));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphcreation.collisiondetection.GCreation#getPSearch()
	 */
	@Override
	public ParameterSearch getPSearch() {
		return pSearch;
	}

	@Override
	public void init() {
		this.sdg = new SDGTaxonomy(peer.getPeerID(), pSearch, pSearch.getDisseminationLayer().getTaxonomy());
	}

	@Override
	public void messageReceived(final BroadcastMessage m, final long receptionTime) {
	}

	private void processConnectServicesMessage(final ConnectServicesMessage connectServicesMessage, final PeerID collDetectionNode) {
		logger.trace("Peer " + peer.getPeerID() + " connecting compatible services RS: " + connectServicesMessage.getRemoteSuccessors() + " RA: " + connectServicesMessage.getRemoteAncestors() + " thanks to collision detected in peer " + connectServicesMessage.getSource());

		traceSDG();
		
		final Map<Service, Set<ServiceDistance>> newSuccessors = new HashMap<Service, Set<ServiceDistance>>();
		final Map<Service, Set<ServiceDistance>> newAncestors = new HashMap<Service, Set<ServiceDistance>>();

		synchronized (sdg) {
			connectRemoteServices(connectServicesMessage.getRemoteSuccessors(), collDetectionNode, newSuccessors, newAncestors);
			connectRemoteServices(connectServicesMessage.getRemoteAncestors(), collDetectionNode, newSuccessors, newAncestors);
		}

		traceSDG();

		if (!newSuccessors.isEmpty())
			graphCreationListener.newSuccessors(newSuccessors);
		
		if (!newAncestors.isEmpty())
			graphCreationListener.newAncestors(newAncestors);
	}

	private void connectRemoteServices(final Map<Service, Set<ServiceDistance>> detectedConnections, final PeerID collDetectionNode, final Map<Service, Set<ServiceDistance>> newSuccessors, final Map<Service, Set<ServiceDistance>> newAncestors) {
		final Set<ServiceDistance> addedServices = new HashSet<ServiceDistance>();
		
		for (final Iterator<Entry<Service, Set<ServiceDistance>>> it = detectedConnections.entrySet().iterator(); it.hasNext();) {
			final Entry<Service, Set<ServiceDistance>> entry = it.next();
			final Service service = entry.getKey();
			if (containsLocalService(service))
				addedServices.addAll(sdg.connectServices(service, entry.getValue(), collDetectionNode));
		}
			
		for (final ServiceDistance addedService : addedServices) {
			for (final ServiceDistance ancestor : sdg.getLocalAncestors(addedService.getService())) {
				if (!newSuccessors.containsKey(ancestor.getService()))
					newSuccessors.put(ancestor.getService(), new HashSet<ServiceDistance>());
				newSuccessors.get(ancestor.getService()).add(addedService);
			}
			
			for (final ServiceDistance successor : sdg.getLocalSuccessors(addedService.getService())) {
				if (!newAncestors.containsKey(successor.getService()))
					newAncestors.put(successor.getService(), new HashSet<ServiceDistance>());
				newAncestors.get(successor.getService()).add(addedService);
			}
		}
	}

	private void traceSDG() {
		String currentSDG;
		
		synchronized (sdg) {
			currentSDG = sdg.toString();
		}
		
		logger.trace("Peer " + peer.getPeerID() + " SDG" + currentSDG);
	}

	private void processDisconnectServicesMessage(final DisconnectServicesMessage disconnectServicesMessage) {
		logger.trace("Peer " + peer.getPeerID() + " disconnecting services " + disconnectServicesMessage.getLostServices() + " connected through collision detected in peer " + disconnectServicesMessage.getSource());

		final Map<Service, Set<Service>> lostSuccessors = new HashMap<Service, Set<Service>>();
		final Map<Service, Set<Service>> lostAncestors = new HashMap<Service, Set<Service>>();
		
		traceSDG();
		
		Set<Service> lostServices = new HashSet<Service>();

		synchronized (sdg) {
			if (disconnectServicesMessage.wereServicesRemoved())
				lostServices.addAll(disconnectServicesMessage.getLostServices());
			else {
				sdg.removeIndirectRoute(disconnectServicesMessage.getServicesPeer(), disconnectServicesMessage.getSource());
				for (final ServiceDistance inaccesibleService : sdg.getInaccesibleServices())
					lostServices.add(inaccesibleService.getService());
			}
			
			for (final Service remoteService : lostServices)
				// test that service really exists in graph before removal
				if (sdg.contains(remoteService)) {
					// get the current ancestors and successors of the removed
					// service
					final Set<ServiceDistance> beforeAncestors = sdg.getLocalAncestors(remoteService);
					final Set<ServiceDistance> beforeSuccessors = sdg.getLocalSuccessors(remoteService);
	
					sdg.removeRemoteService(remoteService);
	
					for (final ServiceDistance ancestor : beforeAncestors) {
						if (!lostSuccessors.containsKey(ancestor.getService()))
							lostSuccessors.put(ancestor.getService(), new HashSet<Service>());
						lostSuccessors.get(ancestor.getService()).add(remoteService);
					}
	
					for (final ServiceDistance successor : beforeSuccessors) {
						if (!lostAncestors.containsKey(successor.getService()))
							lostAncestors.put(successor.getService(), new HashSet<Service>());
						lostAncestors.get(successor.getService()).add(remoteService);
					}
				}
		}
		
		if (!lostAncestors.isEmpty())
			graphCreationListener.lostAncestors(lostAncestors);

		if (!lostSuccessors.isEmpty())
			graphCreationListener.lostSuccessors(lostSuccessors);

		traceSDG();
	}

	private void processForwardMessage(final ForwardMessage forwardMessage, final int distance, boolean directBroadcast) {
		logger.trace("Peer " + peer.getPeerID() + " accepted a forward message from " + forwardMessage.getSource() + " to " + forwardMessage.getDestinations());

		// get destinations from services
		pSearch.sendMulticastMessage(forwardMessage.getDestinations(), forwardMessage.getPayload(), distance, directBroadcast);
	}

	@Override
	public void multicastMessageAccepted(final PeerID source, final BroadcastMessage payload, final int distance, final boolean directBroadcast) {
		if (!enabled)
			return;
		
		if (payload instanceof ConnectServicesMessage)
			processConnectServicesMessage((ConnectServicesMessage) payload, source);
		else if (payload instanceof DisconnectServicesMessage)
			processDisconnectServicesMessage((DisconnectServicesMessage) payload);
		else if (payload instanceof RemovedServicesMessage)
			collisionNode.processRemovedServicesMessage((RemovedServicesMessage) payload);
		else if (payload instanceof ForwardMessage)
			processForwardMessage((ForwardMessage) payload, distance, directBroadcast);
		else {
			// the message must be processed by the upper layers
			mMessageListener.multicastMessageAccepted(source, payload, distance, directBroadcast);
		}
	}

	@Override
	public void parametersFound(final SearchResponseMessage message) {
		if (message.getPayload() instanceof CollisionResponseMessage) {
			logger.trace("Peer " + peer.getPeerID() + " accepted a collision response message " + message.getPayload() + " from " + message.getSource());
			collisionNode.processCollisionResponse(message);
		}
	}

	private void sendRemoveServicesMessage(final Set<Service> rServices, final Set<PeerID> notifiedPeers) {
		if (!enabled)
			return;
		
		logger.trace("Peer " + peer.getPeerID() + " sending removed services message to " + notifiedPeers + " with removed services " + rServices);
		final RemovedServicesMessage message = new RemovedServicesMessage(peer.getPeerID(), rServices);
		pSearch.sendMulticastMessage(notifiedPeers, message, false);
	}

	@Override
	public BroadcastMessage searchReceived(final Set<Parameter> foundParameters, final MessageID routeID) {
		// only messages containing a collision message as payload are valid

		Set<Service> services;
		synchronized (sdg) {
			// obtain those services which provide the searched parameters
			services = sdg.findLocalCompatibleServices(foundParameters);
		}
		
		if (!services.isEmpty()) {
			if (!foundServices.containsKey(routeID))
				foundServices.put(routeID, new HashSet<Service>());
			foundServices.get(routeID).addAll(services);
		}

		// initial distance is zero because all services are local
		final Map<Service, Byte> serviceDistanceTable = new HashMap<Service, Byte>();
		for (final Service service : services)
			serviceDistanceTable.put(service, Byte.valueOf((byte)0));

		final CollisionResponseMessage collisionResponseMessage = new CollisionResponseMessage(peer.getPeerID(), serviceDistanceTable);
		logger.trace("Peer " + peer.getPeerID() + " sending collision response with services " + serviceDistanceTable + " to " + routeID.getPeer());
		return collisionResponseMessage;
	}
	
	@Override
	public void searchCanceled(Set<MessageID> canceledSearches) {
		for (final MessageID canceledSearch : canceledSearches)
			foundServices.remove(canceledSearch);
	}

	@Override
	public void lostDestinations(Set<PeerID> lostDestinations) {
		collisionNode.checkCollisions(lostDestinations);
		
		logger.trace("Peer " + peer.getPeerID() + " checking for services connecting through lost destinations: " + lostDestinations);
		final Map<Service, Set<Service>> lostAncestors = new HashMap<Service, Set<Service>>();
		final Map<Service, Set<Service>> lostSuccessors = new HashMap<Service, Set<Service>>();
		
		synchronized (sdg) {
			sdg.checkServices(lostDestinations, lostAncestors, lostSuccessors);
		}
		
		for (final Iterator<MessageID> it = foundServices.keySet().iterator(); it.hasNext(); ) {
			final MessageID routeID = it.next();
			if (lostDestinations.contains(routeID.getPeer()))
				it.remove();
		}

		if (!lostAncestors.isEmpty())
			graphCreationListener.lostAncestors(lostAncestors);

		if (!lostSuccessors.isEmpty())
			graphCreationListener.lostSuccessors(lostSuccessors);
	}

	@Override
	public void stop() {
	}

	@Override
	public Service getService(String serviceID) {
		synchronized (sdg) {
			return sdg.getService(serviceID);
		}
	}

	@Override
	public void saveToXML(OutputStream os) throws IOException {
		synchronized (sdg) {
			sdg.saveToXML(os);
		}
	}

	@Override
	public void readFromXML(InputStream is) throws IOException {}

	@Override
	public Set<ServiceDistance> getAncestors(Service service) {
		synchronized (sdg) {
			return sdg.getAncestors(service);
		}
	}

	@Override
	public Set<ServiceDistance> getSuccessors(Service service) {
		synchronized (sdg) {
			return sdg.getSuccessors(service);
		}
	}

	public void disableMulticastLayer() {
		((ParameterSearchImpl)pSearch).setDisabled();
	}

	@Override
	public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		return false;		
	}

	@Override
	public BroadcastMessage parametersChanged(final PeerID neighbor, final Set<Parameter> newParameters, final Set<Parameter> removedParameters, 
											final Set<Parameter> removedLocalParameters, final Map<Parameter, DistanceChange> changedParameters, 
											final Set<Parameter> tableAdditions, final List<BroadcastMessage> payloadMessages) {
		return collisionNode.parametersChanged(removedParameters, changedParameters, tableAdditions, payloadMessages);
	}

	@Override
	public boolean containsLocalService(Service service) {
		return service.isLocal(peer.getPeerID()) && sdg.contains(service);
	}

	@Override
	public Taxonomy getTaxonomy() {
		return pSearch.getDisseminationLayer().getTaxonomy();
	}

	@Override
	public void neighborsChanged(Set<PeerID> newNeighbors, Set<PeerID> lostNeighbors) {
		//collisionNode.neighborsChanged(lostNeighbors);
	}
}
