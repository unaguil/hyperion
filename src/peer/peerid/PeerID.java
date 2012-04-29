package peer.peerid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;

/**
 * This class is used for peer identification. It is implemented through the
 * usage of a string identifier.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public final class PeerID implements Comparable<PeerID>, BSerializable {
	
	private final int id;

	public static final PeerID VOID_PEERID = new PeerID(Integer.MIN_VALUE);
	
	public PeerID() {
		id = Integer.MIN_VALUE;
	}

	public PeerID(final String id) {
		this.id = Integer.parseInt(id);
	}
	
	public PeerID(final int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "" + id;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof PeerID))
			return false;
		final PeerID peerID = (PeerID) o;
		return peerID.id == this.id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public int compareTo(final PeerID peerID) {
		return this.id - peerID.id;
	}

	@Override
	public void read(final ObjectInputStream in) throws IOException {
		SerializationUtils.setFinalField(PeerID.class, this, "id", in.readInt());
	}

	@Override
	public void write(final ObjectOutputStream out) throws IOException {
		out.writeInt(id);
	}
}
