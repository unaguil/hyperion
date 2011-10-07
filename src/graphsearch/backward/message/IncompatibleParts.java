package graphsearch.backward.message;

class IncompatibleParts extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IncompatibleParts() {
	}

	public IncompatibleParts(final String arg0) {
		super(arg0);
	}

	public IncompatibleParts(final Throwable arg0) {
		super(arg0);
	}

	public IncompatibleParts(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}
}
