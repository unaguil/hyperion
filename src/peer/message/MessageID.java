package peer.message;

import java.io.Serializable;

import peer.peerid.PeerID;

public class MessageID implements Serializable, Comparable<MessageID> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final PeerID peer;

	private final long id;

	public MessageID(final PeerID peer, final long id) {
		this.peer = peer;
		this.id = id;
	}

	public PeerID getPeer() {
		return peer;
	}

	public long getID() {
		return id;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof MessageID))
			return false;

		final MessageID messageID = (MessageID) o;
		return this.peer.equals(messageID.peer) && this.id == messageID.id;
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + (int) (id ^ (id >>> 32));
		result = 37 * result + peer.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "(S:" + peer + " ID:" + id + ")";
	}

	@Override
	public int compareTo(final MessageID messageID) {
		if (peer.equals(messageID.peer)) {
			if (id == messageID.id)
				return 0;
			else if (id < messageID.id)
				return -1;

			return 1;
		}

		return peer.compareTo(messageID.peer);
	}
}
