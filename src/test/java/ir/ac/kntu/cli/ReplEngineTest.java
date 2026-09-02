package ir.ac.kntu.cli;

import ir.ac.kntu.engine.DatabaseEngine;
import ir.ac.kntu.model.Database;
import ir.ac.kntu.parser.CommandParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplEngineTest {

    private String runReplWithInput(String inputScript) {
        ByteArrayInputStream in = new ByteArrayInputStream(inputScript.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out, true, StandardCharsets.UTF_8);

        Database database = new Database();
        DatabaseEngine engine = new DatabaseEngine(database);
        CommandParser parser = new CommandParser();

        ReplEngine repl = new ReplEngine(engine, parser, in, printStream);
        repl.start();

        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Should execute end-to-end scenario and exit gracefully on quit")
    void testEndToEndScriptExecution() {
        String script = String.join("\n",
                "create users[id int, username str, balance dbl]",
                "add users{id 1, username \"Fariborz\", balance 1500.5}",
                "add users{id 2, username \"Ali\", balance 200.0}",
                "get users(balance > 1000.0)",
                "set users(id = 2){balance 350.0}",
                "del users(id = 1)",
                "quit"
        );

        String output = runReplWithInput(script);

        // Verification of query outputs
        assertTrue(output.contains("Fariborz"));
        assertTrue(output.contains("1500.5"));
        assertTrue(output.contains("1")); // Affected count for SET
        assertTrue(output.contains("1")); // Affected count for DEL
    }

    @Test
    @DisplayName("Should handle unknown commands gracefully without terminating the session")
    void testUnknownCommandResilience() {
        String script = String.join("\n",
                "invalid_command something",
                "create items[id int]",
                "quit"
        );

        String output = runReplWithInput(script);

        assertTrue(output.contains("Error: Unknown command"));
    }

    @Test
    @DisplayName("Should report error on non-existent table operations and continue running")
    void testTableNotFoundErrorResilience() {
        String script = String.join("\n",
                "get missing_table",
                "quit"
        );

        String output = runReplWithInput(script);

        assertTrue(output.contains("Error: Table not found"));
        assertFalse(output.contains("Exception"));
    }
}