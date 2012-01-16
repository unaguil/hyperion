package detection;

import java.util.Set;

import peer.peerid.PeerID;

/**
 * This interface defines those methods which are used to notify neighbor
 * events.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface NeighborEventsListener {
	
	public void neighborsChanged(Set<PeerID> newNeighbors, Set<PeerID> lostNeighbors);
}
