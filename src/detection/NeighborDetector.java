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

package detection;

import java.util.Set;

import peer.CommunicationLayer;
import peer.peerid.PeerID;

/**
 * Interface which defines the methods of neighbor detectors
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface NeighborDetector extends CommunicationLayer {

	/**
	 * Gets the current neighbor list.
	 * 
	 * @return the current neighbor list
	 */
	public Set<PeerID> getCurrentNeighbors();

	/**
	 * Adds a new neighbor listener to the neighbor detector. The listener will
	 * be notified when neighbors appear or disappear.
	 * 
	 * @param listener
	 */
	public void addNeighborListener(NeighborEventsListener listener);
}
