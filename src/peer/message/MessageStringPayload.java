package peer.message;

import peer.PeerID;


public class MessageStringPayload extends MessageString implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MessageStringPayload(final PeerID sender, final String content) {
		super(sender, content);
	}

	@Override
	public PayloadMessage copy() {
		return new MessageStringPayload(getSender(), toString());
	}
}
