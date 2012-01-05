package peer;


class DelayAdjuster {

	private long receivedMessages = 0;
	
	private long startTime;
	
	private static final double f = 0.04;
	
	private static int MIN = 10;
	private static int MAX = 50;
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public synchronized void addReceivedMessage() {
		receivedMessages++;
	}
	
	public synchronized int getCurrentMaxDelay() {
		final long elapsedTime = System.currentTimeMillis() - startTime;
		final double messagesPerMilli = elapsedTime > 0?receivedMessages / (float) elapsedTime:0.0f;
		final double normValue = messagesPerMilli >= f?1.0f: messagesPerMilli / f;
		
		return (int) ((MAX - MIN) * normValue + MIN);
	}
}
