package graphsearch.compositionData.localSearchesTable;

import graphcreation.services.Service;

public class RelatedData {

	private final Service init;
	private final Service goal;
	private final int maxTTL;
	private final long searchTime;

	private final long timestamp;

	public RelatedData(final Service init, final Service goal, final int maxTTL, final long searchTime) {
		this.init = init;
		this.goal = goal;
		this.maxTTL = maxTTL;
		this.searchTime = searchTime;
		this.timestamp = System.currentTimeMillis();
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

	public long getRemainingTime() {
		final long elapsedTime = System.currentTimeMillis() - this.timestamp;
		final long remainingTime = searchTime - elapsedTime;

		if (remainingTime < 0)
			return 0;

		return remainingTime;
	}
}