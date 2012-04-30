package taxonomy.parameter;

import taxonomy.Taxonomy;

public class InputParameter extends Parameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public InputParameter() {
		
	}

	public InputParameter(final short value) {
		super(value);
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
	public String pretty(final Taxonomy taxonomy) {
		return "I-" + super.pretty(taxonomy);
	}
}
