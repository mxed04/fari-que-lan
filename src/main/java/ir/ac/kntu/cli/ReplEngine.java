package ir.ac.kntu.cli;

import ir.ac.kntu.engine.DatabaseEngine;
import ir.ac.kntu.engine.ExecutionResult;
import ir.ac.kntu.exception.FqlException;
import ir.ac.kntu.model.Column;
import ir.ac.kntu.model.Row;
import ir.ac.kntu.parser.CommandParser;
import ir.ac.kntu.parser.CommandType;
import ir.ac.kntu.parser.ParsedCommand;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Interactive Read-Eval-Print Loop (REPL) engine managing terminal interactions,
 * error boundaries, and standard command output formatting.
 */
public class ReplEngine {
    private final DatabaseEngine databaseEngine;
    private final CommandParser commandParser;
    private final InputStream in;
    private final PrintStream out;

    public ReplEngine(DatabaseEngine databaseEngine, CommandParser commandParser) {
        this(databaseEngine, commandParser, System.in, System.out);
    }

    public ReplEngine(DatabaseEngine databaseEngine, CommandParser commandParser, InputStream in, PrintStream out) {
        this.databaseEngine = Objects.requireNonNull(databaseEngine, "DatabaseEngine cannot be null");
        this.commandParser = Objects.requireNonNull(commandParser, "CommandParser cannot be null");
        this.in = Objects.requireNonNull(in, "InputStream cannot be null");
        this.out = Objects.requireNonNull(out, "PrintStream cannot be null");
    }

    /**
     * Starts the interactive command-reading loop.
     * Keeps running until 'quit' is encountered or input stream ends.
     */
    public void start() {
        Scanner scanner = new Scanner(in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                ParsedCommand command = commandParser.parse(line);
                if (command.getType() == CommandType.QUIT) {
                    break;
                }

                ExecutionResult result = databaseEngine.execute(command);
                renderResult(command.getType(), result);
            } catch (FqlException e) {
                // Catch domain and syntax exceptions without terminating the process
                out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                // Catch any unexpected runtime exception safely
                out.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    /**
     * Renders execution outputs according to command type specifications.
     */
    private void renderResult(CommandType commandType, ExecutionResult result) {
        if (result.isEmptyResult()) {
            return; // CREATE and DROP execute silently without output
        }

        if (commandType == CommandType.SET || commandType == CommandType.DEL) {
            out.println(result.getAffectedRows());
            return;
        }

        // Render table records for ADD and GET queries
        List<Column> columns = result.getColumns();
        List<Row> rows = result.getRows();

        if (columns.isEmpty()) {
            return;
        }

        // Print header
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            header.append(columns.get(i).getName());
            if (i < columns.size() - 1) {
                header.append(" | ");
            }
        }
        out.println(header);

        // Print row values
        for (Row row : rows) {
            StringBuilder rowLine = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).getName();
                rowLine.append(row.get(colName).toString());
                if (i < columns.size() - 1) {
                    rowLine.append(" | ");
                }
            }
            out.println(rowLine);
        }
    }
}