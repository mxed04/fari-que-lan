package ir.ac.kntu.cli;

import ir.ac.kntu.model.Column;
import ir.ac.kntu.model.DataType;
import ir.ac.kntu.model.Row;
import ir.ac.kntu.model.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableRendererTest {

    @Test
    @DisplayName("Should format table with proper padding and divider lines")
    void testRenderAlignedTable() {
        List<Column> columns = List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STR),
                new Column("score", DataType.DBL)
        );

        Row row1 = new Row();
        row1.set("id", Value.of(DataType.INT, "1"));
        row1.set("name", Value.of(DataType.STR, "\"Fariborz\""));
        row1.set("score", Value.of(DataType.DBL, "19.75"));

        Row row2 = new Row();
        row2.set("id", Value.of(DataType.INT, "102"));
        row2.set("name", Value.of(DataType.STR, "\"Ali\""));
        row2.set("score", Value.of(DataType.DBL, "20.0"));

        String output = TableRenderer.render(columns, List.of(row1, row2));

        String[] lines = output.split("\n");
        assertEquals(4, lines.length);

        // Verify headers and separator presence
        assertTrue(lines[0].contains("id"));
        assertTrue(lines[0].contains("name"));
        assertTrue(lines[0].contains("score"));
        assertTrue(lines[1].contains("+"));
        assertTrue(lines[1].contains("-"));

        // Verify content rows
        assertTrue(lines[2].contains("Fariborz"));
        assertTrue(lines[3].contains("102"));
    }

    @Test
    @DisplayName("Should return empty string for empty columns list")
    void testRenderEmptyColumns() {
        String output = TableRenderer.render(List.of(), List.of());
        assertEquals("", output);
    }
}