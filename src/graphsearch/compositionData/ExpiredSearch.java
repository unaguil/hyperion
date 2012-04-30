package graphsearch.compositionData;

import graphcreation.services.Service;
import graphsearch.SearchID;

public class ExpiredSearch {

	private final SearchID searchID;
	private final Service initService, goalService;
	
	public ExpiredSearch(final SearchID searchID, final Service initService, final Service goalService) {
		this.searchID = searchID;
		this.initService = initService;
		this.goalService = goalService;
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
}
