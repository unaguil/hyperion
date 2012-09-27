/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package multicast;

import java.util.Set;

import detection.NeighborEventsListener;

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
public interface ParameterSearchListener extends MulticastMessageListener, NeighborEventsListener {

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
