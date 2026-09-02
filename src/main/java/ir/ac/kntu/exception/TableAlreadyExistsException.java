package ir.ac.kntu.exception;

/**
 * Thrown when attempting to create a table that already exists in the database.
 */
public class TableAlreadyExistsException extends FqlException {
    public TableAlreadyExistsException(String tableName) {
        super("Table already exists: " + tableName);
    }
}