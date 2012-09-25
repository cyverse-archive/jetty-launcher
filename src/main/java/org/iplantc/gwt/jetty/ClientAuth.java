package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.TreeLogger;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Indicates whether or not client authentication should be required.
 */
public enum ClientAuth {

    NONE    (false, false, "Not requesting client certificates"),
    WANT    (true,  false, "Requesting client certificates"),
    REQUIRE (true,  true,  "Requiring client certificates");

    /**
     * Indicates whether or not client authentication is requested.
     */
    private final boolean wantClientAuth;

    /**
     * Indicates whether or not client authentication is required.
     */
    private final boolean needClientAuth;

    /**
     * The message to log when getting a connector for a client authentication strategy.
     */
    private final String logMsg;

    /**
     * @param wantClientAuth indicates whether or not client authentication is requested.
     * @param needClientAuth indicates whether or not client authentication is required.
     * @param logMsg the message to log when obtaining an SSL socket connector.
     */
    private ClientAuth(boolean wantClientAuth, boolean needClientAuth, String logMsg) {
        this.wantClientAuth = wantClientAuth;
        this.needClientAuth = needClientAuth;
        this.logMsg = logMsg;
    }

    /**
     * Gets an SSL socket connector for the provided SSL context factory.  The context factory should have all of
     * its settings configured before calling this method except for the {@code wantClientAuth} and
     * {@code needClientAuth} settings.
     *
     * @param contextFactory the SSL context factory.
     * @param logger the logger to use when logging informational messages.
     * @return the SSL socket connector.
     */
    public SslSocketConnector getConnector(SslContextFactory contextFactory, TreeLogger logger) {
        logger.log(TreeLogger.Type.TRACE, logMsg);
        contextFactory.setWantClientAuth(wantClientAuth);
        contextFactory.setNeedClientAuth(needClientAuth);
        return new SslSocketConnector(contextFactory);
    }
}
