package testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class MultipleTests {
	private static final String TEST_TAG = "test";
	private static final String WORKING_DIR_ATTRIB = "workingDir";
	private static final String SCRIPT_ATTRIB = "script";

	private final String xmlPath;

	private final List<BasicTest> testList = new ArrayList<BasicTest>();

	public static final int MAX_TRIES = 5;

	private enum ErrorCause {
		UNKNOWN, COLLISION
	}

	private final FileFilter fileFilter;

	private int repeat = 1;

	public MultipleTests(final String xmlPath, final FileFilter fileFilter) {
		this.xmlPath = xmlPath;
		this.fileFilter = fileFilter;

		loadTestData(xmlPath);
	}

	public MultipleTests(final String xmlPath) {
		this.xmlPath = xmlPath;
		this.fileFilter = null;

		loadTestData(xmlPath);
	}

	public void setRepeat(final int repeat) {
		this.repeat = repeat;
	}

	private void loadTestData(final String testXMLData) {
		try {
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(testXMLData));

			final NodeList tests = doc.getElementsByTagName(TEST_TAG);
			for (int i = 0; i < tests.getLength(); i++) {
				final Element e = (Element) tests.item(i);
				final String workingDir = e.getAttribute(WORKING_DIR_ATTRIB);
				final String script = e.getAttribute(SCRIPT_ATTRIB);
				testList.add(new BasicTest(workingDir, script));
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void launchAll() throws Exception {
		final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final long startTime = System.currentTimeMillis();
		for (int i = 0; i < repeat; i++) {
			final int iteration = i + 1;
			System.out.println("Starting iteration " + iteration);

			final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
			for (final BasicTest test : testList) {
				final Future<Boolean> future = executor.submit(new ParallelTest(test));
				futures.add(future);
			}

			// Wait for futures
			for (final Future<Boolean> future : futures)
				future.get();

			System.out.println("Finished iteration " + iteration);
		}

		System.out.println("All test from " + xmlPath + " executed succesfully in " + (System.currentTimeMillis() - startTime) + " ms");
	}

	private class ParallelTest implements Callable<Boolean> {

		private final BasicTest test;

		public ParallelTest(final BasicTest test) {
			this.test = test;
		}

		private boolean performTry(final ArrayList<ErrorCause> detectedErrorCauses) throws Exception {
			if (test.runScript()) {
				if (fileFilter != null)
					try {
						checkOutputFiles(test);
					} catch (final Exception e) {
						System.out.println(e.getMessage());
						throw e;
					}
				else
					try {
						check(test);
					} catch (final Exception e) {
						System.out.println(e.getMessage());
						throw e;
					}

				test.finish();

				return true;
			}

			detectedErrorCauses.add(ErrorCause.UNKNOWN);
			System.out.println("NS-2 simulation failed due to unknown reasons " + test.getWorkingDir());

			return false;
		}

		@Override
		public Boolean call() throws Exception {
			System.out.println("Running script " + test.getScript() + " in directory: " + test.getWorkingDir());

			final ArrayList<ErrorCause> detectedErrorCauses = new ArrayList<ErrorCause>();

			int tryNumber = 1;
			boolean finished = false;
			while (tryNumber <= MAX_TRIES && !finished) {
				finished = performTry(detectedErrorCauses);
				tryNumber++;
			}

			if (!finished) {
				final String error = "Script " + test.getScript() + " in directory: " + test.getWorkingDir() + " failed. Max tries: " + MAX_TRIES + " reached. Error causes: " + detectedErrorCauses;
				System.out.println(error);
				fail(error);
			} else
				System.out.println("Script " + test.getScript() + " in directory: " + test.getWorkingDir() + " successfully finished");

			return Boolean.TRUE;
		}
	}

	private int getExpectedFilesNumber(final BasicTest test) throws Exception {
		final File expectedFolder = new File(test.getExpectedDir());
		final File[] expectedFiles = expectedFolder.listFiles(fileFilter);
		return expectedFiles.length;
	}

	private void checkOutputFiles(final BasicTest test) throws Exception {
		final File outputFolder = new File(test.getOutputDir());

		final File[] files = outputFolder.listFiles(fileFilter);
		// Check output files number
		assertEquals("Output files number does not match " + test.getWorkingDir(), getExpectedFilesNumber(test), files.length);
		for (final File file : files) {
			final String fileName = file.getName();

			Object output = null;
			try {
				output = readObject(new FileInputStream(file));
			} catch (final Exception e) {
				System.out.println(e.getMessage());
				throw new Exception(e.getMessage() + " File: " + fileName);
			}

			final String expectedFileName = test.getExpectedDir() + File.separatorChar + fileName;
			final File expectedFile = new File(expectedFileName);
			Object expected = null;
			if (expectedFile.exists())
				try {
					expected = readObject(new FileInputStream(expectedFile));
					assertEquals("Script: " + test.getScript() + " on working dir: " + test.getWorkingDir() + " file " + fileName, expected, output);
				} catch (final Exception e) {
					throw new Exception(e.getMessage() + " File: " + expectedFileName);
				}
		}
	}

	public abstract Object readObject(FileInputStream fileInputStream) throws Exception;

	public abstract void check(BasicTest test) throws Exception;
}
