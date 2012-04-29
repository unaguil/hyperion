package util.timer;

import util.WaitableThread;

public class Timer extends WaitableThread {

	private final long period;
	private final TimerTask timerTask;
	private final boolean notFirst;

	public Timer(final long period, final TimerTask timerTask) {
		this.period = period;
		this.timerTask = timerTask;
		this.notFirst = false;
	}
	
	public Timer(final long period, final TimerTask timerTask, final boolean notFirst) {
		this.period = period;
		this.timerTask = timerTask;
		this.notFirst = notFirst;
	}

	@Override
	public void run() {
		boolean firstTime = true;
		while (!Thread.interrupted())
			try {
				if (!notFirst || !firstTime) {
					// perform timer task
					timerTask.perform();
				}
	
				if (period > 0)
					Thread.sleep(period);
				else
					Thread.sleep(1);
			
				firstTime = false;
			} catch (final InterruptedException e) {
				interrupt();
			}

		threadFinished();
	}
}
