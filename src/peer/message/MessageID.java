package peer.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import peer.peerid.PeerID;
import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;

public class MessageID implements Comparable<MessageID>, BSerializable {

	private final PeerID peer;

	private final short id;
	
	public MessageID() {
		peer = new PeerID();
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
	public void read(ObjectInputStream in) throws IOException {
		peer.read(in); 
		SerializationUtils.setFinalField(MessageID.class, this, "id", in.readShort());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		peer.write(out);
		out.writeShort(id);
	}
	
	public static void main(final String args[]) {
		try {					
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ObjectOutput out = new ObjectOutputStream(baos);
			for (int i = 0; i < 200; i++)
				out.writeInt(Integer.MAX_VALUE);
		
			out.close();
			System.out.println("Serialization length: " + baos.toByteArray().length);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
