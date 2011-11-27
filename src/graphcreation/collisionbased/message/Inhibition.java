package graphcreation.collisionbased.message;

import graphcreation.collisionbased.collisiondetector.Collision;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class Inhibition implements Externalizable {
	
	private final Collision collision;
	private final PeerID notApplied;
	
	public Inhibition() {
		this.collision = null;
		this.notApplied = null;
	}
	
	public Inhibition(final Collision collision, PeerID notApplied) {
		this.collision = collision;
		this.notApplied = notApplied;
	}
	
	public Collision getCollision() {
		return collision;
	}
	
	public PeerID getNotAppliedTo() {
		return notApplied;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(Inhibition.class, this, "collision", in.readObject());
		UnserializationUtils.setFinalField(Inhibition.class, this, "notApplied", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(collision);
		out.writeObject(notApplied);
	}
	
	@Override
	public String toString() {
		return "[" + collision + ", notApplied: " + notApplied + "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Inhibition))
			return false;
		
		Inhibition inhibition = (Inhibition)o;
		return this.collision.equals(inhibition.collision) && this.notApplied.equals(inhibition.notApplied);
	}
	
	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + this.collision.hashCode();
		result = 37 * result + this.notApplied.hashCode();

		return result; 
	}
}
