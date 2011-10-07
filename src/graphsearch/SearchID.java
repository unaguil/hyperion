package graphsearch;

import java.io.Serializable;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;

public class SearchID implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final MessageID messageID;

	public SearchID(final PeerID startPeer) {
		messageID = new MessageID(startPeer, MessageIDGenerator.getNewID());
	}

	@Override
	public int hashCode() {
		return messageID.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof SearchID))
			return false;

		final SearchID searchID = (SearchID) o;
		return this.messageID.equals(searchID.messageID);
	}

	@Override
	public String toString() {
		return messageID.toString();
	}
}
