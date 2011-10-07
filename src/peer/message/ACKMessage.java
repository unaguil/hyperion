package peer.message;

import peer.peerid.PeerID;


public class ACKMessage extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final MessageID respondingTo;

	public ACKMessage(final PeerID sender, final MessageID respondingTo) {
		super(sender);
		this.respondingTo = respondingTo;
	}

	public MessageID getRespondedMessageID() {
		return respondingTo;
	}

	@Override
	public String toString() {
		return getType() + " " + respondingTo;
	}
}
