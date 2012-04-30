package peer;

import peer.message.BroadcastMessage;
import detection.NeighborDetector;

public interface ReliablePeer extends Peer {

	public NeighborDetector getDetector();

	public void enqueueBroadcast(BroadcastMessage message, CommunicationLayer layer);
	
}
