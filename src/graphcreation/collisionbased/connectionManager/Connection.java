package graphcreation.collisionbased.connectionManager;

import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.collisionbased.message.CollisionResponseMessage;
import graphcreation.services.Service;
import graphsearch.util.Utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import multicast.search.message.SearchResponseMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

public class Connection {

	// the collision represented by this connection
	private final Collision collision;

	// the responses received from those peers with inputs compatible with the
	// collision input
	private final Set<CollisionResponseMessage> inputResponses = new HashSet<CollisionResponseMessage>();

	// the responses received from those peers with outputs compatible with the
	// collision output
	private final Set<CollisionResponseMessage> outputResponses = new HashSet<CollisionResponseMessage>();

	// the taxonomy used during management
	private final Taxonomy taxonomy;
	
	private final GraphType graphType;

	/**
	 * Constructor of the connection.
	 * 
	 * @param collision
	 *            the collision represented by this connection
	 * @param taxonomy
	 *            the taxonomy used during management
	 */
	public Connection(final Collision collision, final Taxonomy taxonomy, final GraphType graphType) {
		this.collision = collision;
		this.taxonomy = taxonomy;
		this.graphType = graphType;
	}

	/**
	 * Gets the collision represented by this connection
	 * 
	 * @return the collision represented by this connection
	 */
	public Collision getCollision() {
		return collision;
	}

	/**
	 * Gets the identifier of those peers which provide services with output
	 * parameters
	 * 
	 * @return the set of peer identifiers which provide services with output
	 *         parameters
	 */
	public PeerIDSet getOutputPeers() {
		final PeerIDSet outputPeers = new PeerIDSet();
		for (final ServiceDistance sDistance : getOutputServicesTable())
			outputPeers.addPeer(sDistance.getService().getPeerID());

		return outputPeers;
	}

	/**
	 * Gets the identifier of those peers which provide services with output
	 * parameters
	 * 
	 * @return the set of peer identifiers which provide services with output
	 *         parameters
	 */
	public PeerIDSet getInputPeers() {
		final PeerIDSet inputPeers = new PeerIDSet();
		for (final ServiceDistance serviceDistance : getInputServicesTable())
			inputPeers.addPeer(serviceDistance.getService().getPeerID());

		return inputPeers;
	}
	
	private boolean notifyInputs() {
		return graphType == GraphType.BIDIRECTIONAL || graphType == GraphType.BACKWARD;
	}
	
	private boolean notifyOutputs() {
		return graphType == GraphType.BIDIRECTIONAL || graphType == GraphType.FORWARD;
	}

	/**
	 * Adds a response message to this connection. The search response must
	 * contain a collision response message to be accepted.
	 * 
	 * @param searchResponseMessage
	 *            the search response message to add
	 * @param taxonomy
	 *            the taxonomy to use during management
	 * @return the peers which must be notified after the change
	 */
	public PeerIDSet addSearchResponse(final SearchResponseMessage searchResponseMessage) {
		final PeerIDSet notifiedPeers = new PeerIDSet();
		if (searchResponseMessage.getPayload() != null && searchResponseMessage.getPayload() instanceof CollisionResponseMessage)
			// Check if input subsumes any of the received parameters
			for (final Parameter p : searchResponseMessage.getParameters())
				// If response contains any parameter which is subsumed by the
				// input of the collision add it as input response
				if (p instanceof InputParameter && taxonomy.subsumes(collision.getInput().getID(), p.getID())) {
					final boolean added = inputResponses.add((CollisionResponseMessage) searchResponseMessage.getPayload().copy());
					if (added && isConnected()) {
						if (notifyOutputs()) {
							// Notify output peers if it is connected
							for (final CollisionResponseMessage outputResponse : getConnectedOutputResponses((CollisionResponseMessage)searchResponseMessage.getPayload()))
								notifiedPeers.addPeer(outputResponse.getSource());
						}
						// And the added input peer
						if (notifyInputs())
							notifiedPeers.addPeer(searchResponseMessage.getSource());
					}
					// if response contains any parameter which is subsumed by
					// the output of the collision add it as output response
				} else if (p instanceof OutputParameter && taxonomy.subsumes(collision.getOutput().getID(), p.getID())) {
					final boolean added = outputResponses.add((CollisionResponseMessage) searchResponseMessage.getPayload().copy());
					if (added && isConnected()) {
						if (notifyInputs()) {
							// Notify input peers if it is connected
							for (final CollisionResponseMessage inputResponse : getConnectedInputResponses((CollisionResponseMessage)searchResponseMessage.getPayload()))
								notifiedPeers.addPeer(inputResponse.getSource());
						}
						// And the added output peer
						if (notifyOutputs())
							notifiedPeers.addPeer(searchResponseMessage.getSource());
					}
				}

		return notifiedPeers;
	}

	/**
	 * Checks if the connection is connected. A connection is connected if there
	 * exists at least one response coming from the input peers and one response
	 * coming from the output peers whose parameters connect.
	 * 
	 * @return true if the connection is connected, false otherwise
	 */
	public boolean isConnected() {
		return !inputResponses.isEmpty() && !outputResponses.isEmpty();
	}

	private Set<CollisionResponseMessage> getConnectedOutputResponses(final CollisionResponseMessage inputResponse) {
		final Set<CollisionResponseMessage> connectedOutputResponses = new HashSet<CollisionResponseMessage>();
		for (final CollisionResponseMessage outputResponse : outputResponses)
			for (final OutputParameter p : outputResponse.getOutputParameters())
				if (isSubsumedByAny(p, inputResponse.getInputParameters())) {
					connectedOutputResponses.add(outputResponse);
					break;
				}
		return connectedOutputResponses;
	}

	private Set<CollisionResponseMessage> getConnectedInputResponses(final CollisionResponseMessage outputResponse) {
		final Set<CollisionResponseMessage> connectedInputResponses = new HashSet<CollisionResponseMessage>();
		for (final CollisionResponseMessage inputResponse : inputResponses)
			for (final OutputParameter p : outputResponse.getOutputParameters())
				if (isSubsumedByAny(p, inputResponse.getInputParameters())) {
					connectedInputResponses.add(inputResponse);
					break;
				}
		return connectedInputResponses;
	}

	private boolean isSubsumedByAny(final OutputParameter outputParameter, final Set<InputParameter> inputParameters) {
		for (final Parameter inputParameter : inputParameters)
			if (taxonomy.subsumes(inputParameter.getID(), outputParameter.getID()))
				return true;
		return false;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Connection))
			return false;

		final Connection connection = (Connection) o;
		return this.collision.equals(connection.collision);
	}

	/**
	 * Gets the service table which corresponds to those messages received from
	 * input peers
	 * 
	 * @return the service table which corresponds to input peers
	 */
	public Set<ServiceDistance> getInputServicesTable() {
		final Set<ServiceDistance> inputServicesTable = new HashSet<ServiceDistance>();

		for (final CollisionResponseMessage collisionResponseMessage : inputResponses) {
			for (final Service s : collisionResponseMessage.getServices())
				inputServicesTable.add(new ServiceDistance(s, collisionResponseMessage.getDistance(s)));
		}

		return inputServicesTable;
	}

	/**
	 * Gets the service table which corresponds to those messages received from
	 * output peers
	 * 
	 * @return the service table which corresponds to output peers
	 */
	public Set<ServiceDistance> getOutputServicesTable() {
		final Set<ServiceDistance> outputServicesTable = new HashSet<ServiceDistance>();

		for (final CollisionResponseMessage collisionResponseMessage : outputResponses) {
			for (final Service s : collisionResponseMessage.getServices())
				outputServicesTable.add(new ServiceDistance(s, collisionResponseMessage.getDistance(s)));
		}

		return outputServicesTable;
	}

	/**
	 * Removes the responses received from the specified peers
	 * @param routes
	 *            the responses coming from the specified routes
	 * 
	 * @return a map containing the peers which must notified and the lost
	 *         services to notify
	 */
	public Map<PeerIDSet, Set<Service>> removeResponses(final Set<PeerID> fromPeers) {
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();

		// Check input services
		final Set<Service> lostInputServices = removeResponses(fromPeers, inputResponses);
		final PeerIDSet outputPeers = getOutputPeers();
		// Remove those disappeared peers
		for (final PeerID peerID : fromPeers)
			outputPeers.remove(peerID);
		if (notifyOutputs() && !lostInputServices.isEmpty() && !outputPeers.isEmpty())
			notifications.put(outputPeers, lostInputServices);

		// Check output services
		final Set<Service> lostOutputServices = removeResponses(fromPeers, outputResponses);
		final PeerIDSet inputPeers = getInputPeers();
		// Remove those disappeared peers
		for (final PeerID peerID : fromPeers)
			inputPeers.remove(peerID);
		if (notifyInputs() && !lostOutputServices.isEmpty() && !inputPeers.isEmpty())
			notifications.put(inputPeers, lostOutputServices);

		return notifications;
	}

	/**
	 * Removes from responses the specified services
	 * 
	 * @param services
	 *            the services to remove
	 * @return a map containing the peers which must notified and the lost
	 *         services to notify
	 */
	public Map<PeerIDSet, Set<Service>> removeServices(final Set<Service> services, final PeerID source) {
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();

		// Check input services
		boolean removed = removeServices(services, source, inputResponses);
		final PeerIDSet outputPeers = getOutputPeers();

		if (notifyOutputs() && removed && !outputPeers.isEmpty())
			notifications.put(outputPeers, services);

		// Check output services
		removed = removeServices(services, source, outputResponses);
		final PeerIDSet inputPeers = getInputPeers();
		if (notifyInputs() && removed && !inputPeers.isEmpty())
			notifications.put(inputPeers, services);

		return notifications;
	}

	// removes the specified parameters from responses coming from the passed
	// source. Only for input responses.
	private boolean removeServices(final Set<Service> services, final PeerID source, final Set<CollisionResponseMessage> responses) {
		boolean removed = false;
		for (final Iterator<CollisionResponseMessage> it = responses.iterator(); it.hasNext();) {
			final CollisionResponseMessage response = it.next();
			if (response.getSource().equals(source)) {
				removed = response.removeServices(services);
				if (response.getServices().isEmpty())
					it.remove();
			}
		}
		return removed;
	}

	// removes the responses identified by the passed routes
	private Set<Service> removeResponses(final Set<PeerID> fromPeers, final Set<CollisionResponseMessage> responses) {
		final Set<Service> lostServices = new HashSet<Service>();
		for (final Iterator<CollisionResponseMessage> it = responses.iterator(); it.hasNext();) {
			final CollisionResponseMessage response = it.next();
			if (fromPeers.contains(response.getSource())) {
				it.remove();

				// Add invalid services
				lostServices.addAll(response.getServices());
			}
		}
		return lostServices;
	}

	public Map<PeerIDSet, Set<Service>> getAllNotifications() {
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();

		// create notifications for input peers
		notifications.put(getInputPeers(), Utility.getServices(getOutputServicesTable()));

		// create notifications for output peers
		notifications.put(getOutputPeers(), Utility.getServices(getInputServicesTable()));

		return notifications;
	}

	@Override
	public int hashCode() {
		return collision.hashCode();
	}

	@Override
	public String toString() {
		return collision.toString() + " inputResponses: " + inputResponses + " outputResponses: " + outputResponses;
	}
}
