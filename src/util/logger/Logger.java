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

package util.logger;

import java.text.DecimalFormat;

public class Logger {

	private final org.apache.log4j.Logger logger;

	public static final boolean TRACE = false;

	private static long delta = 0;

	private Logger(final org.apache.log4j.Logger logger) {
		this.logger = logger;
	}

	private String getTime() {
		final double time = getCurrentTimeSeconds();
		final DecimalFormat df = new DecimalFormat("00.000");
		final String str = df.format(time);
		return str.replace('.', ',');
	}

	@SuppressWarnings("rawtypes")
	public static Logger getLogger(final Class clazz) {
		return new Logger(org.apache.log4j.Logger.getLogger(clazz));
	}

	public static void setDeltaTime(final long deltaTime) {
		delta = deltaTime;
	}
	
	public static long getDeltaTime() {
		return delta;
	}

	public void debug(final Object message) {
		logger.debug(message + " " + getTime());
	}

	public void trace(final Object message) {
		logger.trace(message + " " + getTime());
	}

	public void error(final Object message) {
		logger.error(message + " " + getTime());
	}

	public void info(final Object message) {
		logger.info(message + " " + getTime());
	}
	
	public double getCurrentTimeSeconds() {
		return (System.currentTimeMillis() - delta) / 1000.0;
	}
}
