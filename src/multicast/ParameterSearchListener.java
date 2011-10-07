package multicast;

import java.util.Map;
import java.util.Set;

import multicast.search.message.SearchMessage;
import multicast.search.message.SearchResponseMessage;
import peer.message.MessageID;
import peer.message.PayloadMessage;
import taxonomy.parameter.Parameter;

/**
 * This interface defines those methods which are called by multicast layer in
 * order to notify search events
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface ParameterSearchListener extends MulticastMessageListener {

	/**
	 * This method is called when a search message is received by this node. It
	 * supports the inclusion of a response as payload in lower layer messages.
	 * 
	 * @param message
	 *            the search message received
	 * @return the response message which could be included in other message as
	 *         payload
	 */
	public PayloadMessage searchMessageReceived(SearchMessage message);

	/**
	 * This method is called when searched parameters is found in some node
	 * 
	 * @param message
	 *            the response message sent by the node which has the searched
	 *            parameter
	 */
	public void parametersFound(SearchResponseMessage message);

	public void changedParameterRoutes(Map<MessageID, Set<Parameter>> lostParameters, Set<MessageID> lostParameterRoutes, Map<MessageID, MessageID> routeAssociations);

	public void changedSearchRoutes(Map<MessageID, Set<Parameter>> changedSearchRoutes, Set<MessageID> lostSearchRoutes);
}
