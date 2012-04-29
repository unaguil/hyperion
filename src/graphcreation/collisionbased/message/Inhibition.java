package graphcreation.collisionbased.message;

import graphcreation.collisionbased.collisiondetector.Collision;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import peer.peerid.PeerID;
import serialization.binary.BSerializable;

public class Inhibition implements BSerializable {
	
	private final Collision collision;
	private final PeerID notApplied;
	
	public Inhibition() {
		this.collision = new Collision();
		this.notApplied = new PeerID();
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
	public void read(ObjectInputStream in) throws IOException {
		collision.read(in);
		notApplied.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		collision.write(out);
		notApplied.write(out);
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
