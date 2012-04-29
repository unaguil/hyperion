package taxonomy.parameter;

import taxonomy.Taxonomy;

public class InputParameter extends Parameter {
	
	public InputParameter() {
		super(INPUT_PARAMETER);
	}

	public InputParameter(final short value) {
		super(INPUT_PARAMETER, value);
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
