package peer;

/**
 * This exception is thrown when a listener is registered again for a previously
 * message class.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class AlreadyRegisteredListenerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AlreadyRegisteredListenerException() {
	}

	public AlreadyRegisteredListenerException(final String message) {
		super(message);
	}

	public AlreadyRegisteredListenerException(final Throwable cause) {
		super(cause);
	}

	public AlreadyRegisteredListenerException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
