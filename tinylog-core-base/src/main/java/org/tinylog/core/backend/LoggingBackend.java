package org.tinylog.core.backend;

import org.tinylog.core.Level;
import org.tinylog.core.context.ContextStorage;
import org.tinylog.core.format.message.MessageFormatter;
import org.tinylog.core.runtime.StackTraceLocation;

/**
 * Provider for issuing log entries.
 *
 * <p>
 *     This interface must be implemented by all logging backends.
 * </p>
 */
public interface LoggingBackend {

	/**
	 * Retrieves the thread-based context value storage.
	 *
	 * @return The storage for thread-based context values
	 */
	ContextStorage getContextStorage();

	/**
	 * Retrieves the visibility of all severity levels for a category tag. Log entries whose severity levels are set to
	 * {@code false} do not need to be passed to this logging backend since they are never output.
	 *
	 * @param tag The category tag for which the visibility of severity levels is requested
	 * @return The visibilities of all severity levels
	 */
	LevelVisibility getLevelVisibility(String tag);

	/**
	 * Checks if a severity level is enabled for outputting log entries.
	 *
	 * @param location Stack trace location of caller (required)
	 * @param tag Category tag (optional)
	 * @param level The severity level to check (required)
	 * @return {@code true} if log entries of the passed severity level will be output, {@code false} if not
	 */
	boolean isEnabled(StackTraceLocation location, String tag, Level level);

	/**
	 * Issues a new log entry.
	 *
	 * @param location Stack trace location of caller (required)
	 * @param tag Category tag (optional)
	 * @param level Severity level (required)
	 * @param throwable Exception or any other kind of throwable (optional)
	 * @param message Text message or any kind of other printable object (optional)
	 * @param arguments Argument values for all placeholders in the text message (only required if the text message
	 *                  contains any placeholders)
	 * @param formatter Message formatter for replacing placeholder with the provided arguments (only required if the
	 *                  text message contains any placeholders)
	 */
	void log(StackTraceLocation location, String tag, Level level, Throwable throwable, Object message,
		Object[] arguments, MessageFormatter formatter);

}