package pl.caltha.osgi.log.console;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.function.Consumer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * A DS component that prints OSGi LogService entries to system console.
 * 
 * <p>
 * Logging verbosity can be controlled using {@value #LEVEL_PROPERTY} framework
 * property. See {@link LogService} {@code LOG_...} constants for values.
 * </p>
 * 
 * <p>
 * The component uses an internal bounded buffer that is used during startup, to
 * process messages accumulated by LogService, and for bursts when messages
 * arrive faster than they can be printed to the console. Capacity of the buffer
 * can be controlled with {@value #BUFFER_PRPERTY} framework property.
 * </p>
 */
@Component
public class ConsoleLogAgent {

	private static final String LEVEL_PROPERTY = "console.log.level";

	private static final int LEVEL_DEFAULT = LogService.LOG_INFO;

	private static final String BUFFER_PROPERTY = "console.log.buffer";

	private static final int BUFFER_DEFAULT = 100;

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

	private LogReaderService logReader;

	private LogBuffer buffer;

	private LogListener listener;

	@Reference
	public void setLogReaderService(LogReaderService logReader) {
		this.logReader = logReader;
	}

	@Activate
	public void activate(BundleContext context) {
		int maxLevel = prop(context, LEVEL_PROPERTY, LEVEL_DEFAULT);
		int bufferSize = prop(context, BUFFER_PROPERTY, BUFFER_DEFAULT);

		Consumer<LogEntry> consumer = (entry) -> System.out
				.println(format(entry));
		buffer = new LogBuffer(bufferSize, maxLevel, consumer);

		listener = buffer.logListener();
		logReader.addLogListener(listener);

		@SuppressWarnings("unchecked")
		Enumeration<LogEntry> log = logReader.getLog();
		buffer.start(log);
	}

	@Deactivate
	public void deactivate() {
		logReader.removeLogListener(listener);
		buffer.stop();
	}

	private static int prop(BundleContext context, String name, int defValue) {
		String property = context.getProperty(name);
		return property != null ? Integer.parseInt(property) : defValue;
	}

	/**
	 * Formats OSGi LogService entries as text.
	 * 
	 * *
	 * <p>
	 * Format of the message is as follows:
	 * 
	 * <pre>
	 * `ISO-8601 timestamp` `bundle symbolic name` [`bundle id`]: `message`
	 * </pre>
	 * 
	 * If {code LogEntry} contains a {@code ServiceReference} the format will be
	 * as follows:
	 * 
	 * <pre>
	 * `ISO-8601 timestamp` `bundle symbolic name` [`bundle id`] service `service class`: `message`
	 * </pre>
	 * 
	 * If {@code LogEntry} contains a {@code Throwable} the stack trace will be
	 * appended in the following lines.
	 * </p>
	 * 
	 * @param entry
	 * @return
	 */
	private static String format(LogEntry entry) {
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
					.append(Long.toString(bundle.getBundleId())).append("]");
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

		return sw.toString();
	}
}
