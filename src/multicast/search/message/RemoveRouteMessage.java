package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import peer.message.MessageID;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

/**
 * This class defines a message which is used to remove invalid routes
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoveRouteMessage extends RemoteMessage {
	
	private final Set<MessageID> lostRoutes = new HashSet<MessageID>();
	
	public RemoveRouteMessage() {
		super(MessageTypes.REMOVE_ROUTE_MESSAGE);
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
		super(MessageTypes.REMOVE_ROUTE_MESSAGE, source, null, expectedDestinations);
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
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readMessageIDs(lostRoutes, in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(lostRoutes, out);
	}
}
