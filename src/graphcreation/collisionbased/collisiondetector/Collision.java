package graphcreation.collisionbased.collisiondetector;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import serialization.binary.UnserializationUtils;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class Collision implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final InputParameter input;
	private final OutputParameter output;
	
	public Collision() {
		input = null;
		output = null;
	}

	public Collision(final InputParameter input, final OutputParameter output) {
		this.input = input;
		this.output = output;
	}

	public InputParameter getInput() {
		return input;
	}

	public OutputParameter getOutput() {
		return output;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Collision))
			return false;

		final Collision collision = (Collision) o;
		return this.input.equals(collision.input) && this.output.equals(collision.output);
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + this.input.hashCode();
		result = 37 * result + this.output.hashCode();

		return result;
	}

	@Override
	public String toString() {
		return "[" + output + "->" + input + "]";
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(Collision.class, this, "input", in.readObject());
		UnserializationUtils.setFinalField(Collision.class, this, "output", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(input);
		out.writeObject(output);
	}
}
