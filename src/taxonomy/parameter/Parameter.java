package taxonomy.parameter;

import java.io.Serializable;

public abstract class Parameter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String id;

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
}
