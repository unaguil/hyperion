package multicast.search.message;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peer.PeerID;
import peer.PeerIDSet;
import peer.message.MessageID;
import peer.message.PayloadMessage;
import taxonomy.parameter.Parameter;

/**
 * This class defines those messages which are used to send a response message.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class SearchResponseMessage extends RemoteMulticastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the set of parameters found in the source node
	private final Set<Parameter> parameters = new HashSet<Parameter>();

	// the search route identifier which this message responds to
	private final MessageID respondedRouteID;

	/**
	 * Constructor of the search response message.
	 * 
	 * @param destination
	 *            the destination of the message
	 * @param foundParameters
	 *            the parameters found in this node
	 * @param payload
	 *            the payload of the response message
	 * @param source
	 *            the source of the message
	 */
	public SearchResponseMessage(final PeerID destination, final Set<Parameter> foundParameters, final PayloadMessage payload, final PeerID source, final MessageID respondedRouteID) {
		super(new PeerIDSet(Collections.singleton(destination)), payload, source);
		this.parameters.addAll(foundParameters);
		this.respondedRouteID = respondedRouteID;
	}

	/**
	 * Constructor of the search response message. It uses another message as
	 * base.
	 * 
	 * @param searchResponseMessage
	 *            the message used as base
	 * @param sender
	 *            the new sender of the message
	 * @param through
	 *            the new peer used to send the message through
	 * @param respondingTo
	 *            the message this one is responding to
	 * @param newDistance
	 *            the new distance traversed by the message
	 */
	public SearchResponseMessage(final SearchResponseMessage searchResponseMessage, final PeerID sender, final PeerID through, final int newDistance) {
		super(searchResponseMessage, sender, new PeerIDSet(Collections.singleton(through)), newDistance);
		this.parameters.addAll(searchResponseMessage.parameters);
		this.respondedRouteID = searchResponseMessage.getRespondedRouteID();
	}

	/**
	 * Gets the parameters found in the remote node
	 * 
	 * @return a set containing the parameters found in the remote node
	 */
	public Set<Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Gets the remote destination of this message
	 * 
	 * @return the remote destination of this message
	 */

	public PeerID getRemoteDestination() {
		return getRemoteDestinations().iterator().next();
	}

	/**
	 * Gets the search route this message responds to
	 * 
	 * @return the search route this message responds to
	 */
	public MessageID getRespondedRouteID() {
		return respondedRouteID;
	}

	@Override
	public String toString() {
		return super.toString() + " P: " + getParameters();
	}
}
