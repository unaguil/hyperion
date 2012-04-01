package taxonomy.parameter;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import serialization.binary.UnserializationUtils;
import taxonomy.Taxonomy;

public abstract class Parameter implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final short value;

	public Parameter() {
		value = 0;
	}
	
	public Parameter(final short value) {
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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(Parameter.class, this, "value", in.readShort());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeShort(value);		
	}
}
