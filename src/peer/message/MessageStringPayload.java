package peer.message;

import java.util.Collections;

import peer.peerid.PeerID;

public class MessageStringPayload extends MessageString implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public MessageStringPayload() {
		
	}

	public MessageStringPayload(final PeerID sender, final String content) {
		super(sender, Collections.<PeerID> emptySet(), content);
	}

	@Override
	public PayloadMessage copy() {
		return new MessageStringPayload(getSender(), toString());
	}
}
