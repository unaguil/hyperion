package peer;

/**
 * This exception represents a problem during communication layer registering.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RegisterCommunicationLayerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RegisterCommunicationLayerException() {
	}

	public RegisterCommunicationLayerException(final String message) {
		super(message);
	}

	public RegisterCommunicationLayerException(final Throwable cause) {
		super(cause);
	}

	public RegisterCommunicationLayerException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
