package peer.message;

public class UnsupportedTypeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnsupportedTypeException() {
	}

	public UnsupportedTypeException(String message) {
		super(message);
	}

	public UnsupportedTypeException(Throwable cause) {
		super(cause);
	}

	public UnsupportedTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}
