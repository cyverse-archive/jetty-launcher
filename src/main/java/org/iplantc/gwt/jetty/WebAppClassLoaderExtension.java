package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Function;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * A specialized {@link WebAppClassLoader} that allows outside resources to be brought in dynamically from the system
 * path. A warning is issued when this occurs.
 *
 * Copied from com.google.gwt.dev.shell.jetty.JettyLauncher.WebAppContextWithReload.WebAppClassLoaderExtension.
 */
public class WebAppClassLoaderExtension extends WebAppClassLoader {

    /**
     * A class loader for the Jetty web application that can only load JVM classes.
     */
    private static final ClassLoader BOOTSTRAP_ONLY_CLASS_LOADER = new ClassLoader(null) {
    };

    /**
     * The property used to indicate whether or not warning messages should be logged for web application classpath
     * lookup failures.
     */
    private static final String PROPERTY_NOWARN_WEBAPP_CLASSPATH = "gwt.nowarn.webapp.classpath";

    /**
     * The path to the META-INF/services directory.
     */
    private static final String META_INF_SERVICES = "META-INF/services";

    /**
     * The system class loader.
     */
    private final ClassLoader systemClassLoader = Thread.currentThread().getContextClassLoader();

    /**
     * Used to log the classpath lookup warning messages.
     */
    private final TreeLogger logger;

    /**
     * @param parent the parent web application context.
     * @param logger the logger to use when logging classpath lookup warning messages.
     * @throws IOException if an I/O error occurs.
     */
    public WebAppClassLoaderExtension(WebAppContext parent, TreeLogger logger) throws IOException {
        super(BOOTSTRAP_ONLY_CLASS_LOADER, parent);
        this.logger = logger;
    }

    // Things to try when looking for resources.
    private final List<? extends Function<String, URL>> FIND_RESOURCE_ATTEMPTS = Arrays.asList(

            // The first attempt checks the system class loader with the META_INF_SERVICES prefix removed.  This is
            // done specifically for META-INF/services/javax.xml.parsers.SAXParserFactory.
            new Function<String, URL>() {
                public URL apply(String name) {
                    return trySystemClassLoader(removeMetaInfServicesPrefix(name));
                }
            },

            // The second attempt checks this class loader.
            new Function<String, URL>() {
                public URL apply(String name) {
                    return superFindResource(name);
                }
            },

            // The last attempt checks on the system classpath and logs a warning message if the resource is found.
            new Function<String, URL>() {
                public URL apply(String name) {
                    URL found = systemClassLoader.getResource(name);
                    if (found != null) {
                        String warnMessage = "Server resource '" + name + "' could not be found in the web "
                                + "application, but was found on the system classpath";
                        if (!addContainingClassPathEntry(warnMessage, found, name)) {
                            return null;
                        }
                    }
                    return superFindResource(name);
                }
            });

    /**
     * Finds a resource to load.
     *
     * @param name the name of the resource.
     * @return a URL referencing the resource.
     */
    @Override
    public URL findResource(final String name) {
        for (Function<String, URL> attempt : FIND_RESOURCE_ATTEMPTS) {
            URL url = attempt.apply(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    // Things to try when looking for classes.
    private final List<? extends Function<String, Class<?>>> FIND_CLASS_ATTEMPTS = Arrays.asList(

            // The first attempt checks to see if the class is a system path and attempts to load the class using
            // the system class loader if it is.
            new Function<String, Class<?>>() {
                public Class<?> apply(String name) {
                    if (isSystemPath(name)) {
                        try {
                            return systemClassLoader.loadClass(name);
                        }
                        catch (ClassNotFoundException e) {
                            logger.log(TreeLogger.Type.DEBUG, "class, " + name + ", not found in system class loader");
                        }
                    }
                    return null;
                }
            },

            // The second attempt checks this class loader.  If the class is not found then an exception is thrown
            // if the class appears to be a system class.
            new Function<String, Class<?>>() {
                public Class<?> apply(String name) {
                    try {
                        return superFindClass(name);
                    }
                    catch (ClassNotFoundException e) {
                        if (getContext().isServerClass(name)) {
                            throw new ResourceNotFoundException(name);
                        }
                    }
                    return null;
                }
            },

            // The last attempt checks to see if the file exists in the outside world.
            new Function<String, Class<?>>() {
                public Class<?> apply(String name) {
                    String resourceName = name.replace('.', ',') + ".class";
                    URL found = systemClassLoader.getResource(name);
                    if (found != null) {
                        String msg = "Server class, " + name + ", could not be found in the web app but was found "
                                + "on the system classpath";
                        if (!addContainingClassPathEntry(msg, found, resourceName)) {
                            throw new ResourceNotFoundException(name);
                        }
                        try {
                            return superFindClass(name);
                        }
                        catch (ClassNotFoundException ignore) {}
                    }
                    throw new ResourceNotFoundException(name);
                }
            });

    /**
     * Finds a class to load.
     *
     * @param name the name of the class.
     * @return the class.
     * @throws ClassNotFoundException if the class isn't found.
     */
    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        for (Function<String, Class<?>> attempt : FIND_CLASS_ATTEMPTS) {
            try {
                Class<?> result = attempt.apply(name);
                if (result != null) {
                    return result;
                }
            }
            catch (ResourceNotFoundException e) {
                throw new ClassNotFoundException(e.getName());
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Adds the entry containing a resource to the classpath for the web application.
     *
     * @param warnMessage the warning message to log.
     * @param resource the resource that was found.
     * @param resourceName the name of the resource.
     * @return true if the classpath entry could be added.
     */
    private boolean addContainingClassPathEntry(String warnMessage, URL resource, String resourceName) {
        TreeLogger.Type logLevel
                = System.getProperty(PROPERTY_NOWARN_WEBAPP_CLASSPATH) == null ? TreeLogger.WARN : TreeLogger.DEBUG;
        TreeLogger branch = logger.branch(logLevel, warnMessage);
        String classPathUrl = classPathUrlForResource(resourceName, resource, branch);
        if (classPathUrl == null) {
            return false;
        }
        String subMessage = "Adding classpath entry, " + classPathUrl
                + ", to the web application classpath for this session";
        return addClassPath(classPathUrl, branch.branch(logLevel, subMessage));
    }

    /**
     * Adds an entry to the classpath.
     *
     * @param classPathUrl the URL to add to the classpath.
     * @param logger the logger to use when logging a classpath modification failure.
     * @return true if the entry could be added to the classpath.
     */
    private boolean addClassPath(String classPathUrl, TreeLogger logger) {
        try {
           addClassPath(classPathUrl);
           return true;
        }
        catch (IOException e) {
            logger.log(TreeLogger.Type.ERROR, "Failed add container URL: '" + classPathUrl + "\'", e);
            return false;
        }
    }

    /**
     * Finds the classpath URL for a resource that was found.
     *
     * @param resourceName the name of the resource.
     * @param resource the resource.
     * @param logger the logger to use when logging unrecognized URL formats.
     * @return the classpath URL or null if the protocol isn't recognized.
     */
    private String classPathUrlForResource(String resourceName, URL resource, TreeLogger logger) {
        String foundStr = resource.toExternalForm();
        if (resource.getProtocol().equals("file")) {
            assert foundStr.endsWith(resourceName);
            return foundStr.substring(0, foundStr.length() - resourceName.length());
        }
        else if (resource.getProtocol().equals("jar")) {
            assert foundStr.startsWith("jar:");
            assert foundStr.endsWith("!/" + resourceName);
            return foundStr.substring(4, foundStr.length() - 2 - resourceName.length());
        }
        else {
            logger.log(TreeLogger.ERROR, "Found resource but unrecognized URL format: " + foundStr);
            return null;
        }
    }

    /**
     * Attempts to find a resource using this class loader.
     *
     * @param name the name of the resource.
     * @return a URL pointing to the resource or null if the resource isn't found.
     */
    private URL superFindResource(String name) {
        return super.findResource(name);
    }

    /**
     * Attempts to find a class using this class loader.
     *
     * @param name the name of the class.
     * @return the class.
     * @throws ClassNotFoundException if the class can't be found.
     */
    private Class<?> superFindClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    /**
     * Attempts to load the resource using the system class loader.
     *
     * @param name the name of the resource, with the META-INF services path prefix removed.
     * @return a URL that points to the resource or null if the resource isn't found.
     */
    private URL trySystemClassLoader(String name) {
        return isSystemPath(name) ? systemClassLoader.getResource(name) : null;
    }

    /**
     * Determines whether or not a path is a system path.  Xerces and Jasper are both included in order to allow the
     * most common XML parsers to be loaded automatically.
     *
     * @param name the path name.
     * @return true if the name represents a system path.
     */
    private Boolean isSystemPath(String name) {
        name = name.replace('/', '.');
        return getContext().isSystemClass(name)
                || name.startsWith("org.apache.jasper.")
                || name.startsWith("org.apache.xerces.");
    }

    /**
     * Removes the leading part of the resource name if it begins with the META-INF services path.
     *
     * @param name the name of the resource/
     * @return the name with the prefix removed.
     */
    private String removeMetaInfServicesPrefix(String name) {
        return name.startsWith(META_INF_SERVICES) ? name.substring(META_INF_SERVICES.length()) : name;
    }
}
