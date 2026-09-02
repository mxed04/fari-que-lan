package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {
    private Database db;

    @BeforeEach
    void setUp() {
        db = new Database();
    }

    @Test
    void testCreateTableAndDefaults() {
        List<Column> cols = List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STR),
                new Column("score", DataType.DBL)
        );
        Table studentTable = new Table("students", cols);
        db.createTable(studentTable);

        Row row = new Row();
        row.set("id", Value.of(DataType.INT, "101"));
        studentTable.addRow(row);

        Row savedRow = studentTable.getRows().get(0);
        assertEquals(101L, savedRow.get("id").getRaw());
        assertEquals("", savedRow.get("name").getRaw()); // مقدار پیش‌فرض رشته
        assertEquals(0.0, savedRow.get("score").getRaw()); // مقدار پیش‌فرض اعشاری
    }

    @Test
    void testDuplicateColumnThrowsException() {
        List<Column> cols = List.of(
                new Column("id", DataType.INT),
                new Column("id", DataType.STR)
        );
        assertThrows(ValidationException.class, () -> new Table("invalid", cols));
    }

    @Test
    void testInvalidTableIdentifierThrowsException() {
        List<Column> cols = List.of(new Column("id", DataType.INT));
        assertThrows(ValidationException.class, () -> new Table("students-table!", cols));
    }
}