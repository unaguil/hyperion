package peer;

import java.util.List;

import peer.message.BroadcastMessage;
import peer.message.MessageReceivedListener;

/**
 * This interface defines a layer which can be registered in the reliable
 * broadcast peer.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface CommunicationLayer extends MessageReceivedListener {

	/**
	 * This method is called in order to initialize the layer. Layer activity,
	 * (e.g. thread starting) should be performed by this method.
	 */
	public void init();

	/**
	 * This method is called in order to stop the layer. Any thread used by the
	 * layer should be finalized by this method.
	 */
	public void stop();

	public BroadcastMessage isDuplicatedMessage(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage);
}
