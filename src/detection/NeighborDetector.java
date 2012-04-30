package detection;

import peer.CommunicationLayer;
import peer.peerid.PeerIDSet;

/**
 * Interface which defines the methods of neighbor detectors
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface NeighborDetector extends CommunicationLayer {

	/**
	 * Gets the current neighbor list.
	 * 
	 * @return the current neighbor list
	 */
	public PeerIDSet getCurrentNeighbors();

	/**
	 * Adds a new neighbor listener to the neighbor detector. The listener will
	 * be notified when neighbors appear or disappear.
	 * 
	 * @param listener
	 */
	public void addNeighborListener(NeighborEventsListener listener);
}
