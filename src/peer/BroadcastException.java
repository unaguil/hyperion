package peer;

public class BroadcastException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BroadcastException() {
	}

	public BroadcastException(final String message) {
		super(message);
	}

	public BroadcastException(final Throwable cause) {
		super(cause);
	}

	public BroadcastException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
