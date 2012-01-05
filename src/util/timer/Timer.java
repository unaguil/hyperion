package util.timer;

import util.WaitableThread;

public class Timer extends WaitableThread {

	private final long period;
	private final TimerTask timerTask;

	public Timer(final long period, final TimerTask timerTask) {
		this.period = period;
		this.timerTask = timerTask;
	}

	@Override
	public void run() {
		while (!Thread.interrupted())
			try {
				// perform timer task
				timerTask.perform();
	
				if (period > 0)
					WaitableThread.mySleep(period);
				else
					WaitableThread.mySleep(1);
				
			} catch (final InterruptedException e) {
				interrupt();
			}

		threadFinished();
	}
}
