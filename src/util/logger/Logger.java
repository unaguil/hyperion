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
		final double time = (System.currentTimeMillis() - delta) / 1000.0;
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
}
