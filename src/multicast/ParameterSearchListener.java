package multicast;

import java.util.Set;

import multicast.search.message.SearchResponseMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.peerid.PeerID;
import taxonomy.parameter.Parameter;

/**
 * This interface defines those methods which are called by multicast layer in
 * order to notify search events
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface ParameterSearchListener extends MulticastMessageListener {

	public BroadcastMessage searchReceived(Set<Parameter> foundParameters, MessageID routeID);

	/**
	 * This method is called when searched parameters is found in some node
	 * 
	 * @param message
	 *            the response message sent by the node which has the searched
	 *            parameter
	 */
	public void parametersFound(SearchResponseMessage message);
	
	public void lostDestinations(Set<PeerID> lostDestinations);
	
	public void searchCanceled(Set<MessageID> canceledSearches);
}
