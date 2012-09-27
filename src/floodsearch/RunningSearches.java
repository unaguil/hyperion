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
	
	private final boolean notFirstTime;
	
	private final long searchExpiration;
	
	public RunningSearches(final long searchExpiration, final boolean notFirstTime) {
		this.searchExpiration = searchExpiration;
		this.notFirstTime = notFirstTime;
	}
	
	public void addRunningSearch(final SearchID searchID, final Service service, final Service initService,
			final Service goalService, final InitCompositionListener initCompositionListener, final long msgInterval) {
		synchronized(runningSearches) {
			runningSearches.put(searchID, new RunningSearch(searchID, service, initService, goalService, System.currentTimeMillis(), initCompositionListener, msgInterval, notFirstTime));
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
			if (runningSearches.containsKey(searchID))
				runningSearches.get(searchID).stopTimer();
		}
	}
}
