package graphsearch;

import peer.message.MessageID;
import peer.peerid.PeerID;

public class SearchID extends MessageID {
	
	private static short searchCounter = 0;
	
	public SearchID() {
	}
	
	public SearchID(final PeerID startPeer, final short id) {
		super(startPeer, id);
	}

	public SearchID(final PeerID startPeer) {
		super(startPeer, searchCounter++);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof SearchID))
			return false;

		return super.equals(o);
	}
}
