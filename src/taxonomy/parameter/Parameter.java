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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package taxonomy.parameter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import peer.message.UnsupportedTypeException;

import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;
import taxonomy.Taxonomy;

public abstract class Parameter implements BSerializable {

	private final byte type;
	private final short value;
	
	public static final byte INPUT_PARAMETER = 0x01;
	public static final byte OUTPUT_PARAMETER = 0x02;

	public Parameter(final byte type) {
		this.type = type;
		this.value = 0;
	}
	
	public Parameter(final byte type, final short value) {
		this.type = type;
		this.value = value;
	}

	public short getID() {
		return value;
	}
	
	public String pretty(final Taxonomy taxonomy) {
		return taxonomy.decode(value);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Parameter))
			return false;

		final Parameter p = (Parameter) o;
		return this.value == p.value;
	}

	@Override
	public int hashCode() {
		return value;
	}
	
	@Override
	public String toString() {
		return "" + value;
	}

	@Override
	public void read(final ObjectInputStream in) throws IOException {
		SerializationUtils.setFinalField(Parameter.class, this, "value", in.readShort());
	}

	@Override
	public void write(final ObjectOutputStream out) throws IOException {
		out.writeByte(type);
		out.writeShort(value);		
	}
	
	private static Parameter getInstance(final ObjectInputStream in) throws UnsupportedTypeException, IOException {
		final byte type = in.readByte();
		if (type == INPUT_PARAMETER)
			return  new InputParameter();
		if (type == OUTPUT_PARAMETER)
			return new OutputParameter();
		throw new UnsupportedTypeException();
	}
	
	public static Parameter readParameter(final ObjectInputStream in) throws UnsupportedTypeException, IOException {
		final Parameter p = getInstance(in);
		p.read(in);
		return p;
	}
}
