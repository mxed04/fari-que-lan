package ir.ac.kntu.engine;

import ir.ac.kntu.cli.TableRenderer;
import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.Database;
import ir.ac.kntu.parser.CommandParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnProjectionTest {
    private DatabaseEngine engine;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine(new Database());
        parser = new CommandParser();

        engine.execute(parser.parse("create students[id int, name str, score dbl, bonus dbl]"));
        engine.execute(parser.parse("add students{id 1, name \"Ali\", score 17.5, bonus 2.0}"));
        engine.execute(parser.parse("add students{id 2, name \"Sara\", score 19.0, bonus 1.0}"));
        engine.execute(parser.parse("add students{id 3, name \"Reza\", score 14.0, bonus 0.5}"));
    }

    @Test
    @DisplayName("Should project only selected subset of table columns")
    void testSelectiveColumnProjection() {
        ExecutionResult result = engine.execute(parser.parse("get students [id, name]"));

        assertEquals(2, result.getColumns().size());
        assertEquals("id", result.getColumns().get(0).getName());
        assertEquals("name", result.getColumns().get(1).getName());
        assertEquals(3, result.getRows().size());
        assertEquals(1L, result.getRows().get(0).get("id").getRaw());
        assertEquals("Ali", result.getRows().get(0).get("name").getRaw());
    }

    @Test
    @DisplayName("Should evaluate arithmetic expressions as computed columns")
    void testArithmeticComputedColumnProjection() {
        // Project score + bonus along with name
        ExecutionResult result = engine.execute(parser.parse("get students (id = 1) [name, score + bonus]"));

        assertEquals(2, result.getColumns().size());
        assertEquals("name", result.getColumns().get(0).getName());
        assertEquals("score + bonus", result.getColumns().get(1).getName());

        assertEquals(1, result.getRows().size());
        assertEquals("Ali", result.getRows().get(0).get("name").getRaw());
        assertEquals(19.5, (Double) result.getRows().get(0).get("score + bonus").getRaw(), 1e-9);
    }

    @Test
    @DisplayName("Should render pretty table correctly with projected computed columns")
    void testPrettyTableRenderingWithComputedColumns() {
        ExecutionResult result = engine.execute(parser.parse("get students (score > 15.0) [name, score - 1.0]"));

        String tableStr = TableRenderer.render(result.getColumns(), result.getRows());
        assertTrue(tableStr.contains("score - 1.0"));
        assertTrue(tableStr.contains("Ali"));
        assertTrue(tableStr.contains("Sara"));
        assertFalse(tableStr.contains("Reza"));
    }

    @Test
    @DisplayName("Should throw ValidationException when comparison operators appear in column projections")
    void testComparisonOperatorsInProjectionThrowsException() {
        assertThrows(ValidationException.class, () ->
                engine.execute(parser.parse("get students [id, score > 15]"))
        );

        assertThrows(ValidationException.class, () ->
                engine.execute(parser.parse("get students [name, score >= bonus]"))
        );
    }

    @Test
    @DisplayName("Should throw ValidationException for invalid column references in projection")
    void testNonExistingColumnInProjectionThrowsException() {
        assertThrows(ValidationException.class, () ->
                engine.execute(parser.parse("get students [id, unknown_col]"))
        );
    }
}