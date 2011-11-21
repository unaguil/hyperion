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
}
