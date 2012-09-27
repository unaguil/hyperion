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

package multicast.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import multicast.search.message.SearchMessage;
import peer.peerid.PeerID;

class SearchMessageReceivers {

	private final Map<SearchMessage, Set<PeerID>> receiversTable = new HashMap<SearchMessage, Set<PeerID>>(); 	
	
	public void addReceivers(SearchMessage searchMessage, Collection<PeerID> neighbors) {
		if (!receiversTable.containsKey(searchMessage))
			receiversTable.put(searchMessage, new HashSet<PeerID>());
		
		receiversTable.get(searchMessage).addAll(neighbors);
	}
	
	public void removeNeighbors(Set<PeerID> neighbors) {
		for (Set<PeerID> receivers : receiversTable.values())
			receivers.remove(neighbors);
	}
	
	public Set<PeerID> getReceivers(SearchMessage searchMessage) {
		if (!receiversTable.containsKey(searchMessage))
			return new HashSet<PeerID>();
		
		return receiversTable.get(searchMessage);
	}

	public void retainAll(List<SearchMessage> activeSearches) {
		for (Iterator<SearchMessage> it = receiversTable.keySet().iterator(); it.hasNext(); ) {
			SearchMessage searchMessage = it.next();
			if (!activeSearches.contains(searchMessage))
				it.remove();
		}
	}
}
