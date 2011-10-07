package peer.message;

/**
 * This interface defines those methods which are called every time a message is
 * sent using the broadcast method.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface MessageSentListener {

	/**
	 * This method is called whenever a message is sent using the broadcast
	 * method defined.
	 * 
	 * @param message
	 *            the sent message
	 * @param sentTime
	 *            the time when the message was sent
	 */
	public void messageSent(BroadcastMessage message, long sentTime);
}
