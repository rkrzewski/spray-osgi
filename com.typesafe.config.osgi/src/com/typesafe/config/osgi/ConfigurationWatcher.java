package com.typesafe.config.osgi;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class ConfigurationWatcher {

	private WatchService watchService;

	private Thread worker;

	private ConfigurationHandler handler;

	private Path basePath;

	public ConfigurationWatcher(Path basePath, ConfigurationHandler handler)
			throws IOException {
		this.basePath = basePath;
		this.handler = handler;
		FileSystem fileSystem = FileSystems.getDefault();
		watchService = fileSystem.newWatchService();
		WatchKey key = basePath.register(watchService,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		worker = new Worker(Files.newDirectoryStream(basePath, "*.conf"), key);
		worker.start();
	}

	public void close() {
		worker.interrupt();
	}

	private class Worker extends Thread {

		private DirectoryStream<Path> initialFiles;

		private WatchKey initialKey;

		public Worker(DirectoryStream<Path> initalFiles, WatchKey initialKey) {
			this.initialFiles = initalFiles;
			this.initialKey = initialKey;
		}

		@Override
		public void run() {
			for (Path path : initialFiles) {
				handler.handleEvent(mockEvent(basePath.relativize(path)));
			}
			try {
				WatchKey key = initialKey;
				while (!Thread.interrupted()) {
					for (WatchEvent<?> event : key.pollEvents()) {
						if (event.context() instanceof Path) {
							@SuppressWarnings("unchecked")
							WatchEvent<Path> standardEvent = (WatchEvent<Path>) event;
							if (standardEvent.context().toString()
									.endsWith(".conf")) {
								handler.handleEvent(standardEvent);
							}
						}
					}
					key.reset();
					key = watchService.take();
				}
			} catch (InterruptedException e) {
				return;
			}
		}

		private WatchEvent<Path> mockEvent(final Path path) {
			return new WatchEvent<Path>() {
				@Override
				public WatchEvent.Kind<Path> kind() {
					return StandardWatchEventKinds.ENTRY_MODIFY;
				}

				@Override
				public int count() {
					return 1;
				}

				@Override
				public Path context() {
					return path;
				}
			};
		}
	}

}
