package peer.peerid;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import serialization.binary.UnserializationUtils;

/**
 * This class is used for peer identification. It is implemented through the
 * usage of a string identifier.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public final class PeerID implements Externalizable, Comparable<PeerID> {

	private static final long serialVersionUID = 1L;
	
	private static final int DEFAULT_VALUE = Integer.MIN_VALUE;
	
	private final int id;

	public static final PeerID VOID_PEERID = new PeerID(Integer.MIN_VALUE);
	
	public PeerID() {
		id = DEFAULT_VALUE;
	}

	public PeerID(final int id) {
		this.id = id;
	}
	
	public PeerID(final String id) {
		this.id = Integer.parseInt(id);
	}

	@Override
	public String toString() {
		return Integer.toString(id);	
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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(PeerID.class, this, "id", in.readInt());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(id);
	}
}
