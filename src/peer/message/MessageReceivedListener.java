package peer.message;

/**
 * This interface defines those methods related to message reception
 * notification.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface MessageReceivedListener {

	/**
	 * This method is called whenever a message is received.
	 * 
	 * @param message
	 *            the received message
	 * @param receptionTime
	 *            the time when the message was received
	 */
	public void messageReceived(BroadcastMessage message, long receptionTime);
}
