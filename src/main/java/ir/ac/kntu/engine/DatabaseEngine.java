package ir.ac.kntu.engine;

import ir.ac.kntu.engine.evaluator.ExpressionEvaluator;
import ir.ac.kntu.exception.TableAlreadyExistsException;
import ir.ac.kntu.exception.TableNotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.*;
import ir.ac.kntu.parser.ParsedCommand;

import java.util.*;

/**
 * Core query execution engine managing DDL, DML, and projected query operations.
 */
public class DatabaseEngine {
    private final Database database;
    private final ExpressionEvaluator evaluator;

    public DatabaseEngine(Database database) {
        this.database = Objects.requireNonNull(database, "Database reference cannot be null");
        this.evaluator = new ExpressionEvaluator();
    }

    public ExecutionResult execute(ParsedCommand command) {
        return switch (command.getType()) {
            case CREATE -> executeCreate(command);
            case DROP -> executeDrop(command);
            case ADD -> executeAdd(command);
            case GET -> executeGet(command);
            case SET -> executeSet(command);
            case DEL -> executeDel(command);
            case QUIT -> ExecutionResult.empty();
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
        ensureTableExists(tableName);

        database.dropTable(tableName);
        return ExecutionResult.empty();
    }

    private ExecutionResult executeAdd(ParsedCommand command) {
        String tableName = command.getTableName();
        ensureTableExists(tableName);

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

        table.addRow(newRow);
        Row insertedRow = table.getRows().get(table.getRows().size() - 1);
        return new ExecutionResult(table.getColumns(), List.of(insertedRow));
    }

    private ExecutionResult executeGet(ParsedCommand command) {
        String tableName = command.getTableName();
        ensureTableExists(tableName);

        Table table = database.getTable(tableName);
        String condition = command.getParameters().isEmpty() ? null : command.getParameters().getFirst();

        // 1. Filter matching rows
        List<Row> matchedRows = new ArrayList<>();
        for (Row row : table.getRows()) {
            if (evaluator.evaluateCondition(condition, row, table)) {
                matchedRows.add(row);
            }
        }

        // 2. Full projection if arguments clause [...] is omitted
        List<String> projectionArgs = command.getArguments();
        if (projectionArgs.isEmpty()) {
            return new ExecutionResult(table.getColumns(), matchedRows);
        }

        // 3. Custom & computed column projection
        List<Column> projectedColumns = buildProjectedColumns(projectionArgs, table);
        List<Row> projectedRows = projectRows(matchedRows, projectionArgs, projectedColumns, table);

        return new ExecutionResult(projectedColumns, projectedRows);
    }

    private List<Column> buildProjectedColumns(List<String> projectionArgs, Table table) {
        List<Column> columns = new ArrayList<>();

        for (String rawArg : projectionArgs) {
            String expr = rawArg.trim();
            validateProjectionExpression(expr);

            if (table.hasColumn(expr.toLowerCase())) {
                // Direct column reference
                columns.add(table.getColumn(expr.toLowerCase()));
            } else {
                // Arithmetic computed expression
                DataType inferredType = inferExpressionType(expr, table);
                columns.add(new Column(expr, inferredType));
            }
        }

        return columns;
    }

    private List<Row> projectRows(List<Row> sourceRows,
                                  List<String> projectionArgs,
                                  List<Column> projectedColumns,
                                  Table table) {
        List<Row> result = new ArrayList<>();

        for (Row sourceRow : sourceRows) {
            Row projectedRow = new Row();
            for (int i = 0; i < projectionArgs.size(); i++) {
                String expr = projectionArgs.get(i).trim();
                Column targetCol = projectedColumns.get(i);
                Value evaluatedVal = evaluator.evaluateExpression(expr, sourceRow, table);
                projectedRow.set(targetCol.getName(), evaluatedVal);
            }
            result.add(projectedRow);
        }

        return result;
    }

    private void validateProjectionExpression(String expr) {
        if (expr.isEmpty()) {
            throw new ValidationException("Projected column expression cannot be empty");
        }
        if (expr.contains(">") || expr.contains("<") || expr.contains("=") || expr.contains("!")) {
            throw new ValidationException("Comparison operators (<, >, <=, >=, =, !=) are not permitted in column projections: " + expr);
        }
    }

    private DataType inferExpressionType(String expr, Table table) {
        boolean hasDouble = false;
        String[] tokens = expr.split("[+-]");

        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }

            if (table.hasColumn(token.toLowerCase())) {
                DataType colType = table.getColumn(token.toLowerCase()).getType();
                if (colType == DataType.DBL) {
                    hasDouble = true;
                } else if (colType != DataType.INT) {
                    throw new ValidationException("Arithmetic operations in projection require numeric columns: " + token);
                }
            } else if (token.contains(".")) {
                hasDouble = true;
            } else {
                try {
                    Long.parseLong(token);
                } catch (NumberFormatException e) {
                    throw new ValidationException("Invalid token in column projection expression: " + token);
                }
            }
        }

        return hasDouble ? DataType.DBL : DataType.INT;
    }

    private ExecutionResult executeSet(ParsedCommand command) {
        String tableName = command.getTableName();
        ensureTableExists(tableName);

        Table table = database.getTable(tableName);
        Map<String, String> variables = command.getVariables();
        if (variables.isEmpty()) {
            throw new ValidationException("Set command requires variable assignments inside {...}");
        }

        Map<String, Value> parsedUpdates = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String colName = entry.getKey().toLowerCase();
            if (!table.hasColumn(colName)) {
                throw new ValidationException("Column '" + colName + "' does not exist in table '" + tableName + "'");
            }
            Column col = table.getColumn(colName);
            parsedUpdates.put(colName, Value.of(col.getType(), entry.getValue()));
        }

        String condition = command.getParameters().isEmpty() ? null : command.getParameters().getFirst();
        int affectedCount = 0;

        for (Row row : table.getRows()) {
            if (evaluator.evaluateCondition(condition, row, table)) {
                parsedUpdates.forEach(row::set);
                affectedCount++;
            }
        }

        return new ExecutionResult(affectedCount);
    }

    private ExecutionResult executeDel(ParsedCommand command) {
        String tableName = command.getTableName();
        ensureTableExists(tableName);

        Table table = database.getTable(tableName);
        String condition = command.getParameters().isEmpty() ? null : command.getParameters().getFirst();

        List<Row> toDelete = new ArrayList<>();
        for (Row row : table.getRows()) {
            if (evaluator.evaluateCondition(condition, row, table)) {
                toDelete.add(row);
            }
        }

        table.removeRows(toDelete);
        return new ExecutionResult(toDelete.size());
    }

    private void ensureTableExists(String tableName) {
        if (!database.hasTable(tableName)) {
            throw new TableNotFoundException(tableName);
        }
    }

    public Database getDatabase() {
        return database;
    }
}