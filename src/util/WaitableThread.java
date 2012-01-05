package util;

public abstract class WaitableThread extends Thread {

	private final Object o = new Object();
	private boolean finished = false;

	protected void threadFinished() {
		synchronized (o) {
			finished = true;
			o.notifyAll();
		}
	}

	private void waitThread() {
		synchronized (o) {
			while (!finished)
				try {
					o.wait();
				} catch (final InterruptedException e) {
					// do nothing
				}
		}
	}

	public void stopAndWait() {
		interrupt();
		waitThread();
	}
	
	private static final long MINIMUM_SLEEP = 3;
	
	//TODO Maybe there is a bug in AgentJ. Thread.sleep() is not interrupted. Using the following workaround. 
	public static void mySleep(long millis) throws InterruptedException {
		final long startTime = System.currentTimeMillis();
		
		do {
			try {
				Thread.sleep(MINIMUM_SLEEP);
			} catch (InterruptedException e) { 
				//Do nothing 
			}
		} while (!Thread.interrupted() && (System.currentTimeMillis() - startTime) < millis);
		
		if ((System.currentTimeMillis() - startTime) < millis)
			throw new InterruptedException();
	}
}
