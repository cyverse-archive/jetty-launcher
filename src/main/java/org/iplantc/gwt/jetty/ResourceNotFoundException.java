package org.iplantc.gwt.jetty;

/**
 * A runtime exception used to indicate when a resource can't be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * The name of the resource.
     */
    private final String name;

    /**
     * @param name the name of the resource.
     */
    public ResourceNotFoundException(String name) {
        super("resource, " + name + ", not found");
        this.name = name;
    }

    /**
     * @return the name of the resource.
     */
    public String getName() {
        return name;
    }
}
