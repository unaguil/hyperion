package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
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
public class GeneralizeSearchMessage extends RemoteMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the new parameters
	private final Set<Parameter> parameters = new HashSet<Parameter>();

	// the route identifiers which parameters are generalized
	private final Set<MessageID> routeIDs = new HashSet<MessageID>();
	
	public GeneralizeSearchMessage() {
		
	}

	/**
	 * Constructor of the class.
	 * 
	 * @param parameters
	 *            the generalized parameters
	 * @param routeIDs
	 *            the identifier of the routes parameters whose parameters are
	 *            generalized
	 * @param source
	 *            the source of the message
	 */
	public GeneralizeSearchMessage(final PeerID source, final Set<PeerID> expectedDestinations, final Set<Parameter> parameters, final Set<MessageID> routeIDs) {
		super(source, expectedDestinations);
		this.parameters.addAll(parameters);
		this.routeIDs.addAll(routeIDs);
	}

	/**
	 * Constructor of the message using another message as base.
	 * 
	 * @param generalizeSearchMessage
	 *            the message to use as base
	 * @param sender
	 *            the new sender of the message
	 * @param through
	 *            the neighbor used to send the message
	 * @param newDistance
	 *            the new distance for this message
	 */
	public GeneralizeSearchMessage(final GeneralizeSearchMessage generalizeSearchMessage, final PeerID sender, final Set<PeerID> expectedDestinations, final int newDistance) {
		super(generalizeSearchMessage, sender, expectedDestinations, newDistance);
		this.routeIDs.addAll(generalizeSearchMessage.routeIDs);
		this.parameters.addAll(generalizeSearchMessage.parameters);
	}

	/**
	 * Gets the generalized parameters
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

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		parameters.addAll(Arrays.asList((Parameter[])in.readObject()));
		routeIDs.addAll(Arrays.asList((MessageID[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(parameters.toArray(new Parameter[0]));
		out.writeObject(routeIDs.toArray(new MessageID[0]));
	}
}
