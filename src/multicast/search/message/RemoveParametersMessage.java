package multicast.search.message;

import java.util.HashSet;
import java.util.Set;

import peer.message.MessageID;
import peer.peerid.PeerID;
import taxonomy.parameter.Parameter;

/**
 * This class defines a message used to remove a parameter route.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoveParametersMessage extends RemoteMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the set of parameters to remove
	private final Set<Parameter> parameters = new HashSet<Parameter>();

	// the route identifiers which parameters are removed from
	private final Set<MessageID> routeIDs = new HashSet<MessageID>();

	/**
	 * Constructor of the class.
	 * 
	 * @param parameters
	 *            the removed parameters
	 * @param routeIDs
	 *            the identifier of the routes parameters are removed from
	 * @param source
	 *            the source of the message
	 */
	public RemoveParametersMessage(final Set<Parameter> parameters, final Set<MessageID> routeIDs, final PeerID source) {
		super(source);
		this.parameters.addAll(parameters);
		this.routeIDs.addAll(routeIDs);
	}

	/**
	 * Constructor of the message using another message as base.
	 * 
	 * @param removeParamRouteMessage
	 *            the message to use as base
	 * @param sender
	 *            the new sender of the message
	 * @param through
	 *            the neighbor used to send the message
	 * @param newDistance
	 *            the new distance for this message
	 */
	public RemoveParametersMessage(final RemoveParametersMessage removeParamRouteMessage, final PeerID sender, final int newDistance) {
		super(removeParamRouteMessage, sender, newDistance);
		this.routeIDs.addAll(removeParamRouteMessage.routeIDs);
		this.parameters.addAll(removeParamRouteMessage.parameters);
	}

	/**
	 * Gets the removed parameters
	 * 
	 * @return the removed parameters
	 */
	public Set<Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Gets the route identifiers
	 * 
	 * @return the route identifier
	 */
	public Set<MessageID> getRouteIDs() {
		return routeIDs;
	}

	@Override
	public String toString() {
		return super.toString() + " P: " + getParameters() + " Routes: " + getRouteIDs();
	}
}
