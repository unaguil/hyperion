package graphsearch.compositionData.localSearchesTable;

import graphsearch.compositionData.ExpiredSearch;

import java.util.Set;

public interface SearchExpiredListener {

	public void expiredSearches(Set<ExpiredSearch> searches);
}
