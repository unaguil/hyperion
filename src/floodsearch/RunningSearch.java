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
			final long startTime, InitCompositionListener initCompositionListener, final long msgInterval) {
		this.searchID = searchID;
		this.service = service;
		this.initCompositionListener = initCompositionListener;
		this.startTime = startTime;
		this.msgTimer = new Timer(msgInterval, this, true);
		
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
