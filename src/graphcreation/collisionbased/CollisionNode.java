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
import graphcreation.collisionbased.message.InhibeCollisionsMessage;
import graphcreation.collisionbased.message.Inhibition;
import graphcreation.collisionbased.message.RemovedServicesMessage;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;
import peer.Peer;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.parameter.Parameter;
import util.logger.Logger;
import dissemination.DistanceChange;
import dissemination.ParameterDisseminator;

class CollisionNode {
	
	private final Peer peer;
	
	private final GraphCreator gCreator;
	
	private final GraphCreationListener graphCreationListener;
	
	private final Logger logger = Logger.getLogger(CollisionNode.class);
	
	// the connections manager
	private final ConnectionsManager cManager;
	
	public CollisionNode(final Peer peer, final GraphCreator gCreator, final GraphCreationListener graphCreationListener) {
		this.peer = peer;
		this.gCreator = gCreator;
		this.graphCreationListener = graphCreationListener;
		this.cManager = new ConnectionsManager(gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
	}

	public PayloadMessage parametersChanged(final PeerID sender, final Set<Parameter> addedParameters, final Set<Parameter> removedParameters, final Map<Parameter, DistanceChange> changedParameters, final PayloadMessage payload) {
		logger.trace("Peer " + peer.getPeerID() + " parameters table changed");
		final Set<Inhibition> inhibitions = new HashSet<Inhibition>();
		final Set<Collision> collisions = new HashSet<Collision>();
		
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
		
		if (!addedParameters.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " new parameters added " + addedParameters + ", checking for collisions");

			// obtain which collisions are caused by the new parameters addition
			collisions.addAll(checkParametersCollisions(addedParameters, sender));
			
			processCollisions(sender, inhibitions, collisions);
		}

		if (!changedParameters.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " parameters estimated distance changed");
			
			Set<Collision> detectedCollisions = checkParametersCollisions(changedParameters.keySet(), sender);
			
			//remove already detected collisions
			//TODO improve with taxonomy checking
			synchronized (cManager) {
				for (final Iterator<Collision> it = detectedCollisions.iterator(); it.hasNext(); ) {
					Collision detectedCollision = it.next();
					if (cManager.contains(detectedCollision))
						it.remove();
				}
			}
			
			processCollisions(sender, inhibitions, collisions);
		}

		// include those inhibitions received with the message which added the
		// new parameters
		if (payload != null) {
			final InhibeCollisionsMessage inhibeCollisionsMessage = (InhibeCollisionsMessage) payload;
			logger.trace("Peer " + peer.getPeerID() + " received inhibitions for collisions " + inhibeCollisionsMessage.getInhibedCollisions());

			// remove collisions using received inhibitions (checking if collision is applied)
			for (Inhibition inhibedCollision : inhibeCollisionsMessage.getInhibedCollisions()) {
				if (!inhibedCollision.getNotAppliedTo().equals(peer.getPeerID()))
					collisions.remove(inhibedCollision.getCollision());
			}
			
			logger.trace("Peer " + peer.getPeerID() + " provisional collisions after inhibition " + collisions);
				
			inhibitions.addAll(inhibeCollisionsMessage.getInhibedCollisions());
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
		if (!inhibitions.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " sending inhibitions " + inhibitions + " as parameter table payload");
			return new InhibeCollisionsMessage(peer.getPeerID(), inhibitions);
		}

		return null;
	}

	private void sendCollisionSearchMessage(final Set<Parameter> parameters) {		
		logger.debug("Peer " + peer.getPeerID() + " starting collision message while searching for parameters " + parameters);
		final CollisionMessage collisionMessage = new CollisionMessage(peer.getPeerID());
		gCreator.getPSearch().sendSearchMessage(parameters, collisionMessage, SearchType.Generic);
	}	

	private void processCollisions(final PeerID sender, final Set<Inhibition> inhibitions, final Set<Collision> collisions) {
		logger.trace("Peer " + peer.getPeerID() + " provisional collisions " + collisions);

		Set<Collision> invalidCollisions = getInvalidCollisions(collisions, sender);
		logger.trace("Peer " + peer.getPeerID() + " invalid collisions " + invalidCollisions);
					
		for (Collision invalidCollision : invalidCollisions) {
			collisions.remove(invalidCollision);
			inhibitions.add(new Inhibition(invalidCollision, sender));
		}
		
		logger.trace("Peer " + peer.getPeerID() + " collisions after removing invalid " + collisions);
			
		for (Collision validCollision : collisions)
			inhibitions.add(new Inhibition(validCollision, PeerID.VOID_PEERID));
	}

	private Set<Collision> getInvalidCollisions(final Set<Collision> newParametersCollisions, final PeerID neighbor) {
		final Set<Collision> invalidCollisions = new HashSet<Collision>();
		for (final Collision collision : newParametersCollisions) {
			if (!isCollisionValid(collision, neighbor))
				invalidCollisions.add(collision);
		}
		return invalidCollisions;
	}

	// checks for new collisions taking into account the new added parameters.
	// Returns the list of detected collisions
	private Set<Collision> checkParametersCollisions(final Set<Parameter> addedParameters, final PeerID sender) {
		boolean checkNewParameters = false;
		if (sender.equals(peer.getPeerID()))
			checkNewParameters = true;

		return CollisionDetector.getParametersColliding(addedParameters, gCreator.getPSearch().getDisseminationLayer().getParameters(), checkNewParameters, gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
	}
	
	// checks if the collision must be detected by the current node
	private boolean isCollisionValid(final Collision collision, final PeerID neighbor) {
		// all collisions produced by local parameters are valid
		if (neighbor.equals(peer.getPeerID()))
			return true;

		final ParameterDisseminator dissemination = gCreator.getPSearch().getDisseminationLayer();

		logger.trace("Peer " + peer.getPeerID() + " I:" + dissemination.getEstimatedDistance(collision.getInput()) + " O:" + dissemination.getEstimatedDistance(collision.getOutput()));
		final int localSum = dissemination.getEstimatedDistance(collision.getInput()) + dissemination.getEstimatedDistance(collision.getOutput());

		logger.trace("Peer " + peer.getPeerID() + " localSum: " + localSum);

		// obtain the estimated distance for the colliding parameters according
		// to the values in the local table
		final int neighborInput = (dissemination.getDistance(collision.getInput(), neighbor) == 0) ? dissemination.getEstimatedDistance(collision.getInput()) - 1 : dissemination.getDistance(collision.getInput(), neighbor) + 1;
		final int neighborOutput = (dissemination.getDistance(collision.getOutput(), neighbor) == 0) ? dissemination.getEstimatedDistance(collision.getOutput()) - 1 : dissemination.getDistance(collision.getOutput(), neighbor) + 1;
		if (neighborInput == 0 || neighborOutput == 0)
			return true;

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
	
	public void processRemovedServicesMessage(final RemovedServicesMessage removedServicesMessage) {
		logger.trace("Peer " + peer.getPeerID() + " received a message from " + removedServicesMessage.getSource() + " to services " + removedServicesMessage.getLostServices());
 
		Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
		synchronized (cManager) {
			notifications.putAll(cManager.removeServices(removedServicesMessage.getLostServices(), removedServicesMessage.getSource()));
		}
		
		sendDisconnectNotifications(notifications, true);
	}
	
	private void sendDisconnectServicesMessage(final Set<Service> lostServices, final PeerIDSet notifiedPeers, boolean servicesWereRemoved) {
		logger.trace("Peer " + peer.getPeerID() + " sending disconnect services message to " + notifiedPeers + " with lost services " + lostServices);
		final DisconnectServicesMessage messageForOutputPeers = new DisconnectServicesMessage(peer.getPeerID(), lostServices, servicesWereRemoved);
		gCreator.getPSearch().sendMulticastMessage(notifiedPeers, messageForOutputPeers);
	}
	
	// sends the notifications for service disconnection
	private void sendDisconnectNotifications(final Map<PeerIDSet, Set<Service>> notifications, boolean servicesWereRemoved) {			
		for (final Entry<PeerIDSet, Set<Service>> entry : notifications.entrySet())
			sendDisconnectServicesMessage(entry.getValue(), entry.getKey(), servicesWereRemoved);
	}

	public void processCollisionResponse(SearchResponseMessage searchResponseMessage) {
		final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) searchResponseMessage.getPayload();

		collisionResponseMessage.addDistance(searchResponseMessage.getDistance());

		// obtain the peers which must be notified with the found services
		// information
		final Map<Connection, PeerIDSet> updatedConnections = new HashMap<Connection, PeerIDSet>();
		synchronized (cManager) {
			updatedConnections.putAll(cManager.updateConnections(searchResponseMessage));
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
	private void sendConnectServicesMessage(final Map<Service, Set<ServiceDistance>> remoteSuccessors, final Map<Service, Set<ServiceDistance>> remoteAncestors, final PeerIDSet notifiedPeers) {
		logger.trace("Peer " + peer.getPeerID() + " sending connect services message to " + notifiedPeers + " RS:" + remoteSuccessors + " RA:" + remoteAncestors);
		final ConnectServicesMessage messageForPeers = new ConnectServicesMessage(peer.getPeerID(), remoteSuccessors, remoteAncestors);
		gCreator.getPSearch().sendMulticastMessage(notifiedPeers, messageForPeers);
	}
	
	private Set<ServiceDistance> updateDistances(final Set<ServiceDistance> serviceDistances, final Integer distance) {
		final Set<ServiceDistance> updatedDistances = new HashSet<ServiceDistance>();
		for (final ServiceDistance sDistance : serviceDistances) {
			final Integer newDistance = Integer.valueOf(sDistance.getDistance().intValue() + distance.intValue());
			updatedDistances.add(new ServiceDistance(sDistance.getService(), newDistance));
		}
		return updatedDistances;
	}
	
	private Set<ServiceDistance> getValidServices(final Set<ServiceDistance> serviceDistances, final PeerIDSet validPeers) {
		final Set<ServiceDistance> validServices = new HashSet<ServiceDistance>();
		for (final ServiceDistance sDistance : serviceDistances)
			if (validPeers.contains(sDistance.getService().getPeerID()))
				validServices.add(sDistance);
		return validServices;
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
