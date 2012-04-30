package graphcreation.collisionbased;

import graphcreation.GraphCreationListener;
import graphcreation.GraphCreator;
import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.collisionbased.collisiondetector.CollisionDetector;
import graphcreation.collisionbased.connectionManager.Connection;
import graphcreation.collisionbased.connectionManager.ConnectionsManager;
import graphcreation.collisionbased.message.CollisionResponseMessage;
import graphcreation.collisionbased.message.ConnectServicesMessage;
import graphcreation.collisionbased.message.DisconnectServicesMessage;
import graphcreation.collisionbased.message.InhibeCollisionsMessage;
import graphcreation.collisionbased.message.Inhibition;
import graphcreation.collisionbased.message.RemovedServicesMessage;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.SearchedParameter;
import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;
import peer.Peer;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.parameter.Parameter;
import taxonomy.parameterList.ParameterList;
import util.logger.Logger;
import dissemination.DistanceChange;

class CollisionNode {
	
	private final Peer peer;
	
	private final GraphCreator gCreator;
	
	private final GraphCreationListener graphCreationListener;
	
	private final Logger logger = Logger.getLogger(CollisionNode.class);
	
	// the connections manager
	private final ConnectionsManager cManager;
	
	public CollisionNode(final Peer peer, final GraphCreator gCreator, final GraphCreationListener graphCreationListener, final GraphType graphType) {
		this.peer = peer;
		this.gCreator = gCreator;
		this.graphCreationListener = graphCreationListener;
		this.cManager = new ConnectionsManager(gCreator.getPSearch().getDisseminationLayer().getTaxonomy(), graphType);
	}

	public BroadcastMessage parametersChanged(final Set<Parameter> removedParameters, final Map<Parameter, DistanceChange> changedParameters, 
			final Set<Parameter> tableAdditions, 
			final List<BroadcastMessage> payloadMessages) {
		logger.trace("Peer " + peer.getPeerID() + " parameters table changed");
		
		if (!removedParameters.isEmpty()) {	
			logger.trace("Peer " + peer.getPeerID() + " parameters removed " + removedParameters + ", checking for affected collisions");

			// obtain which previously detected collisions are affected by
			// parameter removal
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
				gCreator.getPSearch().sendCancelSearchMessage(canceledParameters);
			}	
		}
		
		final Set<Parameter> validParameters = getValidParameters(tableAdditions, changedParameters);
		
		final Set<Collision> collisions = new HashSet<Collision>();
		if (!validParameters.isEmpty()) {
			logger.debug("Peer " + peer.getPeerID() + " checking collisions for " + validParameters);
			collisions.addAll(checkParametersCollisions(validParameters));
		}
		
		final Set<Inhibition> inhibitions = new HashSet<Inhibition>();
		for (final Collision collision : collisions) 
			inhibitions.add(new Inhibition(collision));
		
		removeAlreadyDetectedCollisions(collisions);

		// include those inhibitions received with the message which added the
		// new parameters
		for (final BroadcastMessage payload : payloadMessages) {
			final InhibeCollisionsMessage inhibeCollisionsMessage = (InhibeCollisionsMessage) payload;
			final Set<Inhibition> receivedInhibitions = inhibeCollisionsMessage.getInhibedCollisions();
			if (!receivedInhibitions.isEmpty()) {
				logger.trace("Peer " + peer.getPeerID() + " received inhibitions for collisions " + receivedInhibitions);
	
				// remove collisions using received inhibitions (checking if collision is applied)
				for (Inhibition inhibedCollision : receivedInhibitions)
					collisions.remove(inhibedCollision.getCollision());
				
				logger.trace("Peer " + peer.getPeerID() + " provisional collisions after inhibition " + collisions);
					
				inhibitions.addAll(receivedInhibitions);
			}
		}

		// if collisions were detected, notify them
		if (!collisions.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " detected collisions " + collisions);

			// Update the collisions detected by the current node
			synchronized (cManager) {
				for (final Collision collision : collisions)
					cManager.addCollision(collision);
			}

			// send a message to search for the colliding parameters
			sendCollisionSearchMessage(collisions);
		}

		// if collisions were detected return them as payload for parameter
		// table update propagated message
		if (!inhibitions.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " sending inhibitions " + inhibitions + " as parameter table payload");
			return new InhibeCollisionsMessage(peer.getPeerID(), inhibitions);
		}

		return null;
	}

	private Set<Parameter> getValidParameters(final Set<Parameter> tableAdditions, final Map<Parameter, DistanceChange> changedParameters) {
		final Set<Parameter> validParameters = new HashSet<Parameter>();
		for (final Parameter p : tableAdditions) {
			if (changedParameters.containsKey(p)) {
				final DistanceChange dChange = changedParameters.get(p);
				if (dChange.getNewValue() > dChange.getPreviousValue())
					validParameters.add(p);
			}
		}
		return validParameters;
	}

	private void removeAlreadyDetectedCollisions(final Set<Collision> collisions) {
		//TODO improve with taxonomy checking
		synchronized (cManager) {
			for (final Iterator<Collision> it = collisions.iterator(); it.hasNext(); ) {
				Collision detectedCollision = it.next();
				if (cManager.contains(detectedCollision))
					it.remove();
			}
		}
	}

	private void sendCollisionSearchMessage(final Set<Collision> collisions) {
		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		for (final Collision collision : collisions) {
			searchedParameters.add(new SearchedParameter(collision.getInput(), gCreator.getPSearch().getDisseminationLayer().getMaxDistance()));
			searchedParameters.add(new SearchedParameter(collision.getOutput(), gCreator.getPSearch().getDisseminationLayer().getMaxDistance()));
		}
		
		final Set<Parameter> parameters = new HashSet<Parameter>();
		for (final SearchedParameter searchedParameter : searchedParameters)
			parameters.add(searchedParameter.getParameter());
		
		logger.debug("Peer " + peer.getPeerID() + " starting collision message while searching for parameters " + (new ParameterList(parameters)).pretty(gCreator.getTaxonomy()));
		gCreator.getPSearch().sendSearchMessage(searchedParameters, null, SearchType.Generic);
	}	

	// checks for new collisions taking into account the new added parameters.
	// Returns the list of detected collisions
	private Set<Collision> checkParametersCollisions(final Set<Parameter> addedParameters) {
		return CollisionDetector.getParametersColliding(addedParameters, gCreator.getPSearch().getDisseminationLayer().getParameters(), false, gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
	}
	
	public void processRemovedServicesMessage(final RemovedServicesMessage removedServicesMessage) {
		logger.trace("Peer " + peer.getPeerID() + " received a message from " + removedServicesMessage.getSource() + " to services " + removedServicesMessage.getLostServices());
 
		Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
		synchronized (cManager) {
			notifications.putAll(cManager.removeServices(removedServicesMessage.getLostServices(), removedServicesMessage.getSource()));
		}
		
		sendDisconnectNotifications(notifications, true);
	}
	
	private void sendDisconnectServicesMessage(final Set<Service> lostServices, final Set<PeerID> notifiedPeers, boolean servicesWereRemoved) {
		logger.trace("Peer " + peer.getPeerID() + " sending disconnect services message to " + notifiedPeers + " with lost services " + lostServices);
		final DisconnectServicesMessage messageForOutputPeers = new DisconnectServicesMessage(peer.getPeerID(), lostServices, servicesWereRemoved);
		gCreator.getPSearch().sendMulticastMessage(notifiedPeers, messageForOutputPeers, false);
	}
	
	// sends the notifications for service disconnection
	private void sendDisconnectNotifications(final Map<PeerIDSet, Set<Service>> notifications, boolean servicesWereRemoved) {			
		for (final Entry<PeerIDSet, Set<Service>> entry : notifications.entrySet())
			sendDisconnectServicesMessage(entry.getValue(), entry.getKey().getPeerSet(), servicesWereRemoved);
	}

	public void processCollisionResponse(SearchResponseMessage searchResponseMessage) {
		final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) searchResponseMessage.getPayload();

		collisionResponseMessage.addDistance(searchResponseMessage.getDistance());

		// obtain the peers which must be notified with the found services
		// information
		final Map<Connection, Set<PeerID>> updatedConnections = new HashMap<Connection, Set<PeerID>>();
		synchronized (cManager) {
			updatedConnections.putAll(cManager.updateConnections(searchResponseMessage));
		}
		
		logger.trace("Peer " + peer.getPeerID() + " updated connections: " + updatedConnections);

		final Set<PeerID> notifiedPeers = new HashSet<PeerID>();

		final Map<Service, Set<ServiceDistance>> successors = new HashMap<Service, Set<ServiceDistance>>();
		final Map<Service, Set<ServiceDistance>> ancestors = new HashMap<Service, Set<ServiceDistance>>();

		for (final Entry<Connection, Set<PeerID>> e : updatedConnections.entrySet()) {
			final Connection connection = e.getKey();
			logger.trace("Peer " + peer.getPeerID() + " connection " + connection + " updated");

			final Set<PeerID> partialPeers = e.getValue();
			final Map<ServiceDistance, Set<ServiceDistance>> currentSuccessors = getSuccessors(connection, collisionResponseMessage);
			final Map<ServiceDistance, Set<ServiceDistance>> currentAncestors = getAncestors(connection, collisionResponseMessage);

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
			notifiedPeers.addAll(partialPeers);

			addServices(successors, inverseSuccessors);

			addServices(successors, currentSuccessors);

			addServices(ancestors, inverseAncestors);

			addServices(ancestors, currentAncestors);
		}

		graphCreationListener.filterConnections(successors, ancestors);

		if (!notifiedPeers.isEmpty())
			sendConnectServicesMessage(successors, ancestors, notifiedPeers);

	}
	
	private Map<ServiceDistance, Set<ServiceDistance>> getSuccessors(final Connection connection, final CollisionResponseMessage collisionResponseMessage) {
		final Map<ServiceDistance, Set<ServiceDistance>> successors = new HashMap<ServiceDistance, Set<ServiceDistance>>();

		// Get those input services which are valid
		final Set<ServiceDistance> inputServices = connection.getInputServicesTable();

		// Add services to a graph
		final ExtendedServiceGraph eServiceGraph = createServiceGraph(collisionResponseMessage.getServices(), inputServices);

		// Get successor relations between the new received services and the
		// input services
		for (final Service receivedService : collisionResponseMessage.getServices()) {
			final ServiceDistance receivedServiceDistance = new ServiceDistance(receivedService, collisionResponseMessage.getDistance(receivedService));

			for (final ServiceNode realSuccessor : eServiceGraph.getSuccessors(eServiceGraph.getServiceNode(receivedService), false))
				// Check if it is a valid successor
				if (inputServices.contains(new ServiceDistance(realSuccessor.getService(), Integer.valueOf(0)))) {
					final ServiceDistance realSuccessorDistance = findServiceDistance(inputServices, realSuccessor.getService());
					if (!successors.containsKey(receivedServiceDistance))
						successors.put(receivedServiceDistance, new HashSet<ServiceDistance>());
					successors.get(receivedServiceDistance).add(realSuccessorDistance);
				}
		}
		return successors;
	}
	
	private Map<ServiceDistance, Set<ServiceDistance>> getAncestors(final Connection connection, final CollisionResponseMessage collisionResponseMessage) {
		final Map<ServiceDistance, Set<ServiceDistance>> ancestors = new HashMap<ServiceDistance, Set<ServiceDistance>>();
		// Get those input services which are valid
		final Set<ServiceDistance> outputServices = connection.getOutputServicesTable();

		// Add services to a graph
		final ExtendedServiceGraph eServiceGraph = createServiceGraph(collisionResponseMessage.getServices(), outputServices);

		// Get successor relations between the new received services and the
		// input services
		for (final Service receivedService : collisionResponseMessage.getServices()) {
			final ServiceDistance receivedServiceDistance = new ServiceDistance(receivedService, collisionResponseMessage.getDistance(receivedService));
			for (final ServiceNode realAncestor : eServiceGraph.getAncestors(eServiceGraph.getServiceNode(receivedService), false))
				// Check if it is a valid successor
				if (outputServices.contains(new ServiceDistance(realAncestor.getService(), Integer.valueOf(0)))) {
					final ServiceDistance ancestorServiceDistance = findServiceDistance(outputServices, realAncestor.getService());
					if (!ancestors.containsKey(receivedServiceDistance))
						ancestors.put(receivedServiceDistance, new HashSet<ServiceDistance>());
					ancestors.get(receivedServiceDistance).add(ancestorServiceDistance);
				}
		}
		return ancestors;
	}
	
	private ServiceDistance findServiceDistance(final Set<ServiceDistance> serviceDistances, final Service service) {
		for (final ServiceDistance sDistance : serviceDistances)
			if (sDistance.getService().equals(service))
				return sDistance;
		return null;
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
	
	// sends a message including the passed connections
	private void sendConnectServicesMessage(final Map<Service, Set<ServiceDistance>> remoteSuccessors, final Map<Service, Set<ServiceDistance>> remoteAncestors, final Set<PeerID> notifiedPeers) {
		logger.trace("Peer " + peer.getPeerID() + " sending connect services message to " + notifiedPeers + " RS:" + remoteSuccessors + " RA:" + remoteAncestors);
		final ConnectServicesMessage messageForPeers = new ConnectServicesMessage(peer.getPeerID(), remoteSuccessors, remoteAncestors);
		gCreator.getPSearch().sendMulticastMessage(notifiedPeers, messageForPeers, false);
	}
	
	private Set<ServiceDistance> updateDistances(final Set<ServiceDistance> serviceDistances, final Integer distance) {
		final Set<ServiceDistance> updatedDistances = new HashSet<ServiceDistance>();
		for (final ServiceDistance sDistance : serviceDistances) {
			final Integer newDistance = Integer.valueOf(sDistance.getDistance().intValue() + distance.intValue());
			updatedDistances.add(new ServiceDistance(sDistance.getService(), newDistance));
		}
		return updatedDistances;
	}

	private ExtendedServiceGraph createServiceGraph(final Set<Service> receivedServices, final Set<ServiceDistance> validServices) {
		final ExtendedServiceGraph eServiceGraph = new ExtendedServiceGraph(gCreator.getPSearch().getDisseminationLayer().getTaxonomy());

		for (final Service receivedService : receivedServices)
			eServiceGraph.merge(receivedService);

		for (final ServiceDistance validService : validServices)
			eServiceGraph.merge(validService.getService());

		return eServiceGraph;
	}
	
	public void checkCollisions(Set<PeerID> lostDestinations) {
		logger.trace("Peer " + peer.getPeerID() + " checking for collisions containing responses from lost destinations: " + lostDestinations);
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
		synchronized (cManager) {
			notifications.putAll(cManager.removeResponses(lostDestinations));
		}
		
		sendDisconnectNotifications(notifications, false);
	}
}
