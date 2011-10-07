package graphcreation.collisionbased.collisiondetector;

import java.io.Serializable;

import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class Collision implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final InputParameter input;
	private final OutputParameter output;

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
}
