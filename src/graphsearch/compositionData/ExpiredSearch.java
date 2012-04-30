package graphsearch.compositionData;

import graphcreation.services.Service;
import graphsearch.SearchID;

public class ExpiredSearch {

	private final SearchID searchID;
	private final Service initService, goalService;
	private final boolean wasPrepared;
	
	public ExpiredSearch(final SearchID searchID, final Service initService, final Service goalService, final boolean wasPrepared) {
		this.searchID = searchID;
		this.initService = initService;
		this.goalService = goalService;
		this.wasPrepared = wasPrepared;
	}

	public SearchID getSearchID() {
		return searchID;
	}

	public Service getInitService() {
		return initService;
	}

	public Service getGoalService() {
		return goalService;
	}
	
	public boolean wasPrepared() {
		return wasPrepared;
	}
}
