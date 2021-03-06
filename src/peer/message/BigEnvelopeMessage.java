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
