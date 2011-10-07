package config;

import java.io.FileInputStream;
import java.util.Properties;

public class Configuration {

	private final static String LOCAL_CONFIG_FILE = "./Configuration.xml";
	private final static String GENERAL_CONFIG_FILE = "../Configuration.xml";

	private final Properties props = new Properties();

	private final static Configuration config = new Configuration();

	private boolean configLoaded = false;

	protected Configuration() {
	}

	public static Configuration getInstance() {
		return config;
	}

	public String getProperty(final String key) throws Exception {
		if (!configLoaded)
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
