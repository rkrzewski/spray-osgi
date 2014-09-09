package com.typesafe.config.osgi;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

/**
 * Part of this code was adapted from Apache Felix Fileinstall.
 * 
 * https://github
 * .com/apache/felix/blob/trunk/fileinstall/src/main/java/org/apache
 * /felix/fileinstall/internal/ConfigInstaller.java
 *
 */
public class ConfigurationHandler {

	private static final String CONFIG_KEY = "com.typesafe.config.source";
	private ConfigurationAdmin configAdmin;

	public ConfigurationHandler(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}

	public void handleEvent(WatchEvent<Path> event) {
		if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
			configModified(event.context(), parsePid(event.context()));
		} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
			configRemoved(event.context(), parsePid(event.context()));
		}
	}

	private void configModified(Path path, String[] pid) {
		try {
			Configuration config = getConfiguration(toConfigKey(path), pid);
			Dictionary<String, Object> oldProps = config.getProperties();
			if (oldProps != null) {
				oldProps.remove(CONFIG_KEY);
				oldProps.remove(Constants.SERVICE_PID);
				oldProps.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
			}
			Dictionary<String, Object> newProps = loadConfig(path);
			if (!newProps.equals(oldProps)) {
				config.update(newProps);
			}
		} catch (IOException e) {
			error("failed to locate configuration " + formatPid(pid), e);
		}
	}

	private void configRemoved(Path path, String[] pid) {
		try {
			Configuration config = findExistingConfiguration(toConfigKey(path));
			try {
				if (config != null) {
					config.delete();
				}
			} catch (IOException e) {
				error("failed to delete configuration for " + formatPid(pid), e);
			}
		} catch (IOException e) {
			error("failed to locate configuration " + formatPid(pid), e);
		}
	}

	private String toConfigKey(Path path) {
		return path.toAbsolutePath().toUri().toString();
	}

	private String[] parsePid(Path path) {
		String fileName = path.getFileName().toString();
		String pid = fileName.substring(0, fileName.lastIndexOf('.'));
		int n = pid.indexOf('-');
		if (n > 0) {
			String factoryPid = pid.substring(n + 1);
			pid = pid.substring(0, n);
			return new String[] { pid, factoryPid };
		} else {
			return new String[] { pid, null };
		}
	}

	private String formatPid(String[] pid) {
		StringBuilder buff = new StringBuilder();
		buff.append(pid[0]);
		if (pid[1] != null)
			;
		buff.append("-").append(pid[1]);
		return buff.toString();
	}

	private Configuration getConfiguration(String configKey, String[] pid)
			throws IOException {
		Configuration oldConfiguration = findExistingConfiguration(configKey);
		if (oldConfiguration != null) {
			return oldConfiguration;
		} else {
			Configuration newConfiguration;
			if (pid[1] != null) {
				// XXX should this actually be pid[0]?
				newConfiguration = configAdmin.createFactoryConfiguration(
						pid[0], null);
			} else {
				newConfiguration = configAdmin.getConfiguration(pid[0], null);
			}
			return newConfiguration;
		}
	}

	private Configuration findExistingConfiguration(String configKey)
			throws IOException {
		try {
			String filter = "(" + CONFIG_KEY + "="
					+ escapeFilterValue(configKey) + ")";
			Configuration[] configurations = configAdmin
					.listConfigurations(filter);
			if (configurations != null && configurations.length > 0) {
				return configurations[0];
			} else {
				return null;
			}
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException("implementation error", e);
		}
	}

	private String escapeFilterValue(String s) {
		return s.replaceAll("[(]", "\\\\(").replaceAll("[)]", "\\\\)")
				.replaceAll("[=]", "\\\\=").replaceAll("[\\*]", "\\\\*");
	}

	private Dictionary<String, Object> loadConfig(Path path) {
		Config config = ConfigFactory.load(ConfigFactory.parseFile(path
				.toFile()));
		Dictionary<String, Object> dict = new Hashtable<>();
		for (java.util.Map.Entry<java.lang.String, ConfigValue> entry : config
				.entrySet()) {
			String value = entry.getValue().render();
			if (entry.getValue().valueType() == ConfigValueType.STRING) {
				dict.put(entry.getKey(), value.substring(1, value.length() - 1));
			} else {
				dict.put(entry.getKey(), value);
			}
		}
		return dict;
	}

	private void error(String msg, Throwable t) {
		System.err.println(msg);
		t.printStackTrace(System.err);
	}
}
