package pl.caltha.osgi.log.console;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * A DS component that registers {@link ConsoleLogListener} with
 * {@code LogReaderService}.
 * 
 * <p>
 * Logging verbosity can be controlled using {@value #LEVEL_PROPERTY} framework
 * property. See {@link LogService} {@code LOG_...} constants for values.
 * </p>
 */
@Component
public class ConsoleLogAgent {

	private static final String LEVEL_PROPERTY = "console.log.level";

	private static final int LEVEL_DEFAULT = LogService.LOG_INFO;

	private LogReaderService logReader;

	private LogListener listener;

	@Reference
	public void setLogReaderService(LogReaderService logReader) {
		this.logReader = logReader;
	}

	@Activate
	public void activate(BundleContext context) {
		String levelProp = context.getProperty(LEVEL_PROPERTY);
		int level = levelProp != null ? Integer.parseInt(levelProp)
				: LEVEL_DEFAULT;
		listener = new ConsoleLogListener(level);
		logReader.addLogListener(listener);
	}

	@Deactivate
	public void deactivate() {
		logReader.removeLogListener(listener);
	}
}
