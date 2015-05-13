package pl.caltha.osgi.log.console;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.function.Consumer;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;

/**
 * A buffer for log entries coupled with a delivery worker thread that
 * guarantees processing historical and currently arriving messages in
 * chronological order.
 */
public class LogBuffer {

	private int capacity;

	private int maxLevel;

	private final LinkedList<LogEntry> buffer = new LinkedList<>();

	private final Thread thread;

	/**
	 * 
	 * @param capacity
	 *            capacity of the buffer.
	 * @param maxLevel
	 *            maximum level of messages that will be printed to the console.
	 *            See {@link LogService} {@code LOG_...} constants for values.
	 * @param consumer
	 *            consumer of log entries that will be invoked asynchronously by
	 *            the worker thread
	 */
	public LogBuffer(int capacity, int maxLevel, Consumer<LogEntry> consumer) {
		this.capacity = capacity;
		this.maxLevel = maxLevel;
		this.thread = new Thread(() -> {
			while (!Thread.interrupted()) {
				synchronized (buffer) {
					if (buffer.isEmpty()) {
						try {
							buffer.wait();
						} catch (InterruptedException e) {
							break;
						}
					}
					while (!buffer.isEmpty()) {
						consumer.accept(buffer.removeLast());
					}
				}
			}
			while (!buffer.isEmpty()) {
				consumer.accept(buffer.removeLast());
			}
		});
	}

	/**
	 * Returns LogListner interface of the buffer.
	 */
	public LogListener logListener() {
		return (entry) -> {
			synchronized (buffer) {
				insert(entry);
				buffer.notify();
			}
		};
	}

	/**
	 * Adds all messages in the enumeration to the buffer and starts the worker
	 * thread.
	 * 
	 * @param log
	 *            an enumeration of log entries.
	 */
	public void start(Enumeration<LogEntry> log) {
		synchronized (buffer) {
			while (log.hasMoreElements()) {
				insert(log.nextElement());
			}
		}
		thread.start();
	}

	/**
	 * Flushes the buffer and stops the worker thread.
	 */
	public void stop() {
		thread.interrupt();
	}

	/**
	 * Inserts the log entry to the buffer at the appropriate position.
	 * 
	 * @param entry
	 *            a log entry
	 */
	private void insert(LogEntry entry) {
		if (entry.getLevel() > maxLevel) {
			return;
		}
		ListIterator<LogEntry> i = buffer.listIterator();
		int pos = -1;
		while (i.hasNext()) {
			LogEntry cur = i.next();
			if (equals(cur, entry)) {
				return;
			}
			if (cur.getTime() > entry.getTime()) {
				pos = i.nextIndex();
			}
		}
		if (pos == -1) {
			buffer.addLast(entry);
		} else {
			buffer.add(pos, entry);
		}
		while (buffer.size() > capacity) {
			buffer.removeLast();
		}
	}

	/**
	 * Rudimentary equivalence check for log entries used to avoid duplicates.
	 * 
	 * @param e1
	 *            first log entry
	 * @param e2
	 *            second log entry
	 * @return true if entries are considered equal
	 */
	private static boolean equals(LogEntry e1, LogEntry e2) {
		return e1.getTime() == e2.getTime()
				&& e1.getMessage().equals(e2.getMessage());
	}
}
