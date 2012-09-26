package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import java.io.File;
import java.net.BindException;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.WebAppContext;
import org.iplantc.gwt.jetty.ConnectorFactory.ConnectorFactoryBuilder;

/**
 * A {@link ServletContainerLauncher} implementation that launches a Jetty 8.1.7 servlet container. Most of this code
 * was taken directly from {@link com.google.gwt.gwt.dev.shell.jetty.JettyLauncher}.
 */
public class JettyLauncher extends ServletContainerLauncher {

    /**
     * Used to synchronize updates to mutable data in this class.
     */
    private final Object privateInstanceLock = new Object();

    /**
     * The selected base log level.
     */
    private TreeLogger.Type baseLogLevel = TreeLogger.INFO;

    /**
     * Indicates whether client authentication is disabled, desired or required.
     */
    private ClientAuth clientAuth;

    /**
     * The keystore to use for SSL connections.
     */
    private String keystore;

    /**
     * The password used to access the keystore.
     */
    private String keystorePassword;

    /**
     * The address to listen to for incoming connections.
     */
    private String bindAddress = null;

    /**
     * True if the server should use SSL.
     */
    private boolean useSsl;

    /*
     * TODO: This is a hack to pass the base log level to the SCL. We'll have to
     * figure out a better way to do this for SCLs in general. Please do not
     * depend on this method, as it is subject to change.
     */
    public void setBaseRequestLogLevel(TreeLogger.Type baseLogLevel) {
        synchronized (privateInstanceLock) {
            this.baseLogLevel = baseLogLevel;
        }
    }

    /*
     * TODO: This is a hack to pass the base log level to the SCL. We'll have to
     * figure out a better way to do this for SCLs in general.
     */
    public TreeLogger.Type getBaseLogLevel() {
        synchronized (privateInstanceLock) {
            return this.baseLogLevel;
        }
    }

    /**
     * @param bindAddress the address to listen to for incoming connections.
     */
    @Override
    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    /**
     * Starts the servlet container.
     *
     * @param logger the logger to use when logging messages.
     * @param port the listen port.
     * @param appRootDir the root directory for the web application.
     * @return the servlet container.
     * @throws BindException if we can't open the listen socket.
     * @throws Exception if an unexpected error occurs.
     */
    @Override
    public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws BindException, Exception {
        checkStartParams(logger, port, appRootDir);
        Log.setLog(new JettyTreeLogger(logger));
        LeakPreventor.jreLeakPrevention(logger);
        disableXmlValidation();
        Server server = createServer(logger, bindAddress, port);
        WebAppContext wac = new WebAppContextWithReload(logger, appRootDir.getAbsolutePath(), "/");
        configureServerLogging(logger, server, wac);
        server.start();
        server.setStopAtShutdown(true);
        Log.setLog(new JettyTreeLogger(logger));
        // TODO: implement JettyServletContainer and instantiate it here.
        return null;
    }

    /**
     * Configures logging for the server.
     *
     * @param logger the logger to use.
     * @param server the web server.
     * @param wac the web application context.
     */
    private void configureServerLogging(TreeLogger logger, Server server, WebAppContext wac) {
        RequestLogHandler logHandler = new RequestLogHandler();
        logHandler.setRequestLog(new JettyRequestLogger(logger, getBaseLogLevel()));
        logHandler.setHandler(wac);
        server.setHandler(logHandler);
    }

    /**
     * Creates the Jetty server.
     *
     * @param logger the logger to use.
     * @param bindAddress the address to listen to.
     * @param port the port to listen to.
     * @return the server.
     */
    private Server createServer(TreeLogger logger, String bindAddress, int port) {
        Server server = new Server();
        AbstractConnector connector = new ConnectorFactoryBuilder()
                .setUseSsl(useSsl)
                .setClientAuth(clientAuth)
                .setKeystorePath(keystore)
                .setKeystorePassword(keystorePassword)
                .build()
                .getConnector(logger);
        if (bindAddress != null) {
            connector.setHost(bindAddress);
        }
        connector.setReuseAddress(false);
        connector.setSoLingerTime(0);
        server.addConnector(connector);
        return server;
    }

    /**
     * Disables XML validation in Jetty.
     */
    private void disableXmlValidation() {
        System.setProperty("org.mortbay.xml.XmlParser.Validating", "false");
    }

    /**
     * Verifies that we have all of the information we need to start the server.
     *
     * @param logger used to log error and informational messages.
     * @param port the port to listen to.
     * @param appRootDir the root directory for the web application.
     */
    private void checkStartParams(TreeLogger logger, int port, File appRootDir) {
        if (logger == null) {
            throw new NullPointerException("the logger may not be null");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("the port must be between 0 and 65535, inclusive");
        }
        if (appRootDir == null) {
            throw new NullPointerException("the app root directory may not be null");
        }
    }
}
