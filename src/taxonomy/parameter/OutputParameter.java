package taxonomy.parameter;

import taxonomy.Taxonomy;

public class OutputParameter extends Parameter {
	
	public OutputParameter() {
		super(OUTPUT_PARAMETER);
	}

	public OutputParameter(final short value) {
		super(OUTPUT_PARAMETER, value);
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
	public String pretty(final Taxonomy taxonomy) {
		return "O-" + super.pretty(taxonomy);
	}
}
