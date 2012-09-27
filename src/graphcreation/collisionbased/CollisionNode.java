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

import java.util.Collections;
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
import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;
import taxonomy.parameterList.ParameterList;
import util.logger.Logger;
import dissemination.DistanceChange;

class CollisionNode {
	
	private final ReliableBroadcastPeer peer;
	
	private final GraphCreator gCreator;
	
	private final GraphCreationListener graphCreationListener;
	
	private final Map<PeerID, Set<Inhibition>> neighborInhibitions = new HashMap<PeerID, Set<Inhibition>>();
	
	private final Logger logger = Logger.getLogger(CollisionNode.class);
	
	// the connections manager
	private final ConnectionsManager cManager;
	
	public CollisionNode(final ReliableBroadcastPeer peer, final GraphCreator gCreator, final GraphCreationListener graphCreationListener, final GraphType graphType) {
		this.peer = peer;
		this.gCreator = gCreator;
		this.graphCreationListener = graphCreationListener;
		this.cManager = new ConnectionsManager(gCreator.getPSearch().getDisseminationLayer().getTaxonomy(), graphType);
	}
	
	private void updateNeighborInhibitions(final Inhibition inhibition) {
		if (peer.getDetector().getCurrentNeighbors().contains(inhibition.getDetectedBy())) {
			synchronized (neighborInhibitions) {
				if (!neighborInhibitions.containsKey(inhibition.getDetectedBy()))
					neighborInhibitions.put(inhibition.getDetectedBy(), new HashSet<Inhibition>());
				neighborInhibitions.get(inhibition.getDetectedBy()).add(inhibition);
			}
		}
	}
	
	private Set<Inhibition> createInhibitions(final Set<Collision> collisions) {
		final Set<Inhibition> inhibitions = new HashSet<Inhibition>();
		for (final Collision collision : collisions) 
			inhibitions.add(new Inhibition(collision, peer.getPeerID()));
		return inhibitions;
	}

	public BroadcastMessage parametersChanged(final Set<Parameter> removedParameters, final Map<Parameter, DistanceChange> changedParameters, 
											  final Set<Parameter> tableAdditions, final List<BroadcastMessage> payloadMessages) {
		logger.trace("Peer " + peer.getPeerID() + " parameters table changed");
		
		checkRemovedParameters(removedParameters);
		
		final Set<Collision> collisions = getChangedParameterCollisions(changedParameters, tableAdditions);
		final Set<Inhibition> inhibitions = createInhibitions(collisions);
		removeAlreadyDetectedCollisions(collisions);
		processReceivedInhibitions(payloadMessages, collisions, inhibitions);
		processDetectedCollisions(collisions);

		if (!inhibitions.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " sending inhibitions " + inhibitions + " as parameter table payload");
			return new InhibeCollisionsMessage(peer.getPeerID(), inhibitions);
		}

		return null;
	}

	private void processDetectedCollisions(final Set<Collision> collisions) {
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
	}

	private void processReceivedInhibitions(final List<BroadcastMessage> payloadMessages, final Set<Collision> collisions, final Set<Inhibition> inhibitions) {
		for (final BroadcastMessage payload : payloadMessages) {
			final InhibeCollisionsMessage inhibeCollisionsMessage = (InhibeCollisionsMessage) payload;
			final Set<Inhibition> receivedInhibitions = inhibeCollisionsMessage.getInhibedCollisions();
			if (!receivedInhibitions.isEmpty()) {
				logger.trace("Peer " + peer.getPeerID() + " received inhibitions for collisions " + receivedInhibitions);
				for (Inhibition inhibition : receivedInhibitions) {
					collisions.remove(inhibition.getCollision());
					updateNeighborInhibitions(inhibition);
				}
				inhibitions.addAll(receivedInhibitions);
				
				logger.trace("Peer " + peer.getPeerID() + " provisional collisions after inhibition " + collisions);			
			}
		}
	}

	private Set<Collision> getChangedParameterCollisions(final Map<Parameter, DistanceChange> changedParameters, final Set<Parameter> tableAdditions) {
		final Set<Collision> collisions = new HashSet<Collision>();
		final Set<Parameter> validParameters = getValidParameters(tableAdditions, changedParameters);
		if (!validParameters.isEmpty()) {
			logger.debug("Peer " + peer.getPeerID() + " checking collisions for " + validParameters);
			collisions.addAll(checkParametersCollisions(validParameters));
		}
		return collisions;
	}

	private void checkRemovedParameters(final Set<Parameter> removedParameters) {
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
	
	private Set<Inhibition> getInhibitions(final PeerID neighbor) {
		synchronized (neighborInhibitions) {
			if (neighborInhibitions.containsKey(neighbor)) {
				final Set<Inhibition> inhibitions = neighborInhibitions.get(neighbor);
				neighborInhibitions.remove(neighbor);
				return inhibitions;
			}
			
			return Collections.emptySet();
		}
	}
	
	public void neighborsChanged(Set<PeerID> lostNeighbors) {
		final Set<Inhibition> affectedInhibitions = new HashSet<Inhibition>();
		for (final PeerID lostNeighbor : lostNeighbors)
			affectedInhibitions.addAll(getInhibitions(lostNeighbor));
		
		final Set<Collision> collisions = new HashSet<Collision>();
		for (final Inhibition inhibition : affectedInhibitions) {
			final Set<Parameter> knownParameters = gCreator.getPSearch().getDisseminationLayer().getParameters();
			final InputParameter input = inhibition.getCollision().getInput();
			final OutputParameter output = inhibition.getCollision().getOutput();
			if (knownParameters.contains(input) && knownParameters.contains(output))
				collisions.add(new Collision(input, output));
		}
		
		removeAlreadyDetectedCollisions(collisions);
		if (!collisions.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " detected collisions due to neighbor removal");
			processDetectedCollisions(collisions);
		}
	}
}
