package graphcreation.collisionbased.sdg.sdgtaxonomy;

import multicast.search.Route;
import peer.peerid.PeerID;

public class IndirectRoute implements Route {

	private final PeerID dest;
	private final PeerID collisionNode;
	private final Integer distance;
	private final long timestamp;
	
	public IndirectRoute(final PeerID dest, final PeerID collisionNode, Integer distance) {
		this.dest = dest;
		this.collisionNode = collisionNode;
		this.distance = distance;
		this.timestamp = System.currentTimeMillis();
	}

	@Override
	public PeerID getDest() {
		return dest;
	}

	@Override
	public PeerID getThrough() {
		return collisionNode;
	}

	@Override
	public int getDistance() {
		return distance.intValue();
	}
	
	@Override
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof IndirectRoute))
			return false;
		
		IndirectRoute indirectRoute = (IndirectRoute)o;
		return indirectRoute.dest.equals(this.dest) && indirectRoute.collisionNode.equals(this.collisionNode);
	}
	
	@Override
	public String toString() {
		return "D:" + dest + " T:" + collisionNode; 
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + dest.hashCode();
		result = 37 * result + collisionNode.hashCode();
		return result;
	}
}
