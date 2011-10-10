package taxonomy.parameter;

public class OutputParameter extends Parameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public OutputParameter() {
		
	}

	public OutputParameter(final String id) {
		super(id);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof OutputParameter))
			return false;

		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "O-" + getID();
	}
}
