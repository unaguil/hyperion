package graphcreation.collisionbased.connectionManager;

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
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

public class Connection {

	// the collision represented by this connection
	private final Collision collision;

	// the responses received from those peers with inputs compatible with the
	// collision input
	private final Set<SearchResponseMessage> inputResponses = new HashSet<SearchResponseMessage>();

	// the responses received from those peers with outputs compatible with the
	// collision output
	private final Set<SearchResponseMessage> outputResponses = new HashSet<SearchResponseMessage>();

	// the taxonomy used during management
	private final Taxonomy taxonomy;

	/**
	 * Constructor of the connection. A default empty taxonomy is used.
	 * 
	 * @param collision
	 *            the collision represented by this connection
	 */
	public Connection(final Collision collision) {
		this.collision = collision;
		this.taxonomy = new BasicTaxonomy();
	}

	/**
	 * Constructor of the connection.
	 * 
	 * @param collision
	 *            the collision represented by this connection
	 * @param taxonomy
	 *            the taxonomy used during management
	 */
	public Connection(final Collision collision, final Taxonomy taxonomy) {
		this.collision = collision;
		this.taxonomy = taxonomy;
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
					final boolean added = inputResponses.add(searchResponseMessage);
					if (added && isConnected()) {
						// Notify output peers if it is connected
						for (final SearchResponseMessage outputResponse : getConnectedOutputResponses(searchResponseMessage))
							notifiedPeers.addPeer(outputResponse.getSource());
						// And the added peer
						notifiedPeers.addPeer(searchResponseMessage.getSource());
					}
					// if response contains any parameter which is subsumed by
					// the output of the collision add it as output response
				} else if (p instanceof OutputParameter && taxonomy.subsumes(collision.getOutput().getID(), p.getID())) {
					final boolean added = outputResponses.add(searchResponseMessage);
					if (added && isConnected()) {
						// Notify input peers if it is connected
						for (final SearchResponseMessage inputResponse : getConnectedInputResponses(searchResponseMessage))
							notifiedPeers.addPeer(inputResponse.getSource());
						// And the added peer
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
		if (inputResponses.isEmpty() || outputResponses.isEmpty())
			return false;

		for (final SearchResponseMessage inputResponse : inputResponses) {
			final Set<SearchResponseMessage> outputConnectedResponses = getConnectedOutputResponses(inputResponse);
			if (!outputConnectedResponses.isEmpty())
				return true;
		}
		return false;
	}

	private Set<SearchResponseMessage> getConnectedOutputResponses(final SearchResponseMessage inputResponse) {
		final Set<SearchResponseMessage> connectedOutputResponses = new HashSet<SearchResponseMessage>();
		for (final SearchResponseMessage outputResponse : outputResponses)
			for (final OutputParameter p : getOutputParameters(outputResponse))
				if (isSubsumedByAny(p, getInputParameters(inputResponse))) {
					connectedOutputResponses.add(outputResponse);
					break;
				}
		return connectedOutputResponses;
	}

	private Set<SearchResponseMessage> getConnectedInputResponses(final SearchResponseMessage outputResponse) {
		final Set<SearchResponseMessage> connectedInputResponses = new HashSet<SearchResponseMessage>();
		for (final SearchResponseMessage inputResponse : inputResponses)
			for (final OutputParameter p : getOutputParameters(outputResponse))
				if (isSubsumedByAny(p, getInputParameters(inputResponse))) {
					connectedInputResponses.add(inputResponse);
					break;
				}
		return connectedInputResponses;
	}

	private Set<InputParameter> getInputParameters(final SearchResponseMessage inputResponse) {
		final Set<InputParameter> inputParameters = new HashSet<InputParameter>();
		for (final Parameter p : inputResponse.getParameters())
			if (p instanceof InputParameter)
				inputParameters.add((InputParameter) p);
		return inputParameters;
	}

	private Set<OutputParameter> getOutputParameters(final SearchResponseMessage outputResponse) {
		final Set<OutputParameter> outputParameters = new HashSet<OutputParameter>();
		for (final Parameter p : outputResponse.getParameters())
			if (p instanceof OutputParameter)
				outputParameters.add((OutputParameter) p);
		return outputParameters;
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

		for (final SearchResponseMessage searchResponseMessage : inputResponses) {
			final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) searchResponseMessage.getPayload();
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

		for (final SearchResponseMessage searchResponseMessage : outputResponses) {
			final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) searchResponseMessage.getPayload();
			for (final Service s : collisionResponseMessage.getServices())
				outputServicesTable.add(new ServiceDistance(s, collisionResponseMessage.getDistance(s)));
		}

		return outputServicesTable;
	}

	/**
	 * Removes the specified parameters from those responses coming from the
	 * passed peers
	 * 
	 * @param the
	 *            removed parameters
	 * @param the
	 *            peers whose parameters are removed from
	 * @return a map containing the peers which must notified and the lost
	 *         services to notify
	 */
	public Map<PeerIDSet, Set<Service>> removeParameters(final Set<Parameter> parameters, final PeerID source) {
		final Map<PeerIDSet, Set<Service>> notifications = new HashMap<PeerIDSet, Set<Service>>();

		// Check input services
		final Set<Service> lostInputServices = removeInputParameters(parameters, source);
		final PeerIDSet outputPeers = getOutputPeers();
		if (!lostInputServices.isEmpty() && !outputPeers.isEmpty())
			notifications.put(outputPeers, lostInputServices);

		// Check output services
		final Set<Service> lostOutputServices = removeOutputParameters(parameters, source);
		final PeerIDSet inputPeers = getInputPeers();
		if (!lostOutputServices.isEmpty() && !inputPeers.isEmpty())
			notifications.put(inputPeers, lostOutputServices);

		return notifications;
	}

	/**
	 * Removes the responses received from the specified peers
	 * 
	 * @param routes
	 *            the responses coming from the specified routes
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
		if (!lostInputServices.isEmpty() && !outputPeers.isEmpty())
			notifications.put(outputPeers, lostInputServices);

		// Check output services
		final Set<Service> lostOutputServices = removeResponses(fromPeers, outputResponses);
		final PeerIDSet inputPeers = getInputPeers();
		// Remove those disappeared peers
		for (final PeerID peerID : fromPeers)
			inputPeers.remove(peerID);
		if (!lostOutputServices.isEmpty() && !inputPeers.isEmpty())
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

		if (removed && !outputPeers.isEmpty())
			notifications.put(outputPeers, services);

		// Check output services
		removed = removeServices(services, source, outputResponses);
		final PeerIDSet inputPeers = getInputPeers();
		if (removed && !inputPeers.isEmpty())
			notifications.put(inputPeers, services);

		return notifications;
	}

	// removes the specified parameters from responses coming from the passed
	// source. Only for input responses.
	private boolean removeServices(final Set<Service> services, final PeerID source, final Set<SearchResponseMessage> responses) {
		boolean removed = false;
		for (final Iterator<SearchResponseMessage> it = responses.iterator(); it.hasNext();) {
			final SearchResponseMessage response = it.next();
			if (response.getSource().equals(source)) {
				final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) response.getPayload();
				removed = collisionResponseMessage.removeServices(services);
				if (collisionResponseMessage.getServices().isEmpty())
					it.remove();
			}
		}
		return removed;
	}

	// removes the responses identified by the passed routes
	private Set<Service> removeResponses(final Set<PeerID> fromPeers, final Set<SearchResponseMessage> responses) {
		final Set<Service> lostServices = new HashSet<Service>();
		for (final Iterator<SearchResponseMessage> it = responses.iterator(); it.hasNext();) {
			final SearchResponseMessage response = it.next();
			if (fromPeers.contains(response.getSource())) {
				final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) response.getPayload();
				it.remove();

				// Add invalid services
				lostServices.addAll(collisionResponseMessage.getServices());
			}
		}
		return lostServices;
	}

	// removes the specified parameters from responses coming from the passed
	// source. Only for input responses.
	private Set<Service> removeInputParameters(final Set<Parameter> parameters, final PeerID source) {
		final Set<Service> lostServices = new HashSet<Service>();
		for (final Iterator<SearchResponseMessage> it = inputResponses.iterator(); it.hasNext();) {
			final SearchResponseMessage inputResponse = it.next();
			if (inputResponse.getSource().equals(source)) {
				final Set<Service> invalidServices = removeParameters(inputResponse, parameters);
				if (!invalidServices.isEmpty()) {
					if (inputResponse.getParameters().isEmpty())
						it.remove();

					// Add invalid services
					lostServices.addAll(invalidServices);
				}
			}
		}
		return lostServices;
	}

	// removes the specified parameters from the passed response message and
	// returns the services which are now invalid.
	// it also removes the invalid services from the associated collision
	// message
	private Set<Service> removeParameters(final SearchResponseMessage responseMessage, final Set<Parameter> parameters) {
		final Set<Service> invalidServices = new HashSet<Service>();
		final boolean removed = responseMessage.getParameters().removeAll(parameters);
		if (removed) {
			final CollisionResponseMessage collisionResponseMessage = (CollisionResponseMessage) responseMessage.getPayload();
			for (final Service service : collisionResponseMessage.getServices()) {
				final Set<Parameter> sParams = new HashSet<Parameter>(service.getParameters());
				if (sParams.removeAll(parameters))
					invalidServices.add(service);
			}
		}
		return invalidServices;
	}

	// removes the specified parameters from responses coming from the passed
	// source. Only for output responses
	private Set<Service> removeOutputParameters(final Set<Parameter> parameters, final PeerID source) {
		final Set<Service> lostServices = new HashSet<Service>();
		for (final Iterator<SearchResponseMessage> it = outputResponses.iterator(); it.hasNext();) {
			final SearchResponseMessage outputResponse = it.next();
			if (outputResponse.getSource().equals(source)) {
				final Set<Service> invalidServices = removeParameters(outputResponse, parameters);
				if (!invalidServices.isEmpty()) {
					if (outputResponse.getParameters().isEmpty())
						it.remove();

					// Add invalid services
					lostServices.addAll(invalidServices);
				}
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
