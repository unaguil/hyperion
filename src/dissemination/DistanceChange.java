package dissemination;

public class DistanceChange {

	private final int previousValue;
	private final int newValue;

	public DistanceChange(final int previousValue, final int newValue) {
		this.previousValue = previousValue;
		this.newValue = newValue;
	}

	public int getPreviousValue() {
		return previousValue;
	}

	public int getNewValue() {
		return newValue;
	}
	
	@Override
	public String toString() {
		return "Change:" + previousValue + "->" + newValue;
	}
}
