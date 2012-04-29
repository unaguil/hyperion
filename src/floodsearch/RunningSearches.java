package floodsearch;

import graphcreation.services.Service;
import graphsearch.SearchID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import util.timer.Timer;
import util.timer.TimerTask;

public class RunningSearches implements TimerTask {
	
	private static final long EXPIRATION_CHECK_TIME = 1000;
	private final Timer searchExpirationTimer = new Timer(EXPIRATION_CHECK_TIME, this);
	
	private final Map<SearchID, RunningSearch> runningSearches = new HashMap<SearchID, RunningSearch>();
	
	private final long searchExpiration;
	
	public RunningSearches(final long searchExpiration) {
		this.searchExpiration = searchExpiration;
	}
	
	public void addRunningSearch(final SearchID searchID, final Service service, final Service initService,
			final Service goalService, final InitCompositionListener initCompositionListener, final long msgInterval) {
		synchronized(runningSearches) {
			runningSearches.put(searchID, new RunningSearch(searchID, service, initService, goalService, System.currentTimeMillis(), initCompositionListener, msgInterval));
		}
	}
	
	public void start() {
		searchExpirationTimer.start();
	}
	
	public void stopAndWait() {
		stopAllSearches();
		searchExpirationTimer.stopAndWait();
	}
	
	public Set<Service> getGoalServices() {
		final Set<Service> goalServices = new HashSet<Service>();
		synchronized (runningSearches) {
			for (final RunningSearch runningSearch : runningSearches.values()) {
				goalServices.add(runningSearch.getGoalService());
			}
		}
		return goalServices;
	}
	
	public RunningSearch getRunningSearch(final SearchID searchID) {
		synchronized (runningSearches) {
			if (runningSearches.containsKey(searchID))
				return runningSearches.get(searchID);
		}
		return null;
	}
	
	@Override
	public void perform() throws InterruptedException {
		synchronized(runningSearches) {
			for (Iterator<Entry<SearchID, RunningSearch>> entryIt = runningSearches.entrySet().iterator(); entryIt.hasNext(); ) {
				final Entry<SearchID, RunningSearch> entry = entryIt.next();
				final long elapsedTime = System.currentTimeMillis() - entry.getValue().getStartTime();
				if (elapsedTime > searchExpiration) {
					entry.getValue().stopTimer();
					entryIt.remove();
				}
			}
		}
	}
	
	private void stopAllSearches() {
		synchronized(runningSearches) {
			for (final RunningSearch runningSearch : runningSearches.values())
				runningSearch.stopTimer();
		}
	}
	
	public void stopSearch(final SearchID searchID) {
		synchronized(runningSearches) {
			runningSearches.get(searchID).stopTimer();
		}
	}
}
