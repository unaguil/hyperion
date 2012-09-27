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
