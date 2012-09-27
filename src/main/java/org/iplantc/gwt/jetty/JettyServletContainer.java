package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import java.io.File;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.util.log.Log;

/**
 * The servlet container that is launched.
 */
public class JettyServletContainer extends ServletContainer {

    private final int actualPort;

    private final File appRootDir;

    private final TreeLogger logger;

    private final Server server;

    private final WebAppContext wac;

    public JettyServletContainer(TreeLogger logger, Server server, WebAppContext wac, int actualPort, File appRootDir) {
        this.logger = logger;
        this.server = server;
        this.wac = wac;
        this.actualPort = actualPort;
        this.appRootDir = appRootDir;
    }

    @Override
    public int getPort() {
        return actualPort;
    }

    @Override
    public void refresh() throws UnableToCompleteException {
        String msg = "Reloading web app to reflect changes in " + appRootDir.getAbsolutePath();
        TreeLogger branch = logger.branch(TreeLogger.INFO, msg);
        Log.setLog(new JettyTreeLogger(branch));
        try {
            wac.stop();
            wac.start();
            branch.log(TreeLogger.INFO, "Reload completed successfully");
        }
        catch (Exception e) {
            branch.log(TreeLogger.ERROR, "Unable to restart embedded Jetty server", e);
            throw new UnableToCompleteException();
        }
        finally {
            Log.setLog(new JettyTreeLogger(logger));
        }
    }

    @Override
    public void stop() throws UnableToCompleteException {
        TreeLogger branch = logger.branch(TreeLogger.INFO, "Stopping Jetty server");
        Log.setLog(new JettyTreeLogger(branch));
        try {
            server.stop();
            server.setStopAtShutdown(false);
            branch.log(TreeLogger.TRACE, "Stopped successfully");
        }
        catch (Exception e) {
            branch.log(TreeLogger.ERROR, "Unable to stop embedded Jetty server", e);
            throw new UnableToCompleteException();
        }
        finally {
            Log.setLog(new JettyTreeLogger(logger));
        }
    }
}
