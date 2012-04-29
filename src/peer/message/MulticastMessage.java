package peer.message;

import java.util.Set;

import peer.peerid.PeerID;

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
	public Set<PeerID> getDestNeighbors();
}
