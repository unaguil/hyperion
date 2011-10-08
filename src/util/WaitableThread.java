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
}
