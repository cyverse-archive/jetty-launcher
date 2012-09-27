package org.iplantc.gwt.jetty;

import org.eclipse.jetty.util.log.Logger;

/**
 * A Jetty logger class that suppresses all output.
 */
public class JettyNullLogger implements Logger {

    public String getName() {
        return getClass().getSimpleName();
    }

    public void warn(String fmt, Object... args) {}

    public void warn(Throwable t) {}

    public void warn(String msg, Throwable t) {}

    public void info(String fmt, Object... args) {
    }

    public void info(Throwable t) {}

    public void info(String msg, Throwable t) {
    }

    public boolean isDebugEnabled() {
        return false;
    }

    public void setDebugEnabled(boolean enabled) {}

    public void debug(String fmt, Object... args) {}

    public void debug(Throwable t) {}

    public void debug(String msg, Throwable t) {
    }

    public Logger getLogger(String name) {
        return this;
    }

    public void ignore(Throwable t) {}
}
