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

	public static void setFile(final String filePath) {
		otherFile = filePath;
	}

	public String getProperty(final String key) throws Exception {
		if (!configLoaded)
			if (otherFile != null)
				props.loadFromXML(new FileInputStream(otherFile));
			else
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
