package ir.ac.kntu.exception;

/**
 * Thrown when an operation references a table that does not exist.
 */
public class TableNotFoundException extends FqlException {
    public TableNotFoundException(String tableName) {
        super("Table not found: " + tableName);
    }
}