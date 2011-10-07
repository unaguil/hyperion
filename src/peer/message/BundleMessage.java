package peer.message;

import java.util.ArrayList;
import java.util.List;

import peer.PeerID;

public class BundleMessage extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();

	public BundleMessage(final PeerID sender, List<BroadcastMessage> messages) {
		super(sender);
		this.messages.addAll(messages);
	}

	public List<BroadcastMessage> getMessages() {
		return messages;
	}

	@Override
	public String toString() {
		return getType() + " [" + messages.size() + "]";
	}
}
