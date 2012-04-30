package peer.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class MessageID implements Externalizable, Comparable<MessageID> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final PeerID peer;

	private final short id;
	
	public MessageID() {
		peer = null;
		id = 0;
	}

	public MessageID(final PeerID peer, final short id) {
		this.peer = peer;
		this.id = id;
	}

	public PeerID getPeer() {
		return peer;
	}

	public short getID() {
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

		result = 37 * result + id;
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

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(MessageID.class, this, "peer", in.readObject());
		UnserializationUtils.setFinalField(MessageID.class, this, "id", in.readShort());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(peer);
		out.writeShort(id);
	}
}
