package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.TreeLogger;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A static method to help prevent memory leaks in the JRE.
 */
public class LeakPreventor {

    // Prevent instantiation.
    private LeakPreventor() {
    }

    /**
     * This is a modified version of JreMemoryLeakPreventionListener.java found in the Apache Tomcat project at
     *
     * http://svn.apache.org/repos/asf/tomcat/trunk/java/org/apache/catalina/core/ JreMemoryLeakPreventionListener.java
     *
     * Relevant part of the Tomcat NOTICE, retrieved from http://svn.apache.org/repos/asf/tomcat/trunk/NOTICE Apache
     * Tomcat Copyright 1999-2010 The Apache Software Foundation
     *
     * This product includes software developed by The Apache Software Foundation (http://www.apache.org/).
     */
    public static void jreLeakPrevention(TreeLogger logger) {

        // Trigger a call to sun.awt.AppContext.getAppContext(). This will
        // pin the common class loader in memory but that shouldn't be an
        // issue.
        ImageIO.getCacheDirectory();

        /*
         * Several components end up calling: sun.misc.GC.requestLatency(long)
         *
         * Those libraries / components known to trigger memory leaks due to
         * eventual calls to requestLatency(long) are: -
         * javax.management.remote.rmi.RMIConnectorServer.start()
         */
        try {
            Class<?> clazz = Class.forName("sun.misc.GC");
            Method method = clazz.getDeclaredMethod("requestLatency",
                    new Class[]{long.class});
            method.invoke(null, Long.valueOf(3600000));
        }
        catch (ClassNotFoundException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        }
        catch (SecurityException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        }
        catch (NoSuchMethodException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        }
        catch (IllegalArgumentException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        }
        catch (IllegalAccessException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        }
        catch (InvocationTargetException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        }

        /*
         * Calling getPolicy retains a static reference to the context class loader.
         */
        try {
            // Policy.getPolicy();
            Class<?> policyClass = Class.forName("javax.security.auth.Policy");
            Method method = policyClass.getMethod("getPolicy");
            method.invoke(null);
        }
        catch (ClassNotFoundException e) {
            // Ignore. The class is deprecated.
        }
        catch (SecurityException e) {
            // Ignore. Don't need call to getPolicy() to be successful,
            // just need to trigger static initializer.
        }
        catch (NoSuchMethodException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        }
        catch (IllegalArgumentException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        }
        catch (IllegalAccessException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        }
        catch (InvocationTargetException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        }

        /*
         * Creating a MessageDigest during web application startup initializes the
         * Java Cryptography Architecture. Under certain conditions this starts a
         * Token poller thread with TCCL equal to the web application class loader.
         *
         * Instead we initialize JCA right now.
         */
        java.security.Security.getProviders();

        /*
         * Several components end up opening JarURLConnections without first
         * disabling caching. This effectively locks the file. Whilst more
         * noticeable and harder to ignore on Windows, it affects all operating
         * systems.
         *
         * Those libraries/components known to trigger this issue include: - log4j
         * versions 1.2.15 and earlier - javax.xml.bind.JAXBContext.newInstance()
         */

        // Set the default URL caching policy to not to cache
        try {
            // Doesn't matter that this JAR doesn't exist - just as long as
            // the URL is well-formed
            URL url = new URL("jar:file://dummy.jar!/");
            URLConnection uConn = url.openConnection();
            uConn.setDefaultUseCaches(false);
        }
        catch (MalformedURLException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.jarUrlConnCacheFail", e);
        }
        catch (IOException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.jarUrlConnCacheFail", e);
        }

        /*
         * Haven't got to the root of what is going on with this leak but if a web
         * app is the first to make the calls below the web application class loader
         * will be pinned in memory.
         */
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.xmlParseFail", e);
        }
    }
}
