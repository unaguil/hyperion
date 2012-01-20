package multicast.search.unicastTable;

import java.util.HashSet;
import java.util.Set;

import multicast.search.message.SearchMessage;

public class SearchRemovalResult {
	
	private final Set<SearchMessage> newActiveSearches = new HashSet<SearchMessage>();
	private final boolean removed;
	
	public SearchRemovalResult(final boolean removed, final Set<SearchMessage> newActiveSearches) {
		this.removed = removed;
		this.newActiveSearches.addAll(newActiveSearches);
	}
	
	public boolean wasRemoved() {
		return removed;
	}
	
	public Set<SearchMessage> getNewActiveSearches() {
		return newActiveSearches;
	}
}
