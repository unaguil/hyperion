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

package graphsearch.compositionData.localSearchesTable;

import graphcreation.services.Service;

public class RelatedData {

	private final Service init;
	private final Service goal;
	private final int maxTTL;
	private final long searchTime;
	private final boolean wasPrepared;

	private final long timestamp;

	public RelatedData(final Service init, final Service goal, final int maxTTL, final long searchTime, final boolean wasPrepared) {
		this.init = init;
		this.goal = goal;
		this.maxTTL = maxTTL;
		this.searchTime = searchTime;
		this.timestamp = System.currentTimeMillis();
		this.wasPrepared = wasPrepared;
	}

	public Service getInit() {
		return init;
	}

	public Service getGoal() {
		return goal;
	}

	public int getMaxTTL() {
		return maxTTL;
	}
	
	public boolean wasPrepared() {
		return wasPrepared;
	}

	public long getRemainingTime() {
		final long elapsedTime = System.currentTimeMillis() - this.timestamp;
		final long remainingTime = searchTime - elapsedTime;

		if (remainingTime < 0)
			return 0;

		return remainingTime;
	}
	
	public long getStartingTime() {
		return timestamp;
	}
}