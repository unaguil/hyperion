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

package dissemination;

import java.util.List;
import java.util.Map;
import java.util.Set;

import detection.NeighborEventsListener;

import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
import taxonomy.parameter.Parameter;

/**
 * This interface defines those method used for table changes notification
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface TableChangedListener extends NeighborEventsListener {

	/**
	 * This method notifies changes in the parameters of the local table
	 * 
	 * @param neighbor
	 *            the neighbor where the changes came from
	 * @param newParameters
	 *            the added parameters
	 * @param removedParameters
	 *            the removed parameters
	 * @param removedLocalParameters
	 *            the set of removed parameters which where local
	 * @param addedParameters TODO
	 * @param changedParamaters
	 *            those parameters whose distance has changed (but not
	 *            completely removed).
	 * @return the payload will be included in the table message by the
	 *         dissemination layer
	 */
	public BroadcastMessage parametersChanged(PeerID neighbor, Set<Parameter> newParameters, Set<Parameter> removedParameters, 
											Set<Parameter> removedLocalParameters, Map<Parameter, DistanceChange> changedParameters,
											Set<Parameter> addedParameters, List<BroadcastMessage> payloadMessages);
}
