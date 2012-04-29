package peer.message;

import java.util.List;

/**
 * This interface defines messages which contains other broadcast messages as a
 * payload.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface BigEnvelopeMessage {

	/**
	 * Gets the payload of the message
	 * 
	 * @return the payload of the message
	 */
	public List<BroadcastMessage> getPayloadMessages();

	/**
	 * Checks if the message contains a payload
	 * 
	 * @return true if the message contains a payload, false otherwise
	 */
	public boolean hasPayload();
}
