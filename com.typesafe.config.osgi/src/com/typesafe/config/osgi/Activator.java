package com.typesafe.config.osgi;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static final String BASE_PATH = "conf";

	private ServiceTracker<ConfigurationAdmin, ConfigurationWatcher> configAdminTracker;

	@Override
	public void start(BundleContext context) throws Exception {

		final Path basePath = Paths.get(BASE_PATH);

		configAdminTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationWatcher>(
				context, ConfigurationAdmin.class, null) {

			@Override
			public ConfigurationWatcher addingService(
					ServiceReference<ConfigurationAdmin> reference) {
				ConfigurationAdmin configAdmin = context.getService(reference);
				ConfigurationHandler handler = new ConfigurationHandler(
						basePath, configAdmin);
				try {
					return new ConfigurationWatcher(basePath, handler);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			public void removedService(
					ServiceReference<ConfigurationAdmin> reference,
					ConfigurationWatcher watcher) {
				context.ungetService(reference);
				watcher.close();
			}
		};
		configAdminTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		configAdminTracker.close();
	}
}
