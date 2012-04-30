package graphsearch.compositionData.localSearchesTable;

import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.compositionData.ExpiredSearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalSearchesTable {

	// WAITING: a search which was enqueued but not processed
	// RUNNING: the search was processed and is currently being performed
	// UNKNOWN: no search with the specified identifier is currently known
	public enum SearchStatus {
		WAITING, RUNNING, UNKNOWN
	}

	// a list containing those searches which are known but are waiting to been
	// processed
	private final List<SearchID> waitingSearches = new ArrayList<SearchID>();

	// a map containing those searches which are currently running and their
	// related services
	private final Map<SearchID, RelatedData> runningSearches = new HashMap<SearchID, RelatedData>();

	/**
	 * Adds a new search with the waiting status;
	 * 
	 * @param searchID
	 *            the search to be added
	 */
	public void addWaitingSearch(final SearchID searchID) {
		waitingSearches.add(searchID);
	}

	/**
	 * Sets the status of the search as running. If the search already existed
	 * in the waiting list it is moved
	 * 
	 * @param searchID
	 *            the search identifier to add
	 * @param maxTTL
	 *            the maximum TTL for this search identifier
	 * @param remainingTime
	 *            the remaining time for this search to expire
	 */
	public void addRunningSearch(final SearchID searchID, final Service init, final Service goal, final int maxTTL, final long remainingTime) {
		waitingSearches.remove(searchID);
		synchronized (runningSearches) {
			runningSearches.put(searchID, new RelatedData(init, goal, maxTTL, remainingTime));
		}
	}

	/**
	 * Gets the status of the specified search
	 * 
	 * @param searchID
	 *            the identifier of the search whose status is obtained
	 * @return the status of the search
	 */
	public SearchStatus getStatus(final SearchID searchID) {
		if (waitingSearches.contains(searchID))
			return SearchStatus.WAITING;
		synchronized (runningSearches) {
			if (runningSearches.containsKey(searchID))
				return SearchStatus.RUNNING;
		}

		return SearchStatus.UNKNOWN;
	}

	/**
	 * Gets the INIT service of the specified search
	 * 
	 * @param the
	 *            specified search identifier
	 * @return the INIT service
	 */
	public Service getInitService(final SearchID searchID) {
		synchronized (runningSearches) {
			return runningSearches.get(searchID).getInit();
		}
	}

	/**
	 * Gets the goal service of the specified search
	 * 
	 * @param the
	 *            specified search identifier
	 * @return the goal service
	 */
	public Service getGoalService(final SearchID searchID) {
		synchronized (runningSearches) {
			return runningSearches.get(searchID).getGoal();
		}
	}

	/**
	 * Removes the specified search
	 * 
	 * @param the
	 *            identifier of the search to remove
	 */
	public void removeSearch(final SearchID searchID) {
		waitingSearches.remove(searchID);
		synchronized (runningSearches) {
			runningSearches.remove(searchID);
		}
	}

	/**
	 * Gets the set of running searches
	 * 
	 * @return the set of running searches
	 */
	public Set<SearchID> getRunningSearches() {
		synchronized (runningSearches) {
			return Collections.unmodifiableSet(runningSearches.keySet());
		}
	}

	/**
	 * Gets the data related to the running search
	 * 
	 * @param searchID
	 *            the search identifier
	 * @return the data related to the running search
	 */
	public RelatedData getRelatedData(final SearchID searchID) {
		synchronized (runningSearches) {
			return runningSearches.get(searchID);
		}
	}

	/**
	 * Removes those local searches which are expired
	 * 
	 * @return the set of expired searches
	 */
	public Set<ExpiredSearch> cleanExpiredSearches() {
		final Set<ExpiredSearch> expiredSearches = new HashSet<ExpiredSearch>();
		synchronized (runningSearches) {
			for (final Iterator<SearchID> it = runningSearches.keySet().iterator(); it.hasNext();) {
				final SearchID searchID = it.next();
				if (getRelatedData(searchID).getRemainingTime() == 0) {
					final Service initService = getInitService(searchID);
					final Service goalService = getGoalService(searchID);
					it.remove();
					expiredSearches.add(new ExpiredSearch(searchID, initService, goalService));
				}
			}
		}
		return expiredSearches;
	}
}
