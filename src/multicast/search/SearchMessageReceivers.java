package multicast.search;

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
	
	public void addReceivers(SearchMessage searchMessage, Set<PeerID> neighbors) {
		if (!receiversTable.containsKey(searchMessage))
			receiversTable.put(searchMessage, neighbors);
		else
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
