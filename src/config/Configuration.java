package config;

import java.io.FileInputStream;
import java.util.Properties;

public class Configuration {

	private final static String LOCAL_CONFIG_FILE = "./Configuration.xml";
	private final static String GENERAL_CONFIG_FILE = "../Configuration.xml";
	
	private static String otherFile = null;

	private final Properties props = new Properties();

	private final static Configuration config = new Configuration();

	private boolean configLoaded = false;

	protected Configuration() {
	}

	public static Configuration getInstance() {
		return config;
	}
	
	public static void setFile(String filePath) {
		otherFile = filePath;
	}

	public String getProperty(final String key) throws Exception {
		if (!configLoaded)
			if (otherFile != null)
				props.loadFromXML(new FileInputStream(otherFile));
			
			try {
				// First attempt
				props.loadFromXML(new FileInputStream(LOCAL_CONFIG_FILE));
				configLoaded = true;
			} catch (final Exception e) {
				// Second attempt
				props.loadFromXML(new FileInputStream(GENERAL_CONFIG_FILE));
			}
		return props.getProperty(key);
	}
}
