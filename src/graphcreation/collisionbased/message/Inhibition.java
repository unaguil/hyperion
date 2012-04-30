package graphcreation.collisionbased.message;

import graphcreation.collisionbased.collisiondetector.Collision;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import serialization.binary.BSerializable;

public class Inhibition implements BSerializable {
	
	private final Collision collision;
	
	public Inhibition() {
		this.collision = new Collision();
	}
	
	public Inhibition(final Collision collision) {
		this.collision = collision;
	}
	
	public Collision getCollision() {
		return collision;
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		collision.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		collision.write(out);
	}
	
	@Override
	public String toString() {
		return "[" + collision + "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Inhibition))
			return false;
		
		Inhibition inhibition = (Inhibition)o;
		return this.collision.equals(inhibition.collision);
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + this.collision.hashCode();
		return result; 
	}
}
