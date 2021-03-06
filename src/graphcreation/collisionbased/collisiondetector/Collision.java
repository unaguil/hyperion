/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphcreation.collisionbased.collisiondetector;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import peer.message.UnsupportedTypeException;
import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

public class Collision implements BSerializable {

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
	public void read(ObjectInputStream in) throws IOException {
		try {
			final Parameter pInput = Parameter.readParameter(in);
			SerializationUtils.setFinalField(Collision.class, this, "input", pInput);
			final Parameter pOutput = Parameter.readParameter(in);
			SerializationUtils.setFinalField(Collision.class, this, "output", pOutput);
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		input.write(out);
		output.write(out);
	}
}
