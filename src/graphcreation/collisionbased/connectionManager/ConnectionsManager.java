package graphcreation.collisionbased.connectionManager;

import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.services.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.SearchResponseMessage;
import peer.conditionregister.ConditionRegister;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.Taxonomy;
import taxonomy.parameter.Parameter;

public class ConnectionsManager {

	// collisions detected by the current node
	private final Set<Connection> detectedConnections = new HashSet<Connection>();
	
	private static final long CLEAN_INVALID_TIME = 60000;
	
	private final ConditionRegister<Connection> invalidConnections = new ConditionRegister<Connection>(CLEAN_INVALID_TIME);
	
	private final GraphType graphType;

	// the taxonomy used during management
	private final Taxonomy taxonomy;

	/**
	 * Constructor of the connections manager
	 */
	public ConnectionsManager(final Taxonomy taxonomy, final GraphType graphType) {
		this.taxonomy = taxonomy;
		this.graphType = graphType;
	}

	/**
	 * Adds a new connection, if does not previously exist, using the passed
	 * collision
	 * 
	 * @param collision
	 *            the collision used to generate the connection
	 * @param
	 */
	public void addCollision(final Collision collision) {
		detectedConnections.add(new Connection(collision, taxonomy, graphType));
	}

	/**
	 * Updates the connections using the received search response message.
	 * 
	 * @return a map which contains the updated connections with those peers
	 *         which must be notified for each connection
	 */
	public Map<Connection, Set<PeerID>> updateConnections(final SearchResponseMessage searchResponseMessage) {
		final Map<Connection, Set<PeerID>> updatedConnections = new HashMap<Connection, Set<PeerID>>();
		for (final Connection connection : detectedConnections) {
			final Set<PeerID> notifiedPeers = connection.addSearchResponse(searchResponseMessage);
			if (!notifiedPeers.isEmpty())
				updatedConnections.put(connection, notifiedPeers);
		}
		return updatedConnections;
	}

	/**
	 * Removes those responses identified by the passed route identifiers
	 * 
	 * @param lostRoutes
	 *            the routes which have been lost
	 * @return the notifications for colliding peers
	 */
	public Map<PeerIDSet, Set<Service>> removeResponses(final Set<PeerID> lostDestinations) {
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
		final Set<Connection> removedConnections = new HashSet<Connection>(); 
				
		for (Iterator<Connection> it = getAllConnections().iterator(); it.hasNext(); ) {
			Connection connection = it.next();
			final Map<PeerIDSet, Set<Service>> partialNotifications = connection.removeResponses(lostDestinations);
			for (final Entry<PeerIDSet, Set<Service>> entry : partialNotifications.entrySet()) {
				final PeerIDSet peers = entry.getKey();
				if (!notifications.containsKey(peers))
					notifications.put(peers, new HashSet<Service>());
				notifications.get(peers).addAll(entry.getValue());
			}
			
			if (!connection.isConnected())
				removedConnections.add(connection);
		}
		
		removeConnections(removedConnections);
		
		return notifications;
	}
	
	private void removeConnections(final Set<Connection> removedConnections) {
		for (final Connection removedConnection : removedConnections) {
			invalidConnections.remove(removedConnection);
			detectedConnections.remove(removedConnection);
		}
	}

	/**
	 * Removes specified services from received responses
	 * 
	 * @param removedServices
	 *            the removed services
	 * @param source
	 *            the source of the services
	 * @return the notifications for colliding peers
	 */
	public Map<PeerIDSet, Set<Service>> removeServices(final Set<Service> removedServices, final PeerID source) {
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
		
		for (final Connection connection : getAllConnections()) {
			final Map<PeerIDSet, Set<Service>> partialNotifications = connection.removeServices(removedServices, source);
			for (final Entry<PeerIDSet, Set<Service>> entry : partialNotifications.entrySet()) {
				final PeerIDSet peers = entry.getKey();
				if (!notifications.containsKey(peers))
					notifications.put(peers, new HashSet<Service>());
				notifications.get(peers).addAll(entry.getValue());
			}
		}
		return notifications;
	}

	private Set<Connection> getAllConnections() {
		//include valid & invalid connections
		final Set<Connection> checkedConnections = new HashSet<Connection>(detectedConnections);
		checkedConnections.addAll(invalidConnections.getEntries());
		return checkedConnections;
	}

	/**
	 * Checks and removes invalid connections.
	 * 
	 * @param removedParamters
	 *            the parameters which have been removed.
	 * @return the invalid collisions
	 */
	public Set<Connection> checkCollisions(final Set<Parameter> removedParameters) {
		final Set<Connection> connections = new HashSet<Connection>();
		for (final Iterator<Connection> it = detectedConnections.iterator(); it.hasNext();) {
			final Connection connection = it.next();
			final Collision collision = connection.getCollision();
			if (removedParameters.contains(collision.getInput()) || removedParameters.contains(collision.getOutput())) {
				connections.add(connection);
				it.remove();
				invalidConnections.addEntry(connection);
			}
		}

		return connections;
	}

	public Map<PeerIDSet, Set<Service>> getNotifications(final Set<Connection> connections) {
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();
		for (final Connection connection : connections) {
			final Map<PeerIDSet, Set<Service>> partialNotifications = connection.getAllNotifications();
			for (final Entry<PeerIDSet, Set<Service>> entry : partialNotifications.entrySet()) {
				final PeerIDSet peers = entry.getKey();
				if (!notifications.containsKey(peers))
					notifications.put(peers, new HashSet<Service>());
				notifications.get(peers).addAll(entry.getValue());
			}
		}

		return notifications;
	}
	
	public boolean contains(final Collision collision) {
		return detectedConnections.contains(new Connection(collision, taxonomy, graphType)); 
	}
}
