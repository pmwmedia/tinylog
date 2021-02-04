package org.tinylog.impl.format;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;

import org.tinylog.impl.LogEntry;
import org.tinylog.impl.LogEntryValue;

/**
 * Placeholder implementation for resolving the date and time of issue for a log entry.
 */
public class DatePlaceholder implements Placeholder {

	private final DateTimeFormatter formatter;
	private final boolean formatForSql;

	/**
	 * @param formatter The formatter to use for formatting the date and time of issue
	 * @param formatForSql The date and time of issue will be applied as formatted string to prepared SQL statements if
	 *                     set to {@code true}, otherwise it will be applied as {@link Timestamp SQL timestamp}
	 */
	public DatePlaceholder(DateTimeFormatter formatter, boolean formatForSql) {
		this.formatter = formatter;
		this.formatForSql = formatForSql;
	}

	@Override
	public Set<LogEntryValue> getRequiredLogEntryValues() {
		return EnumSet.of(LogEntryValue.TIMESTAMP);
	}

	@Override
	public void render(StringBuilder builder, LogEntry entry) {
		Instant instant = entry.getTimestamp();
		if (instant == null) {
			builder.append("<timestamp unknown>");
		} else {
			formatter.formatTo(instant, builder);
		}
	}

	@Override
	public void apply(PreparedStatement statement, int index, LogEntry entry) throws SQLException {
		Instant instant = entry.getTimestamp();

		if (formatForSql) {
			statement.setString(index, instant == null ? null : formatter.format(instant));
		} else {
			statement.setTimestamp(index, instant == null ? null : Timestamp.from(instant));
		}
	}

}