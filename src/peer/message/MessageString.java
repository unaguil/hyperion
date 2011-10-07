package peer.message;

import peer.PeerID;


/**
 * Example implementation of a message.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class MessageString extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String content;

	public MessageString(final PeerID sender, final String content) {
		super(sender);
		this.content = content;
	}

	@Override
	public String toString() {
		return content;
	}
}
