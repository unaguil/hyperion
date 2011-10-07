package testing;

import static junit.framework.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class NS2Simulation {

	private static final int ABORT_TIME = 120 * 1000;

	public boolean runScript(final String workingDir, final String script, final String outputDir) {
		final String outputFilePath = outputDir + File.separatorChar + "output.log";
		final String errorFilePath = outputDir + File.separatorChar + "error.log";

		boolean finished = false;

		try {
			// Execute command
			final ProcessBuilder pb = new ProcessBuilder(getNSExecutablePath(), script);
			final Map<String, String> env = pb.environment();

			if (checkEnvironment(env)) {

				final String NS2_PATH = env.get("NS_DIR");
				final String AGENTJ_PATH = env.get("AGENTJ");
				final String JAVA_HOME = env.get("JAVA_HOME");

				final String NS2_BIN_PATH = NS2_PATH + File.separatorChar + "bin";
				final String NS2_TCL_PATH = NS2_PATH + File.separatorChar + "tcl8.4.18" + File.separatorChar + "unix";
				final String NS2_TCL_LIB_PATH = NS2_PATH + File.separatorChar + "tcl8.4.18" + File.separatorChar + "library";
				final String NS2_TK_PATH = NS2_PATH + File.separatorChar + "tk8.4.18" + File.separatorChar + "unix";
				final String NS2_OTCL_PATH = NS2_PATH + File.separatorChar + "otcl-1.13";
				final String NS2_LIB_PATH = NS2_PATH + File.separatorChar + "lib";

				final String AGENTJ_CORE_LIB = AGENTJ_PATH + File.separator + "core" + File.separator + "lib";
				final String JAVA_AMD64_LIB = JAVA_HOME + File.separator + "jre" + File.separator + "lib" + File.separator + "amd64" + File.separator + "server";

				final String CLASSPATH = AGENTJ_PATH + File.separator + "core" + File.separator + "target" + File.separator + "agentj-core-1.0.jar" + File.pathSeparator + AGENTJ_CORE_LIB + File.separator + "proto-logging-0.1.jar";

				// Set to the current Eclipse classpath
				final String AGENTJ_CLASSPATH = System.getProperty("java.class.path");

				env.put("PATH", env.get("PATH") + File.pathSeparator + NS2_BIN_PATH + File.pathSeparator + NS2_TCL_PATH + File.pathSeparator + NS2_TK_PATH);
				env.put("LD_LIBRARY_PATH", env.get("LD_LIBRARY_PATH") + File.pathSeparator + NS2_OTCL_PATH + File.pathSeparator + NS2_LIB_PATH);
				env.put("TCL_LIBRARY", env.get("TCL_LIBRARY") + File.pathSeparator + NS2_TCL_LIB_PATH);

				env.put("AGENTJ", AGENTJ_PATH);
				env.put("JAVA_HOME", JAVA_HOME);
				env.put("LD_LIBRARY_PATH", env.get("LD_LIBRARY_PATH") + File.pathSeparator + AGENTJ_CORE_LIB + File.pathSeparator + JAVA_AMD64_LIB);
				env.put("AGENTJ_CLASSPATH", AGENTJ_CLASSPATH);
				env.put("CLASSPATH", env.get("CLASSPATH") + File.pathSeparator + CLASSPATH);

				pb.directory(new File(workingDir));
				final Process p = pb.start();

				final ReadProcessOutput readOutput = new ReadProcessOutput(p.getInputStream(), outputFilePath);
				readOutput.start();

				final ReadProcessOutput readErrorOutput = new ReadProcessOutput(p.getErrorStream(), errorFilePath);
				readErrorOutput.start();

				final Timer timer = new Timer(true);
				try {
					final InterruptTimerTask interrupter = new InterruptTimerTask(Thread.currentThread());
					timer.schedule(interrupter, ABORT_TIME);
					finished = (p.waitFor() == 0);
				} catch (final InterruptedException e) {
					// do something to handle the timeout here
				} finally {
					timer.cancel();
					Thread.interrupted();
				}

				p.destroy();

				readOutput.finish();
				readErrorOutput.finish();
			} else
				fail("Environment variables were not correctly set. Needed variables: NS_DIR, AGENTJ and JAVA_HOME");
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return finished;
	}

	private boolean checkEnvironment(final Map<String, String> env) {
		final String NS2_PATH = env.get("NS_DIR");
		final String AGENTJ_PATH = env.get("AGENTJ");
		final String JAVA_HOME = env.get("JAVA_HOME");

		if (NS2_PATH == null) {
			System.out.println("NS_DIR environment variable not set");
			return false;
		} else if (AGENTJ_PATH == null) {
			System.out.println("AGENTJ environment variable not set");
			return true;
		} else if (JAVA_HOME == null) {
			System.out.println("JAVA_HOME environment variable not set");
			return true;
		}
		return true;
	}

	private String getNSExecutablePath() {
		final ProcessBuilder pb = new ProcessBuilder("");
		final Map<String, String> env = pb.environment();

		final String NS2_PATH = env.get("NS_DIR");

		final String NS2_BIN_PATH = NS2_PATH + File.separatorChar + "bin";
		final String NS2_EXECUTABLE_PATH = NS2_BIN_PATH + File.separatorChar + "ns";

		return NS2_EXECUTABLE_PATH;
	}

	private static class InterruptTimerTask extends TimerTask {

		private final Thread thread;

		public InterruptTimerTask(final Thread t) {
			this.thread = t;
		}

		@Override
		public void run() {
			thread.interrupt();
		}

	}

	private static class ReadProcessOutput extends Thread {

		private final String newLineSeparator = System.getProperty("line.separator");

		private final InputStream is;
		private final String filePath;

		private boolean finished = false;

		public ReadProcessOutput(final InputStream is, final String filePath) {
			this.is = is;
			this.filePath = filePath;
		}

		public synchronized void finish() {
			finished = true;
		}

		private synchronized boolean isFinished() {
			return finished;
		}

		@Override
		public void run() {
			final BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			BufferedWriter writer = null;

			try {
				writer = new BufferedWriter(new FileWriter(filePath));
				while (!isFinished() && (line = br.readLine()) != null)
					writer.write(line + newLineSeparator);
			} catch (final IOException e) {
				e.printStackTrace();
			} finally {

				try {
					if (writer != null)
						writer.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			try {
				br.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
}
