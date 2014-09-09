package com.typesafe.config.osgi;

import java.io.IOException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	ServiceTracker<ConfigurationAdmin, ConfigurationWatcher> configAdminTracker;

	@Override
	public void start(BundleContext context) throws Exception {
		configAdminTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationWatcher>(
				context, ConfigurationAdmin.class, null) {
			
			@Override
			public ConfigurationWatcher addingService(
					ServiceReference<ConfigurationAdmin> reference) {
				ConfigurationAdmin configAdmin = context.getService(reference);
				ConfigurationHandler handler = new ConfigurationHandler(
						configAdmin);
				try {
					return new ConfigurationWatcher("conf", handler);
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
