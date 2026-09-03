package ir.ac.kntu.engine;

import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.Database;
import ir.ac.kntu.parser.CommandParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseEngineGroupByTest {
    private DatabaseEngine engine;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine(new Database());
        parser = new CommandParser();

        engine.execute(parser.parse("create employees[id int, dept str, salary dbl]"));
        engine.execute(parser.parse("add employees{id 1, dept \"IT\", salary 2000.0}"));
        engine.execute(parser.parse("add employees{id 2, dept \"HR\", salary 1500.0}"));
        engine.execute(parser.parse("add employees{id 3, dept \"IT\", salary 2500.0}"));
        engine.execute(parser.parse("add employees{id 4, dept \"IT\", salary 2200.0}"));
        engine.execute(parser.parse("add employees{id 5, dept \"Sales\", salary 1800.0}"));
    }

    @Test
    @DisplayName("Should group rows by column and return count aggregation")
    void testGroupByDepartment() {
        ExecutionResult result = engine.execute(parser.parse("get employees <dept>"));

        assertEquals(2, result.getColumns().size());
        assertEquals("dept", result.getColumns().get(0).getName());
        assertEquals("count", result.getColumns().get(1).getName());

        assertEquals(3, result.getRows().size()); // IT, HR, Sales

        // Verify IT group count (3 members)
        assertEquals("IT", result.getRows().get(0).get("dept").getRaw());
        assertEquals(3L, result.getRows().get(0).get("count").getRaw());

        // Verify HR group count (1 member)
        assertEquals("HR", result.getRows().get(1).get("dept").getRaw());
        assertEquals(1L, result.getRows().get(1).get("count").getRaw());
    }

    @Test
    @DisplayName("Should apply filter conditions before grouping")
    void testFilteredGroupBy() {
        // Group by dept ONLY for salary > 1800
        ExecutionResult result = engine.execute(parser.parse("get employees (salary > 1800.0) <dept>"));

        assertEquals(1, result.getRows().size()); // Only IT meets the criteria
        assertEquals("IT", result.getRows().get(0).get("dept").getRaw());
        assertEquals(3L, result.getRows().get(0).get("count").getRaw()); // 3 IT employees have > 1800
    }

    @Test
    @DisplayName("Should throw ValidationException for non-existing group by column")
    void testInvalidGroupByColumnThrowsException() {
        assertThrows(ValidationException.class, () ->
                engine.execute(parser.parse("get employees <location>"))
        );
    }
}