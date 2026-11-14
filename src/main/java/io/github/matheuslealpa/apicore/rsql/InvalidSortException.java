package io.github.matheuslealpa.apicore.rsql;

/**
 * A {@code ?sort=} property outside the whitelist of the endpoint. Responded as 400.
 */
public class InvalidSortException extends RuntimeException {

    private final String property;

    public InvalidSortException(String property) {
        super("Sorting by property '" + property + "' is not allowed.");
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
