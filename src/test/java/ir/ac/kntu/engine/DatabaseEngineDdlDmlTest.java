package ir.ac.kntu.engine;

import ir.ac.kntu.exception.TableAlreadyExistsException;
import ir.ac.kntu.exception.TableNotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.Database;
import ir.ac.kntu.parser.CommandParser;
import ir.ac.kntu.parser.ParsedCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseEngineDdlDmlTest {
    private DatabaseEngine engine;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine(new Database());
        parser = new CommandParser();
    }

    @Test
    void testCreateAndDropTable() {
        ParsedCommand createCmd = parser.parse("create students[id int, name str, grade dbl]");
        ExecutionResult createResult = engine.execute(createCmd);
        assertTrue(createResult.isEmptyResult());
        assertTrue(engine.getDatabase().hasTable("students"));

        ParsedCommand dropCmd = parser.parse("drop students");
        ExecutionResult dropResult = engine.execute(dropCmd);
        assertTrue(dropResult.isEmptyResult());
        assertFalse(engine.getDatabase().hasTable("students"));
    }

    @Test
    void testCreateDuplicateTableThrowsException() {
        ParsedCommand createCmd = parser.parse("create students[id int]");
        engine.execute(createCmd);

        assertThrows(TableAlreadyExistsException.class, () -> engine.execute(createCmd));
    }

    @Test
    void testDropNonExistingTableThrowsException() {
        ParsedCommand dropCmd = parser.parse("drop non_existing");
        assertThrows(TableNotFoundException.class, () -> engine.execute(dropCmd));
    }

    @Test
    void testAddRowWithDefaultsAndValidation() {
        engine.execute(parser.parse("create students[id int, name str, grade dbl, approved int]"));

        ParsedCommand addCmd = parser.parse("add students { id 101, name \"Ali Reza\" }");
        ExecutionResult result = engine.execute(addCmd);

        assertFalse(result.isEmptyResult());
        assertEquals(1, result.getRows().size());
        assertEquals(101L, result.getRows().get(0).get("id").getRaw());
        assertEquals("Ali Reza", result.getRows().get(0).get("name").getRaw());
        assertEquals(0.0, result.getRows().get(0).get("grade").getRaw()); // Default dbl
        assertEquals(0L, result.getRows().get(0).get("approved").getRaw()); // Default int
    }

    @Test
    void testAddRowToNonExistingColumnThrowsException() {
        engine.execute(parser.parse("create students[id int]"));
        ParsedCommand addCmd = parser.parse("add students { age 20 }");

        assertThrows(ValidationException.class, () -> engine.execute(addCmd));
    }

    @Test
    void testAddRowWithIncompatibleTypeThrowsException() {
        engine.execute(parser.parse("create students[id int]"));
        ParsedCommand addCmd = parser.parse("add students { id \"NotAnInt\" }");

        assertThrows(ValidationException.class, () -> engine.execute(addCmd));
    }
}