package graphsearch.compositionData.localSearchesTable;

import graphsearch.SearchID;

import java.util.Set;

public interface SearchExpiredListener {

	public void expiredSearches(Set<SearchID> searches);
}
