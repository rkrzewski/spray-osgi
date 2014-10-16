package com.typesafe.config.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * An utility class that allows recovering {@code Config} objects with origin
 * information.
 *
 * <p>
 * When passing Configuration properties loaded from files to OSGi
 * {@code ConfigurationAdmin}, the management agent encodes origin information
 * for all configuration values (file name and line number) into additional
 * properties, named {@code <complete entry path>.origin}.
 * {@link #fromProperties(Dictionary)} method recovers original {@code Config}
 * object with as much fidelity as possible.
 * </p>
 *
 * @author Rafał Krzewski
 */
public class ConfigRecovery {

	/**
	 * Recover original {@code Config} object loaded by Typesafe Config
	 * management agent, passed by {@code ConfigurationAdmin} to a
	 * {@code ManagedSevice}.
	 * 
	 * @param properties
	 *            Configuration properties passed through OSGi
	 *            {@code ConfigurationAdmin}.
	 * @return recovered {@code Config} object with value origin information.
	 */
	public static Config fromProperties(Dictionary<String, ?> properties) {
		Enumeration<String> keys = properties.keys();
		Config config = ConfigFactory.empty();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.endsWith(".origin")) {
				String originDesc = (String) properties.get(key);
				String valueKey = key.replaceAll("\\.origin$", "");
				config = config.withValue(valueKey, ConfigValueFactory
						.fromAnyRef(properties.get(valueKey), originDesc));
			}
		}
		return config;
	}

	/**
	 * Recover original {@code Config} object loaded by Typesafe Config
	 * management agent, passed by Declarative Services runtime to a component.
	 * 
	 * @param properties
	 *            Configuration properties passed through OSGi
	 *            {@code ConfigurationAdmin}.
	 * @return recovered {@code Config} object with value origin information.
	 */
	public static Config fromProperties(Map<String, ?> properties) {
		Config config = ConfigFactory.empty();
		for (Map.Entry<String, ?> entry : properties.entrySet()) {
			String key = entry.getKey();
			if (key.endsWith(".origin")) {
				String originDesc = (String) entry.getValue();
				String valueKey = key.replaceAll("\\.origin$", "");
				config = config.withValue(valueKey, ConfigValueFactory
						.fromAnyRef(properties.get(valueKey), originDesc));
			}
		}
		return config;
	}

}
