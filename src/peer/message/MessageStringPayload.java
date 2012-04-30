package peer.message;

import java.util.Collections;

import peer.peerid.PeerID;

public class MessageStringPayload extends MessageString {
	
	public MessageStringPayload() {
		
	}

	public MessageStringPayload(final PeerID sender, final String content) {
		super(sender, Collections.<PeerID> emptySet(), content);
	}

	@Override
	public BroadcastMessage copy() {
		return new MessageStringPayload(getSender(), toString());
	}
}
