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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.MulticastMessageListener;
import multicast.ParameterSearch;
import multicast.ParameterSearchListener;
import multicast.search.ParameterSearchImpl;
import multicast.search.Route;
import multicast.search.message.SearchResponseMessage;
import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.message.BroadcastMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
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
	public CollisionGraphCreator(final Peer peer, final MulticastMessageListener mMessageListener, final GraphCreationListener graphCreationListener, final GraphType graphType) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see graphcreation.collisiondetection.GCreation#forwardMessage(message.
	 * BroadcastMessage, java.util.Set)
	 */
	@Override
	public void forwardMessage(final PayloadMessage payload, final Set<Service> destinations) {		
		logger.debug("Peer " + peer.getPeerID() + " forwarding " + payload.getType() + " to " + destinations);
		final Map<PeerID, Set<PeerID>> forwardTable = new HashMap<PeerID, Set<PeerID>>();
		final Set<PeerID> directMulticast = new HashSet<PeerID>();
		
		synchronized (sdg) {
			for (final Service service : destinations) {
				//find the shortest path to reach each destination			
				final Route route = sdg.getRoute(service.getPeerID());
				if (route != null) {
					if (route instanceof IndirectRoute) {
						if (!forwardTable.containsKey(route.getThrough()))
							forwardTable.put(route.getThrough(), new HashSet<PeerID>());
						forwardTable.get(route.getThrough()).add(service.getPeerID());
					} else
						directMulticast.add(route.getDest());
				}
			}
		}

		//perform forwarding through different intermediate nodes
		for (final Entry<PeerID, Set<PeerID>> forwardEntry : forwardTable.entrySet()) {
			logger.trace("Peer " + peer.getPeerID() + " forwarding message to " + forwardEntry.getValue() + " through " + forwardEntry.getKey());
			pSearch.sendMulticastMessage(new PeerIDSet(Collections.singleton(forwardEntry.getKey())), new ForwardMessage(peer.getPeerID(), payload, forwardEntry.getValue()));
		}
		
		//perform direct multicast
		logger.trace("Peer " + peer.getPeerID() + " multicasting to " + directMulticast);
		pSearch.sendMulticastMessage(new PeerIDSet(directMulticast), payload);
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
		
		logger.debug("Peer " + peer.getPeerID() + " adding local services " + addedServices);
		logger.debug("Peer " + peer.getPeerID() + " removing local services " + removedServices);
		
		final Set<ServiceDistance> remoteConnectedServices = new HashSet<ServiceDistance>();
		final Set<PeerID> collisionPeers = new HashSet<PeerID>();
	
		synchronized (sdg) {			
			// Obtain those remote services connected with the removed ones
			for (final Service service : removedServices)
				remoteConnectedServices.addAll(sdg.getRemoteConnectedServices(service));
			
			for (final ServiceDistance sDistance : remoteConnectedServices)
				collisionPeers.addAll(sdg.getThroughCollisionNodes(sDistance.getService()));
		}

		try {
			commit(addedServices, removedServices);
		} catch (NonLocalServiceException e) {
			
		}
		
		if (!remoteConnectedServices.isEmpty())
			sendRemoveServicesMessage(removedServices, collisionPeers);
	}
	
	private void commit(Set<Service> addedServices, Set<Service> removedServices) throws NonLocalServiceException{
		final Map<Service, Set<ServiceDistance>> newSuccessors = new HashMap<Service, Set<ServiceDistance>>();
		final Map<Service, Set<ServiceDistance>> newAncestors = new HashMap<Service, Set<ServiceDistance>>();
		
		for (final Service addedService : addedServices) {
			// Increment parameter references
			for (final Parameter parameter : addedService.getParameters()) {
				incReference(parameter);

				// Is the first addition of the parameter
				if (refCount(parameter) == 1)
					pSearch.addLocalParameter(parameter);
			}

			synchronized (sdg) {
				sdg.addLocalService(addedService);
				final Set<ServiceDistance> localSuccessors = sdg.getLocalSuccessors(addedService);
				if (!localSuccessors.isEmpty())
					newSuccessors.put(addedService, localSuccessors);
				
				final Set<ServiceDistance> localAncestors = sdg.getLocalAncestors(addedService);
				if (!localAncestors.isEmpty())
					newAncestors.put(addedService, localAncestors);
			}
		}

		synchronized (sdg) {
			for (final Service removedService : removedServices)
				if (sdg.isLocal(removedService)) {
					for (final Parameter parameter : removedService.getParameters()) {
						decReference(parameter);
	
						// Only remove parameter if reference count is 0
						if (refCount(parameter) == 0)
							pSearch.removeLocalParameter(parameter);
					}
	
					sdg.removeLocalService(removedService);
				}
	
			pSearch.commit();
		}
		
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

	private void processConnectServicesMessage(final ConnectServicesMessage connectServicesMessage, final PeerID source) {
		logger.trace("Peer " + peer.getPeerID() + " connecting compatible services RS: " + connectServicesMessage.getRemoteSuccessors() + " RA: " + connectServicesMessage.getRemoteAncestors() + " thanks to collision detected in peer " + connectServicesMessage.getSource());

		traceSDG();

		synchronized (sdg) {
			for (final Iterator<Entry<Service, Set<ServiceDistance>>> it = connectServicesMessage.getRemoteSuccessors().entrySet().iterator(); it.hasNext();) {
				final Entry<Service, Set<ServiceDistance>> entry = it.next();
				final Service service = entry.getKey();
				if (sdg.isLocal(service)) {
					try {	
						sdg.connectRemoteServices(service, entry.getValue(), new HashSet<ServiceDistance>(), source);
					} catch (final NonLocalServiceException nlse) {
						logger.error("Peer " + peer.getPeerID() + " error connecting remote service. " + nlse.getMessage());
					}
				}
			}
	
			for (final Iterator<Entry<Service, Set<ServiceDistance>>> it = connectServicesMessage.getRemoteAncestors().entrySet().iterator(); it.hasNext();) {
				final Entry<Service, Set<ServiceDistance>> entry = it.next();
				final Service service = entry.getKey();
				if (sdg.isLocal(service)) {
					try {
						sdg.connectRemoteServices(service, new HashSet<ServiceDistance>(), entry.getValue(), source);
					} catch (final NonLocalServiceException nlse) {
						logger.error("Peer " + peer.getPeerID() + " error connecting remote service. " + nlse.getMessage());
					}
				}
			}
		}

		traceSDG();

		graphCreationListener.newSuccessors(connectServicesMessage.getRemoteSuccessors());
		graphCreationListener.newAncestors(connectServicesMessage.getRemoteAncestors());
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
				if (sdg.hasService(remoteService)) {
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

	private void processForwardMessage(final ForwardMessage forwardMessage, final int distance) {
		logger.trace("Peer " + peer.getPeerID() + " accepted a forward message from " + forwardMessage.getSource() + " to " + forwardMessage.getDestinations());

		// get destinations from services
		pSearch.sendMulticastMessage(new PeerIDSet(forwardMessage.getDestinations()), forwardMessage.getPayload(), distance);
	}

	@Override
	public void multicastMessageAccepted(final PeerID source, final PayloadMessage payload, final int distance) {
		if (!enabled)
			return;
		
		if (payload instanceof ConnectServicesMessage)
			processConnectServicesMessage((ConnectServicesMessage) payload, source);
		else if (payload instanceof DisconnectServicesMessage)
			processDisconnectServicesMessage((DisconnectServicesMessage) payload);
		else if (payload instanceof RemovedServicesMessage)
			collisionNode.processRemovedServicesMessage((RemovedServicesMessage) payload);
		else if (payload instanceof ForwardMessage)
			processForwardMessage((ForwardMessage) payload, distance);
		else {
			// the message must be processed by the upper layers
			mMessageListener.multicastMessageAccepted(source, payload, distance);
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
		pSearch.sendMulticastMessage(new PeerIDSet(notifiedPeers), message);
	}

	@Override
	public PayloadMessage searchReceived(final Set<Parameter> foundParameters, final PeerID source) {
		// only messages containing a collision message as payload are valid

		Set<Service> services;
		synchronized (sdg) {
			// obtain those services which provide the searched parameters
			services = sdg.findLocalCompatibleServices(foundParameters);
		}

		// initial distance is zero because all services are local
		final Map<Service, Integer> serviceDistanceTable = new HashMap<Service, Integer>();
		for (final Service service : services)
			serviceDistanceTable.put(service, Integer.valueOf(0));

		final CollisionResponseMessage collisionResponseMessage = new CollisionResponseMessage(peer.getPeerID(), serviceDistanceTable);

		logger.trace("Peer " + peer.getPeerID() + " sending collision response with services " + serviceDistanceTable + " to " + source);
		return collisionResponseMessage;
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
	public boolean isLocal(Service service) {
		synchronized (sdg) {
			return sdg.isLocal(service);
		}
	}

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
	public PayloadMessage parametersChanged(PeerID neighbor, Set<Parameter> addedParameters, Set<Parameter> removedParameters, Set<Parameter> removedLocalParameters, Map<Parameter, DistanceChange> changedParameters, List<PayloadMessage> payloadMessages) {
		return collisionNode.parametersChanged(neighbor, addedParameters, removedParameters, changedParameters, payloadMessages);
	}
}
