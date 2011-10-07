package peer.message;

import peer.peerid.PeerIDSet;

/**
 * This interface identifies those messages which are broadcasted to a set of
 * neighbors.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface MulticastMessage {

	/**
	 * Gets the set of destination neighbors.
	 * 
	 * @return the set of destination neighbors
	 */
	public PeerIDSet getDestinations();
}
