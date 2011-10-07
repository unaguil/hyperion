package detection;

import peer.PeerIDSet;

/**
 * This interface defines those methods which are used to notify neighbor
 * events.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface NeighborEventsListener {

	/**
	 * Called when new neighbors have appeared
	 * 
	 * @param neighbors
	 *            the new appeared neighbors
	 */
	public void appearedNeighbors(PeerIDSet neighbors);

	/**
	 * Called when some neighbors have disappeared
	 * 
	 * @param neighbors
	 *            the disappeared neighbors
	 */
	public void dissapearedNeighbors(PeerIDSet neighbors);
}
