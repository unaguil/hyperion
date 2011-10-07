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
		while (!Thread.interrupted()) {
			try {
				Thread.sleep(period);

				// perform timer task
				timerTask.perform();
			} catch (final InterruptedException e) {
				break;
			}
		}

		this.threadFinished();
	}
}
