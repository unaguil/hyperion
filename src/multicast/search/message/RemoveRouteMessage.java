package multicast.search.message;

import java.util.HashSet;
import java.util.Set;

import peer.PeerID;
import peer.message.MessageID;

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

	/**
	 * Constructor of the remove route message.
	 * 
	 * @param source
	 *            the source of the message
	 * @param lostRoutes
	 *            the routes which have to be removed
	 */
	public RemoveRouteMessage(final PeerID source, final Set<MessageID> lostRoutes) {
		super(source);
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
	public RemoveRouteMessage(final RemoveRouteMessage removeRouteMessage, final PeerID sender, final int newDistance) {
		super(removeRouteMessage, sender, newDistance);
		this.lostRoutes.addAll(removeRouteMessage.lostRoutes);
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
}
