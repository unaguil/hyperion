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

	public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage);
}
