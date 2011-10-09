package graphsearch.compositionData;

import graphcreation.GraphCreator;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.compositionData.localSearchesTable.LocalSearchesTable;
import graphsearch.compositionData.localSearchesTable.LocalSearchesTable.SearchStatus;
import graphsearch.compositionData.localSearchesTable.RelatedData;
import graphsearch.compositionData.localSearchesTable.SearchExpiredListener;

import java.util.Set;

import util.timer.Timer;
import util.timer.TimerTask;

public abstract class CompositionData implements TimerTask {

	private final LocalSearchesTable localSearchesTable = new LocalSearchesTable();

	private final Timer cleaningThread;

	private final SearchExpiredListener searchExpiredListener;
	
	protected final GraphCreator gCreator;

	public CompositionData(final long checkTime, final SearchExpiredListener searchExpiredListener, final GraphCreator gCreator) {
		this.cleaningThread = new Timer(checkTime, this);
		this.searchExpiredListener = searchExpiredListener;
		this.gCreator = gCreator;
	}

	public void start() {
		cleaningThread.start();
	}

	public void addWaitingSearch(final SearchID searchID) {
		localSearchesTable.addWaitingSearch(searchID);
	}

	public void addRunningSearch(final SearchID searchID, final Service initService, final Service goalService, final int ttl, final long remainingTime) {
		localSearchesTable.addRunningSearch(searchID, initService, goalService, ttl, remainingTime);
	}

	public void stopAndWait() {
		cleaningThread.stopAndWait();
	}

	public SearchStatus getSearchStatus(final SearchID searchID) {
		return localSearchesTable.getStatus(searchID);
	}

	@Override
	public void perform() {
		// remove local searches
		final Set<SearchID> expiredLocalSearches = localSearchesTable.cleanExpiredSearches();
		synchronized (searchExpiredListener) {
			searchExpiredListener.expiredSearches(expiredLocalSearches);
		}
	}

	public int getMaxTTL(final SearchID searchID) {
		if (!localSearchesTable.getStatus(searchID).equals(SearchStatus.RUNNING))
			return 0;
		return localSearchesTable.getRelatedData(searchID).getMaxTTL();
	}

	public long getRemainingInitTime(final SearchID searchID) {
		if (!localSearchesTable.getStatus(searchID).equals(SearchStatus.RUNNING))
			return 0;
		return localSearchesTable.getRelatedData(searchID).getRemainingTime();
	}

	public SearchID getGoalRelatedSearch(final Service goal) {
		for (final SearchID searchID : localSearchesTable.getRunningSearches()) {
			final RelatedData relatedServices = localSearchesTable.getRelatedData(searchID);
			if (relatedServices.getGoal().equals(goal))
				return searchID;
		}
		return null;
	}

	public SearchID getInitRelatedSearch(final Service init) {
		for (final SearchID searchID : localSearchesTable.getRunningSearches()) {
			final RelatedData relatedServices = localSearchesTable.getRelatedData(searchID);
			if (relatedServices.getInit().equals(init))
				return searchID;
		}
		return null;
	}
}
