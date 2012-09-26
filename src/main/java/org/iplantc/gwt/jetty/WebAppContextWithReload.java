package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.TreeLogger;
import java.lang.reflect.Method;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * A {@link WebAppContext} tailored to GWT hosted mode, which features hot-reload with a new {@link WebAppClassLoader}
 * to pick up disk changes.  The default Jetty {@code WebAppContext} will create new instances of servlets, but it will
 * not create a brand new {@link ClassLoader}.  By creating a new {@code ClassLoader} each time, we re-read updated
 * classes from disk.
 *
 * Also provides special class filtering to isolate the web application from the GWT hosting environment.
 *
 * Copied from com.google.gwt.dev.shell.jetty.JettyLauncher.WebAppContextWithReload.
 */
public class WebAppContextWithReload extends WebAppContext {

    /**
     * The class loader to use.
     */
    private WebAppClassLoaderExtension classLoader;

    /**
     * The logger to use for error and warning messages.
     */
    private final TreeLogger logger;

    /**
     * @param logger the logger to use for error and warning messages.
     * @param webApp the path to the web application root directory.
     * @param contextPath the context path to use.
     */
    public WebAppContextWithReload(TreeLogger logger, String webApp, String contextPath) {
        super(webApp, contextPath);
        this.logger = logger;
    }

    /**
     * Starts the web application, adding a specialized class loader.
     *
     * @throws Exception if an error occurs.
     */
    @Override
    protected void doStart() throws Exception {
        classLoader = new WebAppClassLoaderExtension(this, logger);
        setClassLoader(classLoader);
        super.doStart();
    }

    /**
     * Stops the web application, destroying the specialized class loader.
     *
     * @throws Exception  if an error occurs.
     */
    @Override
    protected void doStop() throws Exception {
        super.doStop();
        Class<?> jdbcUnloader = classLoader.loadClass("com.google.gwt.dev.shell.jetty.JDBCUnloader");
        Method unload = jdbcUnloader.getMethod("unload");
        unload.invoke(null);
        setClassLoader(null);
    }
}
