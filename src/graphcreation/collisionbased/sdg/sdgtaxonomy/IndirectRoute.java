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

package graphcreation.collisionbased.sdg.sdgtaxonomy;

import multicast.search.Route;
import peer.peerid.PeerID;

public class IndirectRoute implements Route {

	private final PeerID dest;
	private final PeerID collisionNode;
	private final Integer distance;
	private final long timestamp;
	
	public IndirectRoute(final PeerID dest, final PeerID collisionNode, Integer distance) {
		this.dest = dest;
		this.collisionNode = collisionNode;
		this.distance = distance;
		this.timestamp = System.currentTimeMillis();
	}

	@Override
	public PeerID getDest() {
		return dest;
	}

	@Override
	public PeerID getThrough() {
		return collisionNode;
	}

	@Override
	public int getDistance() {
		return distance.intValue();
	}
	
	@Override
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof IndirectRoute))
			return false;
		
		IndirectRoute indirectRoute = (IndirectRoute)o;
		return indirectRoute.dest.equals(this.dest) && indirectRoute.collisionNode.equals(this.collisionNode);
	}
	
	@Override
	public String toString() {
		return "D:" + dest + " T:" + collisionNode; 
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + dest.hashCode();
		result = 37 * result + collisionNode.hashCode();
		return result;
	}
}
