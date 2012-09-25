package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.TreeLogger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jetty.util.log.Logger;

/**
 * A Jetty logger implementation that forwards logging requests to GWT's tree Logger.
 */
public class JettyTreeLogger implements Logger {

    /**
     * A regular expression matching ISO control characters.
     */
    private static final Pattern CONTROL_CHAR_REGEX = Pattern.compile("[\\x00-\\x1f\\x7f\\x9f]");

    /**
     * The line separator for the local operating system.
     */
    private static final String EOL = System.getProperty("line.separator");

    /**
     * The logger to forward logging requests to.
     */
    private final TreeLogger logger;

    /**
     * @param logger the logger to forward logging requests to.
     */
    public JettyTreeLogger(TreeLogger logger) {
        if (logger == null) {
            throw new NullPointerException("the embedded tree logger may not be null");
        }
        this.logger = logger;
    }

    /**
     * Builds the format string to use for a log message.
     *
     * @param msg the original message.
     * @param argCount the number of arguments passed to the message.
     * @return the format string to use.
     */
    private String buildFormatString(String msg, int argCount) {
        String result = msg;
        if (msg == null) {
            StringBuilder buffer = new StringBuilder(3 * argCount);
            for (int i = 0; i < argCount; i++) {
                buffer.append("{} ");
            }
            result = buffer.toString();
        }
        return result;
    }

    /**
     * Formats a log message.
     *
     * @param msg the message format string.
     * @param args the arguments to use when formatting the message.
     */
    private String format(String msg, Object... args) {
        String fmt = buildFormatString(msg, args.length);
        StringBuffer buffer = new StringBuffer(80);
        String[] components = fmt.split("\\{\\}", -1);
        for (int i = 0; i < components.length; i++) {
            appendEscaped(buffer, components[i]);
            if (i < args.length) {
                buffer.append(args[i]);
            }
        }
        return buffer.toString();
    }

    /**
     * Appends an escaped version of a string to a string buffer.
     *
     * @param buffer the buffer to append the escaped version of the string to.
     * @param str the string to escape and append.
     */
    private void appendEscaped(StringBuffer buffer, String str) {
        Matcher m = CONTROL_CHAR_REGEX.matcher(str);
        while (m.find()) {
            m.appendReplacement(buffer, escapeChar(m.group().charAt(0)));
        }
        m.appendTail(buffer);
    }

    /**
     * Escapes a single character in a log message. The incoming character is expected to be an ISO control character.
     *
     * @param c the character to escape.
     * @return a string to replace the character with.
     */
    private String escapeChar(char c) {
        if (c == '\n') {
            return "|";
        }
        else if (c == '\r') {
            return "<";
        }
        else {
            return "?";
        }
    }

    /**
     * Logs a message if the logging system is configured to log messages at the selected level.
     *
     * @param level the log level.
     * @param msg the format string used to build the log message.
     * @param args the arguments to substitute into the log message.
     */
    private void log(TreeLogger.Type level, String msg, Object... args) {
        if (logger.isLoggable(level)) {
            logger.log(level, format(msg, args));
        }
    }

    /**
     * Logs a message if the logging system is configured to log messages at the selected level.
     *
     * @param level the log level.
     * @param msg the message to log.
     * @param t the Throwable that caused the message to be logged.
     */
    private void log(TreeLogger.Type level, String msg, Throwable t) {
        if (logger.isLoggable(level)) {
            logger.log(level, msg, t);
        }
    }

    /**
     * @return the name of the logger.
     */
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Logs a warning message.
     *
     * @param msg the message format string.
     * @param args the arguments to pass when formatting the message.
     */
    public void warn(String msg, Object... args) {
        log(TreeLogger.Type.WARN, msg, args);
    }

    /**
     * Logs an exception or error as a warning.
     *
     * @param t the Throwable to log.
     */
    public void warn(Throwable t) {
        log(TreeLogger.Type.WARN, "", t);
    }

    /**
     * Logs an exception or error as a warning.
     *
     * @param msg the detail message.
     * @param t the Throwable to log.
     */
    public void warn(String msg, Throwable t) {
        log(TreeLogger.Type.WARN, msg, t);
    }

    /**
     * Logs an informational message.
     *
     * @param msg the message format string.
     * @param args the arguments to pass when formatting the message.
     */
    public void info(String msg, Object... args) {
        log(TreeLogger.Type.TRACE, msg, args);
    }

    /**
     * Logs an exception or error as an informational message.
     *
     * @param t the Throwable to log.
     */
    public void info(Throwable t) {
        log(TreeLogger.Type.TRACE, "", t);
    }

    /**
     * Logs an exception or error as an informational message.
     *
     * @param msg the detail message.
     * @param t the Throwable to log.
     */
    public void info(String msg, Throwable t) {
        log(TreeLogger.Type.TRACE, msg, t);
    }

    /**
     * Determines if debugging is enabled.
     *
     * @return true if debugging is enabled.
     */
    public boolean isDebugEnabled() {
        return logger.isLoggable(TreeLogger.Type.SPAM);
    }

    /**
     * This is supposed to enable or disable logging, but this implementation ignores this method.
     *
     * @param enabled
     */
    public void setDebugEnabled(boolean enabled) {
    }

    /**
     * Logs a debugging message.
     *
     * @param msg the format string used to build the log message.
     * @param args the arguments to substitute into the format string.
     */
    public void debug(String msg, Object... args) {
        log(TreeLogger.Type.SPAM, msg, args);
    }

    /**
     * Logs an exception or error as a debug message.
     *
     * @param t the Throwable to log.
     */
    public void debug(Throwable t) {
        log(TreeLogger.Type.SPAM, "", t);
    }

    /**
     * Logs an exception or error as a debug message.
     *
     * @param msg the detail message.
     * @param t the Throwable to log.
     */
    public void debug(String msg, Throwable t) {
        log(TreeLogger.Type.SPAM, msg, t);
    }

    /**
     * Returns this logger instance.
     *
     * @param name the name of the logger to return.
     * @return the logger.
     */
    public Logger getLogger(String name) {
        return this;
    }

    /**
     * Logs an ignored exception or error.
     *
     * @param t the Throwable to log.
     */
    public void ignore(Throwable t) {
        log(TreeLogger.SPAM, "THROWABLE IGNORED", t);
    }
}
