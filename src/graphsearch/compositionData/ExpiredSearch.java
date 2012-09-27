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
