package multicast.search;

import peer.peerid.PeerID;

public interface Route {

	public PeerID getDest();

	public int getDistance();

	public PeerID getThrough();

	public long getTimestamp();

}