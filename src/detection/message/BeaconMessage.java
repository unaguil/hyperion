package detection.message;

import message.BroadcastMessage;
import peer.PeerID;

/**
 * A broadcast message which works as beacon.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class BeaconMessage extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a beacon message.
	 * 
	 * @param sender
	 *            the sender of the message
	 * @param sentTime
	 *            the time when the message was sent
	 */
	public BeaconMessage(final PeerID sender) {
		super(sender);
	}

	@Override
	public String toString() {
		return super.toString() + " " + getType();
	}
}
