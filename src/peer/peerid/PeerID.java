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
	
	private final String id;

	public static final PeerID VOID_PEERID = new PeerID("VOID");
	
	public PeerID() {
		id = null;
	}

	public PeerID(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof PeerID))
			return false;
		final PeerID peerID = (PeerID) o;
		return peerID.id.equals(this.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public int compareTo(final PeerID peerID) {
		return this.id.compareTo(peerID.id);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(PeerID.class, this, "id", in.readUTF());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(id);
	}
}
