package com.infernalbeast.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/*
 * 7. thread name
 */
public class SimpleFormatter extends java.util.logging.SimpleFormatter {
	static String getLoggingProperty(final String name) {
		String value = LogManager.getLogManager().getProperty(name);
		return value;
	}

	private final String format = getLoggingProperty(SimpleFormatter.class.getName() + ".format");

	@Override
	public String format(final LogRecord record) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
		String source;
		if (record.getSourceClassName() != null) {
			source = record.getSourceClassName();
			if (record.getSourceMethodName() != null) {
				source += " " + record.getSourceMethodName();
			}
		} else {
			source = record.getLoggerName();
		}
		String message = formatMessage(record);
		String throwable = "";
		if (record.getThrown() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			pw.println();
			record.getThrown().printStackTrace(pw);
			pw.close();
			throwable = sw.toString();
		}
		Thread currentThread = Thread.currentThread();
		return String.format(format, zdt, source, record.getLoggerName(), record.getLevel().getLocalizedName(), message,
				throwable, currentThread.getName());
	}
}
