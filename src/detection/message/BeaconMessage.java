package detection.message;


import java.util.Collections;

import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.peerid.PeerID;

/**
 * A broadcast message which works as beacon.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class BeaconMessage extends BroadcastMessage {
	
	public BeaconMessage() {
		super(MessageTypes.BEACON_MESSAGE);
	}

	/**
	 * Constructs a beacon message.
	 * 
	 * @param sender
	 *            the sender of the message
	 * @param sentTime
	 *            the time when the message was sent
	 */
	public BeaconMessage(final PeerID sender) {
		super(MessageTypes.BEACON_MESSAGE, sender, Collections.<PeerID>emptySet());
	}

	@Override
	public String toString() {
		return getType() + " " + getMessageID();
	}
}
