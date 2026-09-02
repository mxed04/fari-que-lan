package ir.ac.kntu.engine;

import ir.ac.kntu.exception.TableAlreadyExistsException;
import ir.ac.kntu.exception.TableNotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.*;
import ir.ac.kntu.parser.ParsedCommand;

import java.util.*;

/**
 * Core query execution engine managing DDL and DML operations.
 */
public class DatabaseEngine {
    private final Database database;

    public DatabaseEngine(Database database) {
        this.database = Objects.requireNonNull(database, "Database reference cannot be null");
    }

    /**
     * Dispatches a parsed command to its dedicated execution routine.
     *
     * @param command parsed FQL command
     * @return ExecutionResult containing output data or affected record counts
     */
    public ExecutionResult execute(ParsedCommand command) {
        return switch (command.getType()) {
            case CREATE -> executeCreate(command);
            case DROP -> executeDrop(command);
            case ADD -> executeAdd(command);
            case QUIT -> ExecutionResult.empty();
            default -> throw new UnsupportedOperationException("Command not supported in Phase 3: " + command.getType());
        };
    }

    private ExecutionResult executeCreate(ParsedCommand command) {
        String tableName = command.getTableName();
        if (database.hasTable(tableName)) {
            throw new TableAlreadyExistsException(tableName);
        }

        List<String> rawColumns = command.getArguments();
        if (rawColumns.isEmpty()) {
            throw new ValidationException("Table creation requires at least one column definition");
        }

        List<Column> columns = new ArrayList<>();
        Set<String> columnNames = new HashSet<>();

        for (String rawCol : rawColumns) {
            String[] tokens = rawCol.trim().split("\\s+");
            if (tokens.length != 2) {
                throw new ValidationException("Invalid column definition: '" + rawCol + "' (expected '<name> <type>')");
            }
            String colName = tokens[0].trim().toLowerCase();
            if (columnNames.contains(colName)) {
                throw new ValidationException("Duplicate column definition: " + colName);
            }

            DataType type = DataType.fromString(tokens[1]);
            columns.add(new Column(colName, type));
            columnNames.add(colName);
        }

        database.createTable(new Table(tableName, columns));
        return ExecutionResult.empty();
    }

    private ExecutionResult executeDrop(ParsedCommand command) {
        String tableName = command.getTableName();
        if (!database.hasTable(tableName)) {
            throw new TableNotFoundException(tableName);
        }

        database.dropTable(tableName);
        return ExecutionResult.empty();
    }

    private ExecutionResult executeAdd(ParsedCommand command) {
        String tableName = command.getTableName();
        if (!database.hasTable(tableName)) {
            throw new TableNotFoundException(tableName);
        }

        Table table = database.getTable(tableName);
        Map<String, String> variables = command.getVariables();

        Row newRow = new Row();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String colName = entry.getKey().toLowerCase();
            if (!table.hasColumn(colName)) {
                throw new ValidationException("Column '" + colName + "' does not exist in table '" + tableName + "'");
            }

            Column column = table.getColumn(colName);
            Value val = Value.of(column.getType(), entry.getValue());
            newRow.set(colName, val);
        }

        // Apply default values for unspecified columns and store in table
        table.addRow(newRow);

        // Fetch the stored complete row to return in the execution result
        Row insertedRow = table.getRows().get(table.getRows().size() - 1);
        return new ExecutionResult(table.getColumns(), List.of(insertedRow));
    }

    public Database getDatabase() {
        return database;
    }
}