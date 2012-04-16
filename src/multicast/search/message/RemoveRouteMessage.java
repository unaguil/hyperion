package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import peer.message.MessageID;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

/**
 * This class defines a message which is used to remove invalid routes
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoveRouteMessage extends RemoteMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Set<MessageID> lostRoutes = new HashSet<MessageID>();
	
	public RemoveRouteMessage() {
	}

	/**
	 * Constructor of the remove route message.
	 * 
	 * @param source
	 *            the source of the message
	 * @param lostRoutes
	 *            the routes which have to be removed
	 */
	public RemoveRouteMessage(final PeerID source, final Set<PeerID> expectedDestinations, final Set<MessageID> lostRoutes) {
		super(source, expectedDestinations);
		this.lostRoutes.addAll(lostRoutes);
	}

	/**
	 * Constructor of the remove route message using another message as base,
	 * 
	 * @param removeRouteMessage
	 *            the message used as base
	 * @param sender
	 *            the new sender of the message
	 * @param newDistance
	 *            the new distance traveled by the message
	 */
	public RemoveRouteMessage(final RemoveRouteMessage removeRouteMessage, final PeerID sender, final Set<PeerID> expectedDestinations, final Set<MessageID> lostRoutes, final int newDistance) {
		super(removeRouteMessage, sender, expectedDestinations, newDistance);
		this.lostRoutes.addAll(lostRoutes);
	}

	/**
	 * Gets the routes which have been lost
	 * 
	 * @return the lost routes
	 */
	public Set<MessageID> getLostRoutes() {
		return lostRoutes;
	}
	
	@Override
	public String toString() {
		return super.toString() + " (LR: " + lostRoutes + ")";
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		lostRoutes.addAll(Arrays.asList((MessageID[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(lostRoutes.toArray(new MessageID[0]));
	}
}
