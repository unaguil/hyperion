package taxonomy.parameter;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import serialization.binary.UnserializationUtils;

public abstract class Parameter implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String id;

	public Parameter() {
		id = null;
	}
	
	public Parameter(final String id) {
		this.id = id;
	}

	public String getID() {
		return id;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Parameter))
			return false;

		final Parameter p = (Parameter) o;
		return this.id.equals(p.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(Parameter.class, this, "id", in.readUTF());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(id);		
	}
}
