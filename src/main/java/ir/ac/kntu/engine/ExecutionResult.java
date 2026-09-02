package ir.ac.kntu.engine;

import ir.ac.kntu.model.Column;
import ir.ac.kntu.model.Row;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the execution output of an FQL command.
 */
public class ExecutionResult {
    private final List<Column> columns;
    private final List<Row> rows;
    private final int affectedRows;
    private final boolean emptyResult;

    public ExecutionResult() {
        this.columns = List.of();
        this.rows = List.of();
        this.affectedRows = 0;
        this.emptyResult = true;
    }

    public ExecutionResult(List<Column> columns, List<Row> rows) {
        this.columns = columns != null ? Collections.unmodifiableList(columns) : List.of();
        this.rows = rows != null ? Collections.unmodifiableList(rows) : List.of();
        this.affectedRows = this.rows.size();
        this.emptyResult = false;
    }

    public ExecutionResult(int affectedRows) {
        this.columns = List.of();
        this.rows = List.of();
        this.affectedRows = affectedRows;
        this.emptyResult = false;
    }

    public static ExecutionResult empty() {
        return new ExecutionResult();
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public boolean isEmptyResult() {
        return emptyResult;
    }
}