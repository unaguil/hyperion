package graphcreation.collisionbased;

import graphcreation.GraphCreationListener;
import graphcreation.GraphCreator;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.collisionbased.collisiondetector.CollisionDetector;
import graphcreation.collisionbased.connectionManager.Connection;
import graphcreation.collisionbased.connectionManager.ConnectionsManager;
import graphcreation.collisionbased.message.CollisionMessage;
import graphcreation.collisionbased.message.CollisionResponseMessage;
import graphcreation.collisionbased.message.ConnectServicesMessage;
import graphcreation.collisionbased.message.DisconnectServicesMessage;
import graphcreation.collisionbased.message.ForwardMessage;
import graphcreation.collisionbased.message.InhibeCollisionsMessage;
import graphcreation.collisionbased.message.RemovedServicesMessage;
import graphcreation.collisionbased.sdg.NonLocalServiceException;
import graphcreation.collisionbased.sdg.SDG;
import graphcreation.collisionbased.sdg.sdgtaxonomy.SDGTaxonomy;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.MulticastMessageListener;
import multicast.ParameterSearch;
import multicast.ParameterSearchListener;
import multicast.search.ParameterSearchImpl;
import multicast.search.message.SearchMessage;
import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;
import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.Parameter;
import util.logger.Logger;
import dissemination.DistanceChange;
import dissemination.ParameterDisseminator;
import dissemination.TableChangedListener;

/**
 * This class implements the graph creation layer. It detects collision of
 * parameters and connects the corresponding services creating a SDG.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class CollisionGraphCreator implements CommunicationLayer, TableChangedListener, ParameterSearchListener, GraphCreator {

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

	// the local service table
	private SDG sdg;

	// the connections manager
	private ConnectionsManager cManager;

	private final Logger logger = Logger.getLogger(CollisionGraphCreator.class);

	/**
	 * Constructor of the class
	 * 
	 * @param peer
	 *            the communication peer
	 * 
	 */
	public CollisionGraphCreator(final Peer peer, final MulticastMessageListener mMessageListener, final GraphCreationListener graphCreationListener) {
		this.peer = peer;
		this.pSearch = new ParameterSearchImpl(peer, this, this);
		this.mMessageListener = mMessageListener;
		this.graphCreationListener = graphCreationListener;

		try {
			peer.addCommunicationLayer(this, new HashSet<Class<? extends BroadcastMessage>>());
		} catch (final RegisterCommunicationLayerException e) {
			logger.error("Peer " + peer.getPeerID() + " had problem registering communication layer: " + e.getMessage());
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
		// Get the intermediate/collision nodes and group them
		final Map<PeerID, Set<Service>> forwardTable = new HashMap<PeerID, Set<Service>>();
		
		synchronized (sdg) {
			for (final Service service : destinations) {
				if (service.getPeerID().equals(peer.getPeerID()))
					pSearch.sendMulticastMessage(new PeerIDSet(Collections.singleton(peer.getPeerID())), payload);
				else {
					final Set<PeerID> intermediateNodes = sdg.getThroughCollisionNodes(service);
					if (!intermediateNodes.isEmpty()) {
						final PeerID intermediateNode = intermediateNodes.iterator().next();
						if (!forwardTable.containsKey(intermediateNode))
							forwardTable.put(intermediateNode, new HashSet<Service>());
						forwardTable.get(intermediateNode).add(service);
					}
				}
			}
		}

		for (final Entry<PeerID, Set<Service>> entry : forwardTable.entrySet()) {
			logger.debug("Peer " + peer.getPeerID() + " forwarding " + payload.getClass().getName());
			pSearch.sendMulticastMessage(new PeerIDSet(Collections.singleton(entry.getKey())), new ForwardMessage(peer.getPeerID(), payload, entry.getValue()));
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
		
		logger.debug("Peer " + peer.getPeerID() + " adding local services " + addedServices);
		logger.debug("Peer " + peer.getPeerID() + " removing local services " + removedServices);
		
		final Map<PeerID, Set<Service>> notifications = new HashMap<PeerID, Set<Service>>();
		
		synchronized (sdg) {
			final Map<Service, Set<ServiceDistance>> connectionTable = new HashMap<Service, Set<ServiceDistance>>();
			
			// Obtain those remote services connected with the removed ones
			for (final Service service : removedServices) {
				final Set<ServiceDistance> remoteConnectedServices = sdg.getRemoteConnectedServices(service);
				connectionTable.put(service, remoteConnectedServices);
			}

			try {
				commit(addedServices, removedServices);
			} catch (NonLocalServiceException e) {
				
			}
	
			// Those services which after removal have some parameters still present
			// or subsumed in the parameter table are notified
			boolean notify = false;
			for (final Entry<Service, Set<ServiceDistance>> entry : connectionTable.entrySet()) {
				final Service localService = entry.getKey();
				for (final Parameter localParameter : pSearch.getDisseminationLayer().getLocalParameters())
					for (final Parameter serviceParameter : localService.getParameters())
						if (!pSearch.getDisseminationLayer().getTaxonomy().subsumes(localParameter.getID(), serviceParameter.getID())) {
							notify = true;
							break;
						}
	
				if (notify) {
					// Get the collision nodes which give access to the service
					// connected services
					for (final ServiceDistance remoteService : entry.getValue()) {
						final Set<PeerID> collisionNodes = sdg.getThroughCollisionNodes(remoteService.getService());
						for (final PeerID collisionNode : collisionNodes) {
							if (!notifications.containsKey(collisionNode))
								notifications.put(collisionNode, new HashSet<Service>());
	
							notifications.get(collisionNode).add(localService);
						}
					}
				}
			}
		}

		// Enqueue remove service messages
		for (final Entry<PeerID, Set<Service>> entry : notifications.entrySet()) {
			final PeerIDSet peers = new PeerIDSet();
			peers.addPeer(entry.getKey());
			sendRemoveServicesMessage(entry.getValue(), peers);
		}
	}
	
	private void commit(Set<Service> addedServices, Set<Service> removedServices) throws NonLocalServiceException{
		for (final Service addedService : addedServices) {
			// Increment parameter references
			for (final Parameter parameter : addedService.getParameters()) {
				incReference(parameter);

				// Is the first addition of the parameter
				if (refCount(parameter) == 1)
					pSearch.addLocalParameter(parameter);
			}

			sdg.addLocalService(addedService);
		}

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
		this.cManager = new ConnectionsManager(pSearch.getDisseminationLayer().getTaxonomy());
		this.sdg = new SDGTaxonomy(peer.getPeerID(), pSearch.getDisseminationLayer().getTaxonomy());
	}

	@Override
	public void messageReceived(final BroadcastMessage m, final long receptionTime) {
	}

	private void processConnectServicesMessage(final ConnectServicesMessage connectServicesMessage, final PeerID source) {
		logger.trace("Peer " + peer.getPeerID() + " connecting compatible services RS: " + connectServicesMessage.getRemoteSuccessors() + " RA: " + connectServicesMessage.getRemoteAncestors() + " thanks to collision detected in peer " + connectServicesMessage.getSource());

		final Map<Service, Set<ServiceDistance>> newSuccessors = new HashMap<Service, Set<ServiceDistance>>(connectServicesMessage.getRemoteSuccessors());
		final Map<Service, Set<ServiceDistance>> newAncestors = new HashMap<Service, Set<ServiceDistance>>(connectServicesMessage.getRemoteAncestors());
		
		traceSDG();

		synchronized (sdg) {
			for (final Iterator<Entry<Service, Set<ServiceDistance>>> it = newSuccessors.entrySet().iterator(); it.hasNext();) {
				final Entry<Service, Set<ServiceDistance>> entry = it.next();
				final Service service = entry.getKey();
				try {
					if (sdg.isLocal(service)) {
	
						final Set<ServiceDistance> alreadySuccessors = new HashSet<ServiceDistance>();
	
						// only maintain new successors
						for (final ServiceDistance sDistance : entry.getValue())
							if (sdg.getSuccessors(service).contains(sDistance))
								alreadySuccessors.add(sDistance);
	
						sdg.connectRemoteServices(service, entry.getValue(), new HashSet<ServiceDistance>(), source);
	
						entry.getValue().removeAll(alreadySuccessors);
	
						if (entry.getValue().isEmpty())
							it.remove();
					} else
						// remove non local services
						it.remove();
				} catch (final NonLocalServiceException nlse) {}
			}
	
			for (final Iterator<Entry<Service, Set<ServiceDistance>>> it = newAncestors.entrySet().iterator(); it.hasNext();) {
				final Entry<Service, Set<ServiceDistance>> entry = it.next();
				final Service service = entry.getKey();
				try {
					if (sdg.isLocal(service)) {
						final Set<ServiceDistance> alreadyAncestors = new HashSet<ServiceDistance>();
	
						// only maintain new ancestors
						for (final ServiceDistance sDistance : entry.getValue())
							if (sdg.getAncestors(service).contains(sDistance))
								alreadyAncestors.add(sDistance);
	
						sdg.connectRemoteServices(service, new HashSet<ServiceDistance>(), entry.getValue(), source);
	
						newAncestors.get(service).removeAll(alreadyAncestors);
	
						if (entry.getValue().isEmpty())
							it.remove();
					} else
						// remove non local services
						it.remove();
				} catch (final NonLocalServiceException nlse) {}
			}
		}

		if (!newSuccessors.isEmpty() || !newAncestors.isEmpty()) {
			traceSDG();

			if (!newSuccessors.isEmpty())
				graphCreationListener.newSuccessors(newSuccessors);

			if (!newAncestors.isEmpty())
				graphCreationListener.newAncestors(newAncestors);
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

		for (final Service remoteService : disconnectServicesMessage.getLostServices())
			// test that service really exists in graph before removal
			if (sdg.hasService(remoteService)) {

				// get the current ancestors and successors of the removed
				// service
				final Set<ServiceDistance> beforeAncestors = sdg.getLocalAncestors(remoteService, disconnectServicesMessage.getSource());
				final Set<ServiceDistance> beforeSuccessors = sdg.getLocalSuccessors(remoteService, disconnectServicesMessage.getSource());

				sdg.removeServiceConnectedBy(remoteService, disconnectServicesMessage.getSource());

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

				if (!lostAncestors.isEmpty())
					graphCreationListener.lostAncestors(lostAncestors);

				if (!lostSuccessors.isEmpty())
					graphCreationListener.lostSuccessors(lostSuccessors);
			}

		traceSDG();
	}

	private void processRemovedServicesMessage(final RemovedServicesMessage removedServicesMessage) {
		logger.trace("Peer " + peer.getPeerID() + " received a message from " + removedServicesMessage.getSource() + " to services " + removedServicesMessage.getLostServices());
 
		Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
		synchronized (cManager) {
			notifications.putAll(cManager.removeServices(removedServicesMessage.getLostServices(), removedServicesMessage.getSource()));
		}
		
		sendDisconnectNotifications(notifications);
	}

	private void processForwardMessage(final ForwardMessage forwardMessage) {
		logger.trace("Peer " + peer.getPeerID() + " accepted a forward message from " + forwardMessage.getSource() + " to " + forwardMessage.getDestinations());

		// get destinations from services
		pSearch.sendMulticastMessage(getDestinations(forwardMessage.getDestinations()), forwardMessage.getPayload());
	}

	@Override
	public void multicastMessageAccepted(final PeerID source, final PayloadMessage payload, final int distance) {
		if (payload instanceof ConnectServicesMessage)
			processConnectServicesMessage((ConnectServicesMessage) payload, source);
		else if (payload instanceof DisconnectServicesMessage)
			processDisconnectServicesMessage((DisconnectServicesMessage) payload);
		else if (payload instanceof RemovedServicesMessage)
			processRemovedServicesMessage((RemovedServicesMessage) payload);
		else if (payload instanceof ForwardMessage)
			processForwardMessage((ForwardMessage) payload);
		else {
			// the message must be processed by the upper layers
			mMessageListener.multicastMessageAccepted(source, payload, distance);
		}
	}

	private PeerIDSet getDestinations(final Set<Service> services) {
		final PeerIDSet destinations = new PeerIDSet();
		for (final Service service : services)
			destinations.addPeer(service.getPeerID());
		return destinations;
	}

	@Override
	public void parametersFound(final SearchResponseMessage message) {
		if (message.getPayload() instanceof CollisionResponseMessage) {
			logger.trace("Peer " + peer.getPeerID() + " accepted a collision response message " + message.getPayload() + " from " + message.getSource());

			final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) message.getPayload();

			collisionResponseMessage.addDistance(message.getDistance());

			// obtain the peers which must be notified with the found services
			// information
			final Map<Connection, PeerIDSet> updatedConnections = new HashMap<Connection, PeerIDSet>();
			synchronized (cManager) {
				updatedConnections.putAll(cManager.updateConnections(message));
			}

			final PeerIDSet notifiedPeers = new PeerIDSet();

			final Map<Service, Set<ServiceDistance>> successors = new HashMap<Service, Set<ServiceDistance>>();
			final Map<Service, Set<ServiceDistance>> ancestors = new HashMap<Service, Set<ServiceDistance>>();

			for (final Entry<Connection, PeerIDSet> e : updatedConnections.entrySet()) {
				final Connection connection = e.getKey();
				logger.trace("Peer " + peer.getPeerID() + " connection " + connection + " updated");

				final PeerIDSet partialPeers = e.getValue();
				final Map<ServiceDistance, Set<ServiceDistance>> currentSuccessors = getSuccessors(connection, collisionResponseMessage, partialPeers);
				final Map<ServiceDistance, Set<ServiceDistance>> currentAncestors = getAncestors(connection, collisionResponseMessage, partialPeers);

				final Map<ServiceDistance, Set<ServiceDistance>> inverseSuccessors = new HashMap<ServiceDistance, Set<ServiceDistance>>();
				final Map<ServiceDistance, Set<ServiceDistance>> inverseAncestors = new HashMap<ServiceDistance, Set<ServiceDistance>>();

				// Create inverse relations
				for (final Entry<ServiceDistance, Set<ServiceDistance>> entry : currentSuccessors.entrySet())
					for (final ServiceDistance successor : entry.getValue()) {
						if (!inverseAncestors.containsKey(successor))
							inverseAncestors.put(successor, new HashSet<ServiceDistance>());
						inverseAncestors.get(successor).add(entry.getKey());
					}

				for (final Entry<ServiceDistance, Set<ServiceDistance>> entry : currentAncestors.entrySet())
					for (final ServiceDistance ancestor : entry.getValue()) {
						if (!inverseSuccessors.containsKey(ancestor))
							inverseSuccessors.put(ancestor, new HashSet<ServiceDistance>());
						inverseSuccessors.get(ancestor).add(entry.getKey());
					}

				// Merge partial results
				notifiedPeers.addPeers(partialPeers);

				addServices(successors, inverseSuccessors);

				addServices(successors, currentSuccessors);

				addServices(ancestors, inverseAncestors);

				addServices(ancestors, currentAncestors);
			}

			graphCreationListener.filterConnections(successors, ancestors);

			if (!notifiedPeers.isEmpty())
				sendConnectServicesMessage(successors, ancestors, notifiedPeers);
		}
	}

	private void addServices(final Map<Service, Set<ServiceDistance>> successors, final Map<ServiceDistance, Set<ServiceDistance>> inverseSuccessors) {
		for (final Entry<ServiceDistance, Set<ServiceDistance>> entry : inverseSuccessors.entrySet()) {
			final ServiceDistance sDistance = entry.getKey();
			final Set<ServiceDistance> updatedDistances = updateDistances(entry.getValue(), sDistance.getDistance());
			if (!successors.containsKey(sDistance.getService()))
				successors.put(sDistance.getService(), updatedDistances);

			successors.get(sDistance.getService()).addAll(updatedDistances);
		}
	}

	private Set<ServiceDistance> updateDistances(final Set<ServiceDistance> serviceDistances, final Integer distance) {
		final Set<ServiceDistance> updatedDistances = new HashSet<ServiceDistance>();
		for (final ServiceDistance sDistance : serviceDistances) {
			final Integer newDistance = Integer.valueOf(sDistance.getDistance().intValue() + distance.intValue());
			updatedDistances.add(new ServiceDistance(sDistance.getService(), newDistance));
		}
		return updatedDistances;
	}

	// sends a message including the passed connections
	private void sendConnectServicesMessage(final Map<Service, Set<ServiceDistance>> remoteSuccessors, final Map<Service, Set<ServiceDistance>> remoteAncestors, final PeerIDSet notifiedPeers) {
		logger.trace("Peer " + peer.getPeerID() + " sending connect services message to " + notifiedPeers + " RS:" + remoteSuccessors + " RA:" + remoteAncestors);
		final ConnectServicesMessage messageForPeers = new ConnectServicesMessage(remoteSuccessors, remoteAncestors, peer.getPeerID());
		pSearch.sendMulticastMessage(notifiedPeers, messageForPeers);
	}

	private void sendDisconnectServicesMessage(final Set<Service> lostServices, final PeerIDSet notifiedPeers) {
		logger.trace("Peer " + peer.getPeerID() + " sending disconnect services message to " + notifiedPeers + " with lost services " + lostServices);
		final DisconnectServicesMessage messageForOutputPeers = new DisconnectServicesMessage(lostServices, peer.getPeerID());
		pSearch.sendMulticastMessage(notifiedPeers, messageForOutputPeers);
	}

	private void sendRemoveServicesMessage(final Set<Service> rServices, final PeerIDSet notifiedPeers) {
		logger.trace("Peer " + peer.getPeerID() + " sending removed services message to " + notifiedPeers + " with removed services " + rServices);
		final RemovedServicesMessage messageForOutputPeers = new RemovedServicesMessage(rServices, peer.getPeerID());
		pSearch.sendMulticastMessage(notifiedPeers, messageForOutputPeers);
	}

	private void sendCollisionSearchMessage(final Set<Parameter> parameters) {
		logger.trace("Peer " + peer.getPeerID() + " sending collision message while searching for parameters " + parameters);
		final CollisionMessage collisionMessage = new CollisionMessage(peer.getPeerID());
		pSearch.sendSearchMessage(parameters, collisionMessage, SearchType.Generic);
	}

	private Map<ServiceDistance, Set<ServiceDistance>> getAncestors(final Connection connection, final CollisionResponseMessage collisionResponseMessage, final PeerIDSet validPeers) {
		final Map<ServiceDistance, Set<ServiceDistance>> ancestors = new HashMap<ServiceDistance, Set<ServiceDistance>>();
		// Get those input services which are valid
		final Set<ServiceDistance> validOutputServices = getValidServices(connection.getOutputServicesTable(), validPeers);

		// Add services to a graph
		final ExtendedServiceGraph eServiceGraph = createServiceGraph(collisionResponseMessage.getServices(), validOutputServices);

		// Get successor relations between the new received services and the
		// input services
		for (final Service receivedService : collisionResponseMessage.getServices()) {
			final ServiceDistance receivedServiceDistance = new ServiceDistance(receivedService, collisionResponseMessage.getDistance(receivedService));
			for (final ServiceNode realAncestor : eServiceGraph.getAncestors(eServiceGraph.getServiceNode(receivedService), false))
				// Check if it is a valid successor
				if (validOutputServices.contains(new ServiceDistance(realAncestor.getService(), Integer.valueOf(0)))) {
					final ServiceDistance ancestorServiceDistance = findServiceDistance(validOutputServices, realAncestor.getService());
					if (!ancestors.containsKey(receivedServiceDistance))
						ancestors.put(receivedServiceDistance, new HashSet<ServiceDistance>());
					ancestors.get(receivedServiceDistance).add(ancestorServiceDistance);
				}
		}
		return ancestors;
	}

	private Set<ServiceDistance> getValidServices(final Set<ServiceDistance> serviceDistances, final PeerIDSet validPeers) {
		final Set<ServiceDistance> validServices = new HashSet<ServiceDistance>();
		for (final ServiceDistance sDistance : serviceDistances)
			if (validPeers.contains(sDistance.getService().getPeerID()))
				validServices.add(sDistance);
		return validServices;
	}

	private ExtendedServiceGraph createServiceGraph(final Set<Service> receivedServices, final Set<ServiceDistance> validServices) {
		final ExtendedServiceGraph eServiceGraph = new ExtendedServiceGraph(pSearch.getDisseminationLayer().getTaxonomy());

		for (final Service receivedService : receivedServices)
			eServiceGraph.merge(receivedService);

		for (final ServiceDistance validService : validServices)
			eServiceGraph.merge(validService.getService());

		return eServiceGraph;
	}

	private Map<ServiceDistance, Set<ServiceDistance>> getSuccessors(final Connection connection, final CollisionResponseMessage collisionResponseMessage, final PeerIDSet validPeers) {
		final Map<ServiceDistance, Set<ServiceDistance>> successors = new HashMap<ServiceDistance, Set<ServiceDistance>>();

		// Get those input services which are valid
		final Set<ServiceDistance> validInputServices = getValidServices(connection.getInputServicesTable(), validPeers);

		// Add services to a graph
		final ExtendedServiceGraph eServiceGraph = createServiceGraph(collisionResponseMessage.getServices(), validInputServices);

		// Get successor relations between the new received services and the
		// input services
		for (final Service receivedService : collisionResponseMessage.getServices()) {
			final ServiceDistance receivedServiceDistance = new ServiceDistance(receivedService, collisionResponseMessage.getDistance(receivedService));

			for (final ServiceNode realSuccessor : eServiceGraph.getSuccessors(eServiceGraph.getServiceNode(receivedService), false))
				// Check if it is a valid successor
				if (validInputServices.contains(new ServiceDistance(realSuccessor.getService(), Integer.valueOf(0)))) {
					final ServiceDistance realSuccessorDistance = findServiceDistance(validInputServices, realSuccessor.getService());
					if (!successors.containsKey(receivedServiceDistance))
						successors.put(receivedServiceDistance, new HashSet<ServiceDistance>());
					successors.get(receivedServiceDistance).add(realSuccessorDistance);
				}
		}
		return successors;
	}

	private ServiceDistance findServiceDistance(final Set<ServiceDistance> serviceDistances, final Service service) {
		for (final ServiceDistance sDistance : serviceDistances)
			if (sDistance.getService().equals(service))
				return sDistance;
		return null;
	}

	@Override
	public PayloadMessage parametersChanged(final PeerID sender, final Set<Parameter> addedParameters, final Set<Parameter> removedParameters, final Set<Parameter> removedLocalParameters, final Map<Parameter, DistanceChange> changedParameters, final PayloadMessage payload) {
		final Set<Collision> inhibitedCollisions = new HashSet<Collision>();
		final Set<Collision> collisions = new HashSet<Collision>();

		if (!removedParameters.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " parameters removed " + removedParameters + ", checking for affected collisions");

			// obtain which previously detected collisions are affected by
			// parameter removal
			final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
			synchronized (cManager) {
				final Set<Connection> removedConnections = cManager.checkCollisions(removedParameters);
	
				if (!removedConnections.isEmpty()) {
					// obtain those parameters whose search must be canceled
					final Set<Parameter> canceledParameters = new HashSet<Parameter>();
					for (final Connection connection : removedConnections) {
						final Collision collision = connection.getCollision();
						canceledParameters.add(collision.getInput());
						canceledParameters.add(collision.getOutput());
					}
	
					// send cancel search message
					pSearch.sendCancelSearchMessage(canceledParameters);
				}
	
				// send disconnect notifications
				 notifications.putAll((cManager.getNotifications(removedConnections)));
			}
			sendDisconnectNotifications(notifications);
		}

		if (!addedParameters.isEmpty()) {

			logger.trace("Peer " + peer.getPeerID() + " new parameters added " + addedParameters + ", checking for collisions");

			// obtain which collisions are caused by the new parameters addition
			final Set<Collision> detectedCollisions = checkNewParametersCollisions(addedParameters, sender);

			final Set<Collision> invalidCollisions = getInvalidCollisions(detectedCollisions, sender);

			// remove all invalid collisions
			detectedCollisions.removeAll(invalidCollisions);

			// remove invalid collisions
			detectedCollisions.removeAll(invalidCollisions);

			// add valid collisions
			collisions.addAll(detectedCollisions);

			// inhibe for neighbors valid & invalid detected collisions
			inhibitedCollisions.addAll(detectedCollisions);
			inhibitedCollisions.addAll(invalidCollisions);
		}

		if (!changedParameters.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " parameters estimated distance changed");

			// obtain those parameters whose estimated distance has been reduced
			final Set<Parameter> reducedParameters = new HashSet<Parameter>();
			for (final Entry<Parameter, DistanceChange> entry : changedParameters.entrySet()) {
				final DistanceChange dChange = entry.getValue();
				if (dChange.getNewValue() < dChange.getPreviousValue())
					reducedParameters.add(entry.getKey());
			}

			if (!reducedParameters.isEmpty()) {
				// obtain collisions with reduced parameters
				final Set<Collision> detectedCollisions = getReducedParametersCollisions(reducedParameters);

				// add detected collisions
				collisions.addAll(detectedCollisions);

				// add detected collisions to inhibited ones
				inhibitedCollisions.addAll(detectedCollisions);
			}
		}

		// include those inhibitions received with the message which added the
		// new parameters
		if (payload != null) {
			final InhibeCollisionsMessage inhibeCollisionsMessage = (InhibeCollisionsMessage) payload;

			logger.trace("Peer " + peer.getPeerID() + " received inhibition for collisions " + inhibeCollisionsMessage.getInhibedCollisions());
			inhibitedCollisions.addAll(inhibeCollisionsMessage.getInhibedCollisions());

			// remove all received inhibited collisions from detected ones
			collisions.removeAll(inhibeCollisionsMessage.getInhibedCollisions());
		}

		// if collisions were detected, notify them
		if (!collisions.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " detected collisions " + collisions);

			// Update the collisions detected by the current node
			final Set<Parameter> parameters = new HashSet<Parameter>();
			synchronized (cManager) {
				for (final Collision collision : collisions) {
					cManager.addConnection(collision);
	
					// Get the parameters which participate in each detected
					// collision
					parameters.add(collision.getInput());
					parameters.add(collision.getOutput());
				}
			}

			// send a message to search for the colliding parameters
			sendCollisionSearchMessage(parameters);
		}

		// if collisions were detected return them as payload for parameter
		// table update propagated message
		if (!inhibitedCollisions.isEmpty())
			return new InhibeCollisionsMessage(inhibitedCollisions, peer.getPeerID());

		return null;
	}

	private Set<Collision> getReducedParametersCollisions(final Set<Parameter> reducedParameters) {
		logger.trace("Peer " + peer.getPeerID() + " checking for collisions for reduced parameters: " + reducedParameters);
		final Set<Collision> collisions = CollisionDetector.getParametersColliding(new HashSet<Parameter>(), pSearch.getDisseminationLayer().getParameters(), false, pSearch.getDisseminationLayer().getTaxonomy());

		// all collisions not containing reducedParameters are removed
		for (final Iterator<Collision> it = collisions.iterator(); it.hasNext();) {
			final Collision collision = it.next();
			if (!reducedParameters.contains(collision.getInput()) && !reducedParameters.contains(collision.getOutput()))
				it.remove();
		}

		return collisions;
	}

	public Set<Collision> getInvalidCollisions(final Set<Collision> newParametersCollisions, final PeerID neighbor) {
		final Set<Collision> invalidCollisions = new HashSet<Collision>();
		for (final Collision collision : newParametersCollisions)
			if (!isCollisionValid(collision, neighbor))
				invalidCollisions.add(collision);
		return invalidCollisions;
	}

	// checks for new collisions taking into account the new added parameters.
	// Returns the list of detected collisions
	private Set<Collision> checkNewParametersCollisions(final Set<Parameter> addedParameters, final PeerID sender) {
		boolean checkNewParameters = false;
		if (sender.equals(peer.getPeerID()))
			checkNewParameters = true;

		return CollisionDetector.getParametersColliding(addedParameters, pSearch.getDisseminationLayer().getParameters(), checkNewParameters, pSearch.getDisseminationLayer().getTaxonomy());
	}

	// checks if the collision must be detected by the current node
	private boolean isCollisionValid(final Collision collision, final PeerID neighbor) {
		// all collisions produced by local parameters are valid
		if (neighbor.equals(peer.getPeerID()))
			return true;

		final ParameterDisseminator dissemination = pSearch.getDisseminationLayer();

		logger.trace("Peer " + peer.getPeerID() + " I:" + dissemination.getEstimatedDistance(collision.getInput()) + " O:" + dissemination.getEstimatedDistance(collision.getOutput()));
		final int localSum = dissemination.getEstimatedDistance(collision.getInput()) + dissemination.getEstimatedDistance(collision.getOutput());

		logger.trace("Peer " + peer.getPeerID() + " localSum: " + localSum);

		// obtain the estimated distance for the colliding parameters according
		// to the values in the local table
		final int neighborInput = (dissemination.getDistance(collision.getInput(), neighbor) == 0) ? dissemination.getEstimatedDistance(collision.getInput()) - 1 : dissemination.getDistance(collision.getInput(), neighbor) + 1;
		final int neighborOutput = (dissemination.getDistance(collision.getOutput(), neighbor) == 0) ? dissemination.getEstimatedDistance(collision.getOutput()) - 1 : dissemination.getDistance(collision.getOutput(), neighbor) + 1;

		final int neighborSum = neighborInput + neighborOutput;

		logger.trace("Peer " + peer.getPeerID() + " I:" + neighborInput + " O:" + neighborOutput);

		logger.trace("Peer " + peer.getPeerID() + " neighbor: " + neighbor + " sum: " + neighborSum);

		// collision is valid if the local sum for colliding parameters is
		// greater than the sum estimated for the neighbor
		if (localSum > neighborSum)
			return true;

		// if it is equal the collision is valid if the local identifier is
		// greater than the one which sent the update message
		if (localSum == neighborSum)
			return peer.getPeerID().compareTo(neighbor) > 0;

		return false;
	}

	@Override
	public PayloadMessage searchMessageReceived(final SearchMessage message) {
		// only messages containing a collision message as payload are valid
		if (message.getPayload() instanceof CollisionMessage) {
			logger.trace("Peer " + peer.getPeerID() + " received collision message " + message.getPayload());

			Set<Service> services;
			synchronized (sdg) {
				// obtain those services which provide the searched parameters
				services = sdg.findLocalCompatibleServices(message.getSearchedParameters());
			}

			// initial distance is zero because all services are local
			final Map<Service, Integer> serviceDistanceTable = new HashMap<Service, Integer>();
			for (final Service service : services)
				serviceDistanceTable.put(service, Integer.valueOf(0));

			final CollisionResponseMessage collisionResponseMessage = new CollisionResponseMessage(serviceDistanceTable, peer.getPeerID());

			logger.trace("Peer " + peer.getPeerID() + " sending collision response with services " + serviceDistanceTable + " to " + message.getSource());
			return collisionResponseMessage;
		}

		return null;
	}

	@Override
	public void changedParameterRoutes(final Map<MessageID, Set<Parameter>> lostParameters, final Set<MessageID> lostParameterRoutes, final Map<MessageID, MessageID> routeAssociations) {
		for (final Entry<MessageID, Set<Parameter>> entry : lostParameters.entrySet()) {
			final MessageID searchRouteID = entry.getKey();
			final MessageID parameterRouteID = routeAssociations.get(searchRouteID);
			// check if route was completely lost
			if (lostParameterRoutes.contains(parameterRouteID)) {

				logger.trace("Peer " + peer.getPeerID() + " checking for collisions containing responses from lost routes: " + lostParameterRoutes);
				final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
				synchronized (cManager) {
					notifications.putAll(cManager.removeResponses(lostParameterRoutes));
				}
				sendDisconnectNotifications(notifications);
			} else {
				final Set<Parameter> parameters = entry.getValue();

				logger.trace("Peer " + peer.getPeerID() + " checking for collisions containing lost parameters: " + lostParameters);
				final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
				synchronized (cManager) {
					notifications.putAll(cManager.removeParameters(parameters, searchRouteID.getPeer()));
				}
				sendDisconnectNotifications(notifications);
			}
		}
	}

	@Override
	public void changedSearchRoutes(final Map<MessageID, Set<Parameter>> changedSearchRoutes, final Set<MessageID> lostSearchRoutes) {
		if (!lostSearchRoutes.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " checking for services connecting through lost search routes: " + lostSearchRoutes);

			final Map<Service, Set<Service>> lostAncestors = new HashMap<Service, Set<Service>>();
			final Map<Service, Set<Service>> lostSuccessors = new HashMap<Service, Set<Service>>();

			synchronized (sdg) {
				for (final MessageID routeID : lostSearchRoutes) {
					// If there are no more routes to the peer remove all services
					// which where obtained through the disappeared peer
					if (!pSearch.knowsSearchRouteTo(routeID.getPeer())) {
	
						// get the successors / ancestors of the removed services
						final Set<ServiceDistance> lostServices = sdg.servicesConnectedThrough(routeID);
	
						// services could be connected through a collision detected
						// in another peer
						// only single connected services are notified as removed
						for (final Iterator<ServiceDistance> it = lostServices.iterator(); it.hasNext();) {
							final ServiceDistance lostService = it.next();
							final Set<PeerID> collisionPeers = sdg.getThroughCollisionNodes(lostService.getService());
							// those services which are accessed through another
							// collision peer not processed
							if (collisionPeers.size() > 1)
								it.remove();
						}
	
						for (final ServiceDistance remoteService : lostServices)
							for (final ServiceDistance successor : sdg.getLocalSuccessors(remoteService.getService())) {
								if (!lostAncestors.containsKey(successor.getService()))
									lostAncestors.put(successor.getService(), new HashSet<Service>());
								lostAncestors.get(successor.getService()).add(remoteService.getService());
							}
	
						for (final ServiceDistance remoteService : lostServices)
							for (final ServiceDistance ancestor : sdg.getLocalAncestors(remoteService.getService())) {
								if (!lostSuccessors.containsKey(ancestor.getService()))
									lostSuccessors.put(ancestor.getService(), new HashSet<Service>());
								lostSuccessors.get(ancestor.getService()).add(remoteService.getService());
							}
	
						logger.trace("Peer " + peer.getPeerID() + " removing services connected through route: " + routeID);
						sdg.removeServicesFromRoute(routeID);
	
						logger.trace("Peer " + peer.getPeerID() + " created new SDG" + sdg.toString());
					}
				}
			}

			if (!lostAncestors.isEmpty())
				graphCreationListener.lostAncestors(lostAncestors);

			if (!lostSuccessors.isEmpty())
				graphCreationListener.lostSuccessors(lostSuccessors);
		}
	}

	// sends the notifications for service disconnection
	private void sendDisconnectNotifications(final Map<PeerIDSet, Set<Service>> notifications) {
		for (final Entry<PeerIDSet, Set<Service>> entry : notifications.entrySet())
			sendDisconnectServicesMessage(entry.getValue(), entry.getKey());
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

	@Override
	public Set<InputParameter> getConnectedInputs(Service service, Service ancestor) {
		synchronized (sdg) {
			return sdg.getConnectedInputs(service, ancestor);
		}
	}
}
