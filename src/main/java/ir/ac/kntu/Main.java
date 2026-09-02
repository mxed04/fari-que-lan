package ir.ac.kntu;

import ir.ac.kntu.cli.ReplEngine;
import ir.ac.kntu.engine.DatabaseEngine;
import ir.ac.kntu.model.Database;
import ir.ac.kntu.parser.CommandParser;

/**
 * Main application entry point bootstrapping the FQL engine and REPL session.
 */
public class Main {
    public static void main(String[] args) {
        Database database = new Database();
        DatabaseEngine engine = new DatabaseEngine(database);
        CommandParser parser = new CommandParser();

        ReplEngine replEngine = new ReplEngine(engine, parser);
        replEngine.start();
    }
}