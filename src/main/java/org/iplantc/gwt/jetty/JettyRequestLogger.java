package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.TreeLogger;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpFields.Field;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

/**
 * A {@link RequestLog} implementation that forwards log messages to a {@link TreeLogger}.
 */
public class JettyRequestLogger extends AbstractLifeCycle implements RequestLog {

    /**
     * The tree logger to forward log messages to.
     */
    private final TreeLogger logger;

    /**
     * The log level to use by default.
     */
    private final TreeLogger.Type normalLogLevel;

    /**
     * @param logger the tree logger to forward log messages to.
     * @param normalLogLevel the log level to use by default.
     */
    public JettyRequestLogger(TreeLogger logger, TreeLogger.Type normalLogLevel) {
        assert logger != null;
        assert normalLogLevel != null;
        this.logger = logger;
        this.normalLogLevel = normalLogLevel;
    }

    /**
     * Logs a message to the tree logger.
     *
     * @param req the request.
     * @param res the response.
     */
    public void log(Request req, Response res) {

    }

    private static class LoggingStrategy {

        /**
         * The log level to use for request status information.
         */
        private final TreeLogger.Type statusLevel;

        /**
         * The log level to use for HTTP headers.
         */
        private final TreeLogger.Type headersLevel;

        /**
         * @param statusLevel the log level to use for request status information.
         * @param headersLevel the log level to use for HTTP headers.
         */
        private LoggingStrategy(TreeLogger.Type statusLevel, TreeLogger.Type headersLevel) {
            this.statusLevel = statusLevel;
            this.headersLevel = headersLevel;
        }

        /**
         * Gets the logging strategy to use for a request and response.
         *
         * @param req the request.
         * @param res the response.
         * @param normalLogLevel the log level to use under normal circumstances.
         * @return the logging strategy.
         */
        public static LoggingStrategy getLoggingStrategy(Request req, Response res, TreeLogger.Type normalLogLevel) {
            if (res.getStatus() >= 500) {
                return new LoggingStrategy(TreeLogger.ERROR, TreeLogger.INFO);
            }
            else if (res.getStatus() == 404) {
                return req.getRequestURI().equals("/favicon.ico") && req.getQueryString() == null
                        ? new LoggingStrategy(TreeLogger.TRACE, TreeLogger.DEBUG)
                        : new LoggingStrategy(TreeLogger.WARN, TreeLogger.INFO);
            }
            else if (res.getStatus() >= 400) {
                return new LoggingStrategy(TreeLogger.WARN, TreeLogger.INFO);
            }
            else {
                return new LoggingStrategy(normalLogLevel, TreeLogger.DEBUG);
            }
        }

        /**
         * Logs request and response information.
         *
         * @param req the request to log.
         * @param res the response to log.
         * @param logger the logger to use.
         */
        public void log(Request req, Response res, TreeLogger logger) {
            if (logger.isLoggable(statusLevel)) {
                TreeLogger branch = logger.branch(statusLevel, statusMsg(req, res));
                logFields(req.getConnection().getRequestFields(), branch.branch(headersLevel, "Request headers"));
                logFields(res.getHttpFields(), branch.branch(headersLevel, "Response headers"));
            }
        }

        /**
         * Builds a status message for a request and response.
         *
         * @param req the request.
         * @param res the response.
         * @return the status message.
         */
        private String statusMsg(Request req, Response res) {
            StringBuilder builder = new StringBuilder();
            builder.append(res.getStatus());
            builder.append(" - ");
            builder.append(req.getMethod());
            builder.append(" ");
            builder.append(req.getUri());
            builder.append(" (");
            if (req.getRemoteUser() != null) {
                builder.append(req.getRemoteUser());
                builder.append("@");
            }
            builder.append(req.getRemoteHost());
            builder.append(")");
            if (res.getContentCount() > 0) {
                builder.append(" ");
                builder.append(res.getContentCount());
                builder.append(" bytes");
            }
            return builder.toString();
        }

        /**
         * Logs a set of HTTP fields.
         *
         * @param fields the HTTP fields to log.
         * @param logger the logger to use.
         */
        private void logFields(HttpFields fields, TreeLogger logger) {
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.getField(i);
                logger.log(headersLevel, field.getName() + ": " + field.getValue());
            }
        }
    }
}
