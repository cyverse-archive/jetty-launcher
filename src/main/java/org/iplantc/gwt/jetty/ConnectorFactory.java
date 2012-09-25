package org.iplantc.gwt.jetty;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.thirdparty.guava.common.io.Closeables;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Creates abstract connectors based on settings provided by the caller.
 */
public abstract class ConnectorFactory {

    /**
     * Gets the connector to use.
     *
     * @param logger used to log informational messages.
     * @return the connector.
     */
    public abstract AbstractConnector getConnector(TreeLogger logger);

    /**
     * The connector factory used to generate SSL connectors.
     */
    private static class SslConnectorFactory extends ConnectorFactory {

        /**
         * Indicates which client authentication strategy to use.
         */
        private ClientAuth clientAuth;

        /**
         * The path to the keystore to use for SSL connections.
         */
        private String keystorePath;

        /**
         * The password used to access the keystore.
         */
        private String keystorePassword;

        /**
         * @param clientAuth indicates which client authentication strategy to use.
         * @param keystorePath the path to the keystore to use for SSL connections.
         * @param keystorePassword the password used to access the keystore.
         */
        private SslConnectorFactory(ClientAuth clientAuth, String keystorePath, String keystorePassword) {
            this.clientAuth = clientAuth;
            this.keystorePath = keystorePath;
            this.keystorePassword = keystorePassword;
        }

        /**
         * Creates and returns a new SSL connector.
         *
         * @param logger the logger to use when logging informational messages.
         * @return the connector.
         */
        @Override
        public AbstractConnector getConnector(TreeLogger logger) {
            TreeLogger sslLogger = logger.branch(TreeLogger.INFO, "Listening for SSL connections");
            if (sslLogger.isLoggable(TreeLogger.Type.TRACE)) {
                sslLogger.log(TreeLogger.Type.TRACE, "Using keystore " + keystorePath);
            }
            KeyStore keystore = loadKeystore(sslLogger);
            SslContextFactory contextFactory = new SslContextFactory();
            contextFactory.setKeyStore(keystore);
            contextFactory.setKeyStorePassword(keystorePassword);
            contextFactory.setTrustStore(keystore);
            contextFactory.setTrustStorePassword(keystorePassword);
            return clientAuth.getConnector(contextFactory, sslLogger);
        }

        /**
         * Loads the selected keystore from the file system.  A runtime exception will be thrown if the keystore
         * can't be loaded.
         *
         * @param logger the logger to use for error messages.
         * @return the keystore.
         */
        private KeyStore loadKeystore(TreeLogger logger) {
            String errorMsg = "unable to load the SSL keystore";
            char[] password = keystorePassword.toCharArray();
            FileInputStream in = null;
            try {
                in = new FileInputStream(keystorePath);
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                keystore.load(in, password);
                return keystore;
            }
            catch (NoSuchAlgorithmException e) {
                logger.log(TreeLogger.Type.ERROR, errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
            catch (CertificateException e) {
                logger.log(TreeLogger.Type.ERROR, errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
            catch (KeyStoreException e) {
                logger.log(TreeLogger.Type.ERROR, errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
            catch (IOException e) {
                logger.log(TreeLogger.Type.ERROR, errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
            finally {
                if (in != null) {
                    Closeables.closeQuietly(in);
                }
            }
        }
    }

    /**
     * The connector factory to use when creating non-SSL connectors.
     */
    private static class PlainConnectorFactory extends ConnectorFactory {

        /**
         * Creates and returns a new connector.
         *
         * @param logger the logger to use when creating the connector.
         * @return the connector.
         */
        @Override
        public AbstractConnector getConnector(TreeLogger logger) {
            return new SelectChannelConnector();
        }
    }

    /**
     * Used to build connector factories.
     */
    public static class ConnectorFactoryBuilder {

        /**
         * True if SSL should be used.
         */
        private boolean useSsl = false;

        /**
         * The client authentication strategy.
         */
        private ClientAuth clientAuth = ClientAuth.NONE;

        /**
         * The path to the keystore.
         */
        private String keystorePath;

        /**
         * The password used to access the keystore.
         */
        private String keystorePassword;

        /**
         * @param useSsl true if SSL should be used.
         * @return a reference to this builder.
         */
        public ConnectorFactoryBuilder setUseSsl(boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        /**
         * @param clientAuth the client authentication strategy.
         * @return a reference to this builder.
         */
        public ConnectorFactoryBuilder setClientAuth(ClientAuth clientAuth) {
            this.clientAuth = clientAuth;
            return this;
        }

        /**
         * @param keystorePath the path to the keystore.
         * @return a reference to this builder.
         */
        public ConnectorFactoryBuilder setKeystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        /**
         * @param keystorePassword the password used to access the keystore.
         * @return a reference to this builder.
         */
        public ConnectorFactoryBuilder setKeystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        /**
         * Builds the connector factory.
         *
         * @return the connector factory.
         */
        public ConnectorFactory build() {
            if (useSsl) {
                validateSslParams();
                return new SslConnectorFactory(clientAuth, keystorePath, keystorePassword);
            }
            else {
                return new PlainConnectorFactory();
            }
        }

        /**
         * Validates the parameters used for building SSL connector factories.
         */
        private void validateSslParams() {
            if (clientAuth == null) {
                throw new NullPointerException("the client authentication strategy is required for SSL");
            }
            if (keystorePath == null) {
                throw new NullPointerException("the keystore path is required for SSL");
            }
            if (keystorePassword == null) {
                throw new NullPointerException("the keystore password is required for SSL");
            }
        }
    }
}
