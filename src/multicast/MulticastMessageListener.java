package multicast;

import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public interface MulticastMessageListener {

	/**
	 * This method is called when a multicast message is received by this node
	 * 
	 * @param source
	 *            the message which sent the multicast message
	 * @param payload
	 *            the payload included in the multicast message
	 * @param distance
	 */
	public void multicastMessageAccepted(PeerID source, PayloadMessage payload, int distance);
}
