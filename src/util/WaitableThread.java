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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

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
	
	private static final long MINIMUM_SLEEP = 1;
	
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
