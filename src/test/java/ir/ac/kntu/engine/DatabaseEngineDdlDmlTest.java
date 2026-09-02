package ir.ac.kntu.engine;

import ir.ac.kntu.exception.TableAlreadyExistsException;
import ir.ac.kntu.exception.TableNotFoundException;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.DataType;
import ir.ac.kntu.model.Database;
import ir.ac.kntu.model.Row;
import ir.ac.kntu.parser.CommandParser;
import ir.ac.kntu.parser.ParsedCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying DDL (CREATE, DROP) and basic DML (ADD) engine operations.
 */
class DatabaseEngineDdlDmlTest {
    private Database database;
    private DatabaseEngine engine;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        database = new Database();
        engine = new DatabaseEngine(database);
        parser = new CommandParser();
    }

    @Test
    @DisplayName("Should successfully create and drop a table")
    void testCreateAndDropTable() {
        ParsedCommand createCmd = parser.parse("create students[id int, name str, grade dbl]");
        ExecutionResult createResult = engine.execute(createCmd);

        assertTrue(createResult.isEmptyResult());
        assertTrue(database.hasTable("students"));
        assertEquals(3, database.getTable("students").getColumns().size());

        ParsedCommand dropCmd = parser.parse("drop students");
        ExecutionResult dropResult = engine.execute(dropCmd);

        assertTrue(dropResult.isEmptyResult());
        assertFalse(database.hasTable("students"));
    }

    @Test
    @DisplayName("Should throw TableAlreadyExistsException when creating a duplicate table")
    void testCreateDuplicateTableThrowsException() {
        ParsedCommand createCmd = parser.parse("create students[id int]");
        engine.execute(createCmd);

        assertThrows(TableAlreadyExistsException.class, () -> engine.execute(createCmd));
    }

    @Test
    @DisplayName("Should throw TableNotFoundException when dropping a non-existent table")
    void testDropNonExistingTableThrowsException() {
        ParsedCommand dropCmd = parser.parse("drop non_existing");
        assertThrows(TableNotFoundException.class, () -> engine.execute(dropCmd));
    }

    @Test
    @DisplayName("Should throw ValidationException when table column list is empty")
    void testCreateWithEmptyColumnsThrowsException() {
        ParsedCommand emptyColCmd = parser.parse("create empty_table[]");
        assertThrows(ValidationException.class, () -> engine.execute(emptyColCmd));
    }

    @Test
    @DisplayName("Should throw ValidationException when duplicate column names are defined in schema")
    void testCreateWithDuplicateColumnsThrowsException() {
        ParsedCommand duplicateColCmd = parser.parse("create invalid_table[id int, name str, id dbl]");
        assertThrows(ValidationException.class, () -> engine.execute(duplicateColCmd));
    }

    @Test
    @DisplayName("Should throw ValidationException when creating table with unsupported data type")
    void testCreateWithInvalidDataTypeThrowsException() {
        ParsedCommand invalidTypeCmd = parser.parse("create invalid_table[id int, active boolean]");
        assertThrows(ValidationException.class, () -> engine.execute(invalidTypeCmd));
    }

    @Test
    @DisplayName("Should populate default values for unspecified columns when adding a row")
    void testAddRowWithDefaultsAndValidation() {
        engine.execute(parser.parse("create students[id int, name str, grade dbl, approved int, registered_at time]"));

        ParsedCommand addCmd = parser.parse("add students { id 101, name \"Ali Reza\" }");
        ExecutionResult result = engine.execute(addCmd);

        assertFalse(result.isEmptyResult());
        assertEquals(1, result.getRows().size());

        Row row = result.getRows().get(0);
        assertAll(
                () -> assertEquals(101L, row.get("id").getRaw()),
                () -> assertEquals("Ali Reza", row.get("name").getRaw()),
                () -> assertEquals(0.0, (Double) row.get("grade").getRaw(), 1e-9), // Default double is 0.0
                () -> assertEquals(0L, row.get("approved").getRaw()),              // Default int is 0
                () -> assertEquals(LocalTime.of(0, 0, 0), row.get("registered_at").getRaw()) // Default time
        );
    }

    @Test
    @DisplayName("Should throw ValidationException when targeting a non-existent column during insertion")
    void testAddRowToNonExistingColumnThrowsException() {
        engine.execute(parser.parse("create students[id int]"));
        ParsedCommand addCmd = parser.parse("add students { age 20 }");

        assertThrows(ValidationException.class, () -> engine.execute(addCmd));
    }

    @Test
    @DisplayName("Should throw ValidationException when row value type does not match column type")
    void testAddRowWithIncompatibleTypeThrowsException() {
        engine.execute(parser.parse("create students[id int]"));
        ParsedCommand addCmd = parser.parse("add students { id \"NotAnInt\" }");

        assertThrows(ValidationException.class, () -> engine.execute(addCmd));
    }

    @Test
    @DisplayName("Should throw TableNotFoundException when adding row to a non-existent table")
    void testAddRowToNonExistingTableThrowsException() {
        ParsedCommand addCmd = parser.parse("add non_existent_table { id 1 }");
        assertThrows(TableNotFoundException.class, () -> engine.execute(addCmd));
    }

    @Test
    @DisplayName("Should correctly handle case insensitivity and arbitrary whitespaces")
    void testCaseInsensitivityAndArbitraryWhitespace() {
        // Mixed casing in keywords and identifiers with irregular spacing
        engine.execute(parser.parse("   CREATE   Members  [  Id   INT ,   NAME   STR  ]  "));

        ParsedCommand addCmd = parser.parse("  ADD   members   {   iD   505  ,   name   \"Mohammad\"   }  ");
        ExecutionResult addResult = engine.execute(addCmd);

        assertFalse(addResult.isEmptyResult());
        Row inserted = addResult.getRows().get(0);
        assertEquals(505L, inserted.get("id").getRaw());
        assertEquals("Mohammad", inserted.get("name").getRaw());

        // Table lookup must be case-insensitive
        assertTrue(database.hasTable("MEMBERS"));
        assertTrue(database.getTable("members").hasColumn("ID"));
    }

    @Test
    @DisplayName("Should properly parse and store string literals with commas and special characters")
    void testAddRowWithStringContainingDelimiters() {
        engine.execute(parser.parse("create logs[id int, message str]"));

        ParsedCommand addCmd = parser.parse("add logs { id 1, message \"Error: division by zero, check input!\" }");
        ExecutionResult result = engine.execute(addCmd);

        Row row = result.getRows().get(0);
        assertEquals("Error: division by zero, check input!", row.get("message").getRaw());
    }

    @Test
    @DisplayName("Should support sequential row additions and maintain insertion order")
    void testMultipleRowInsertions() {
        engine.execute(parser.parse("create scores[id int, score dbl]"));

        engine.execute(parser.parse("add scores { id 1, score 19.5 }"));
        engine.execute(parser.parse("add scores { id 2, score 17.0 }"));
        ExecutionResult thirdAdd = engine.execute(parser.parse("add scores { id 3, score 20.0 }"));

        assertEquals(1, thirdAdd.getRows().size());
        assertEquals(3L, thirdAdd.getRows().get(0).get("id").getRaw());

        List<Row> allRows = database.getTable("scores").getRows();
        assertEquals(3, allRows.size());
        assertEquals(1L, allRows.get(0).get("id").getRaw());
        assertEquals(2L, allRows.get(1).get("id").getRaw());
        assertEquals(3L, allRows.get(2).get("id").getRaw());
    }
}