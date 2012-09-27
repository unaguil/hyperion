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
