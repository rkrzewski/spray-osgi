package pl.caltha.osgi.log.console;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;

/**
 * {@code LogListener} that dumps incoming events to {@code System.out}.
 * 
 * <p>
 * Format of the message is as follows:
 * 
 * <pre>
 * `ISO-8601 timestamp` `bundle symbolic name` [`bundle id`]: `message`
 * </pre>
 * 
 * If {@code LogEntry} contains a {@code Throwable} the stack trace will be
 * appended in the following lines.
 * </p>
 */
public class ConsoleLogListener implements LogListener {

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

	private int verbosity;

	/**
	 * Creates a ConsoleLogListener instance.
	 * 
	 * @param maxLevel
	 *            maximum level of messages that will be printed to the console.
	 *            See {@link LogService} {@code LOG_...} constants for values.
	 * @param log
	 *            LogEntries past log entries, in reverse chronological order
	 */
	public ConsoleLogListener(int maxLevel, Enumeration<LogEntry> log) {
		this.verbosity = maxLevel;
		List<LogEntry> entries = new ArrayList<>();
		while (log.hasMoreElements()) {
			entries.add(log.nextElement());
		}
		for (int i = entries.size() - 1; i >= 0; i--) {
			logged(entries.get(i));
		}
	}

	@Override
	public void logged(LogEntry entry) {
		if (entry.getLevel() <= verbosity) {
			StringWriter sw = new StringWriter();

			switch (entry.getLevel()) {
			case LogService.LOG_DEBUG:
				sw.append("DEBUG");
				break;
			case LogService.LOG_INFO:
				sw.append("INFO");
				break;
			case LogService.LOG_WARNING:
				sw.append("WARNING");
				break;
			case LogService.LOG_ERROR:
				sw.append("ERROR");
				break;
			}
			sw.append(" ");

			TIME_FORMAT.formatTo(Instant.ofEpochMilli(entry.getTime()), sw);
			sw.append(" ");

			Bundle bundle = entry.getBundle();
			if (bundle != null) {
				sw.append(bundle.getSymbolicName()).append(" [")
						.append(Long.toString(bundle.getBundleId()))
						.append("]");
			} else {
				sw.append("<unkown bundle>");
			}
			ServiceReference ref = entry.getServiceReference();
			if (ref != null) {
				String[] objectClass = (String[]) ref
						.getProperty(Constants.OBJECTCLASS);
				sw.append(" service ");
				for (int i = 0; i < objectClass.length; i++) {
					sw.append(objectClass[i]);
					if (i < objectClass.length - 1) {
						sw.append(", ");
					}
				}
			}

			sw.append(": ").append(entry.getMessage());

			Throwable exception = entry.getException();
			if (exception != null) {
				PrintWriter pw = new PrintWriter(sw);
				pw.println();
				exception.printStackTrace(pw);
				pw.flush();
			}

			System.out.println(sw.toString());
		}
	}
}