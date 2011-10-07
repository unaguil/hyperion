package taxonomy.parameter;

public class InputParameter extends Parameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InputParameter(final String id) {
		super(id);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof InputParameter))
			return false;

		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "I-" + getID();
	}
}
