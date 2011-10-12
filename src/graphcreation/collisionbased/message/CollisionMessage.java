package graphcreation.collisionbased.message;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class CollisionMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CollisionMessage() {
		
	}

	public CollisionMessage(final PeerID source) {
		super(source);
	}

	@Override
	public PayloadMessage copy() {
		return new CollisionMessage(getSource());
	}
}
