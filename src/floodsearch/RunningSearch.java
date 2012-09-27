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

package floodsearch;

import graphcreation.services.Service;
import graphsearch.SearchID;
import util.timer.Timer;
import util.timer.TimerTask;

public class RunningSearch implements TimerTask {

	private final Service service;
	private final Service initService;
	private final Service goalService;
	private final SearchID searchID;
	
	private final long startTime;
	
	private final InitCompositionListener initCompositionListener;
	
	private final Timer msgTimer;
	
	private boolean disabled = false;
	
	public RunningSearch(final SearchID searchID, final Service service, final Service initService, final Service goalService, 
			final long startTime, InitCompositionListener initCompositionListener, final long msgInterval, final boolean notFirstTime) {
		this.searchID = searchID;
		this.service = service;
		this.initCompositionListener = initCompositionListener;
		this.startTime = startTime;
		this.msgTimer = new Timer(msgInterval, this, notFirstTime);
		
		this.initService = initService;
		this.goalService = goalService;
		
		msgTimer.start();
	}
	
	public Service getService() {
		return service;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public void stopTimer() {
		disabled = true;
		//msgTimer.stopAndWait();
	}
	
	public Service getInitService() {
		return initService;
	}
	
	public Service getGoalService() {
		return goalService;
	}
	
	public SearchID getSearchID() {
		return searchID;
	}

	@Override
	public void perform() throws InterruptedException {
		if (!disabled)
			initCompositionListener.initFComposition(getInitService(), getSearchID());
	}
}
