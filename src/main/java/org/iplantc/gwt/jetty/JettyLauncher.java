package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.Util;
import java.io.File;
import java.net.BindException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
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

    static {
        // Suppress Jetty log initialization.
        Log.setLog(new JettyNullLogger());

        // Make JDT the default Ant compiler so that JSP compilation just works.  If we don't wet this, it's
        // difficult to make JSP compiltion work.
        String antJavaC = System.getProperty("build.compiler", "org.eclipse.jdt.core.JDTCompilerAdapter");
        System.setProperty("build.compiler", antJavaC);
    }

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

    private final Map<String,ArgHandler> argHandlerFor = new HashMap<String, ArgHandler>();

    public JettyLauncher() {
        initArgHandlers();
    }

    /**
     * Initializes the argument handlers.
     */
    private void initArgHandlers() {

        // The handler for the "ssl" argument.
        argHandlerFor.put("ssl", new ArgHandler() {
            public void handle(TreeLogger logger, String value) throws ArgException {
                useSsl = true;
                if (keystore == null) {
                    URL keystoreUrl = getClass().getResource("localhost.keystore");
                    if (keystoreUrl == null) {
                        logger.log(TreeLogger.ERROR, "Default GWT keystore not found");
                        throw new ArgException();
                    }
                    keystore = keystoreUrl.toExternalForm();
                    keystorePassword = "localhost";
                }
            }
        });

        // The handler for the "keystore" argument.
        argHandlerFor.put("keystore", new ArgHandler() {
           public void handle(TreeLogger logger, String value) throws ArgException {
               useSsl = true;
               keystore = value;
           }
        });

        // The handler for the "password" argument.
        argHandlerFor.put("password", new ArgHandler() {
            public void handle(TreeLogger logger, String value) throws ArgException {
                useSsl = true;
                keystorePassword = value;
            }
        });

        // The handler for the "pwfile" argument.
        argHandlerFor.put("pwfile", new ArgHandler() {
            public void handle(TreeLogger logger, String value) throws ArgException {
                useSsl = true;
                keystorePassword = Util.readFileAsString(new File(value));
                if (keystorePassword == null) {
                    logger.log(TreeLogger.ERROR, "Unable to read keystore password from '" + value + "'");
                    throw new ArgException();
                }
                keystorePassword = keystorePassword.trim();
            }
        });

        // The handler for the "clientAuth" argument.
        argHandlerFor.put("clientAuth", new ArgHandler() {
            public void handle(TreeLogger logger, String value) throws ArgException {
                useSsl = true;
                try {
                    clientAuth = ClientAuth.valueOf(value);
                }
                catch (IllegalArgumentException e) {
                    logger.log(TreeLogger.WARN, "Ignoring invalid clientAuth of '" + value + "'");
                }
            }
        });
    }

    /**
     * @return the name of the embedded Jetty servlet.
     */
    @Override
    public String getName() {
        return "Jetty";
    }

    /**
     * @return true if the server should use SSL.
     */
    @Override
    public boolean isSecure() {
        return useSsl;
    }

    /**
     * Processes the arguments to this class.
     *
     * @param logger the logger to use for error and warning messages.
     * @param arguments the arguments as a comma-delimited string.
     * @return true if the arguments are valid.
     */
    @Override
    public boolean processArguments(TreeLogger logger, String arguments) {
        if (arguments != null && arguments.length() > 0) {
            for (String arg : arguments.split(",")) {
                String[] components = arg.split("=", 2);
                String name = components[0];
                String value = components.length > 1 ? components[1] : null;
                try {
                    processArgument(logger, name, value);
                }
                catch (ArgException e) {
                    return false;
                }
            }
        }
        return validateArguments(logger);
    }

    /**
     * Processes a single argument in the argument list.
     *
     * @param logger the logger to use for error and warning messages.
     * @param name the argument name.
     * @param value the argument value.
     * @throws org.iplantc.gwt.jetty.JettyLauncher.ArgException if the argument can't be processed.
     */
    private void processArgument(TreeLogger logger, String name, String value) throws ArgException {
        ArgHandler handler = argHandlerFor.get(name);
        if (handler == null) {
            logger.log(TreeLogger.ERROR, "Unexpected argument to " + getClass().getSimpleName() + ": " + name);
            throw new ArgException();
        }
    }

    /**
     * Validates the arguments after they've been processed.
     *
     * @param logger the logger to use for error messages.
     * @return true if the arguments are valid.
     */
    private boolean validateArguments(TreeLogger logger) {
        if (useSsl) {
            if (keystore == null) {
                logger.log(TreeLogger.ERROR, "A keystore is required to use SSL");
                return false;
            }
            if (keystorePassword == null) {
                logger.log(TreeLogger.ERROR, "A keystore password is required to use SSL");
                return false;
            }
        }
        return true;
    }

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
        printClassPath(logger, getClass().getClassLoader());
        checkStartParams(logger, port, appRootDir);
        Log.setLog(new JettyTreeLogger(logger));
        LeakPreventor.jreLeakPrevention(logger);
        disableXmlValidation();
        AbstractConnector connector = createConnector(logger, bindAddress, port);
        Server server = createServer(connector);
        WebAppContext wac = new WebAppContextWithReload(logger, appRootDir.getAbsolutePath(), "/");
        configureServerLogging(logger, server, wac);
        server.start();
        server.setStopAtShutdown(true);
        Log.setLog(new JettyTreeLogger(logger));
        return new JettyServletContainer(logger, server, wac, connector.getLocalPort(), appRootDir);
    }

    private void printClassPath(TreeLogger logger, ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                logger.log(TreeLogger.WARN, url.toExternalForm());
            }
        }
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
     * Creates the connector to use for the server.
     *
     * @param logger the logger to use.
     * @param bindAddress the address to listen to.
     * @param port the port to listen to.
     * @return the connector.
     */
    private AbstractConnector createConnector(TreeLogger logger, String bindAddress, int port) {
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
        return connector;
    }

    /**
     * Creates the Jetty server.
     *
     * @param connector the connector to use.
     * @return the server.
     */
    private Server createServer(AbstractConnector connector) {
        Server server = new Server();
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

    /**
     * Thrown when an argument can't be processed.
     */
    public static class ArgException extends Exception {}

    /**
     * An interface that can handle arguments.
     *
     * @param logger the logger to use.
     * @param value the argument value.
     * @throws ArgException if the argument can't be processed.
     */
    private static interface ArgHandler {
        public void handle(TreeLogger logger, String value) throws ArgException;
    }
}
