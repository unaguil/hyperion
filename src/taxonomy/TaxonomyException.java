package taxonomy;

public class TaxonomyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TaxonomyException() {
	}

	public TaxonomyException(final String message) {
		super(message);
	}

	public TaxonomyException(final Throwable cause) {
		super(cause);
	}

	public TaxonomyException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
