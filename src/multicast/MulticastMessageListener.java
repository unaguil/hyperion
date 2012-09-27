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
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package multicast;

import peer.message.BroadcastMessage;
import peer.peerid.PeerID;

public interface MulticastMessageListener {

	/**
	 * This method is called when a multicast message is received by this node
	 * 
	 * @param source
	 *            the message which sent the multicast message
	 * @param payload
	 *            the payload included in the multicast message
	 * @param distance
	 */
	public void multicastMessageAccepted(PeerID source, BroadcastMessage payload, int distance, boolean directBroadcast);
}
