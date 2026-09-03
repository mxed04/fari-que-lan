package ir.ac.kntu.cli;

import ir.ac.kntu.model.Column;
import ir.ac.kntu.model.DataType;
import ir.ac.kntu.model.Row;
import ir.ac.kntu.model.Value;

import java.util.List;

/**
 * Utility class rendering tabular query results into clean, aligned ASCII tables
 * matching standard RDBMS and FQL project visual specifications.
 */
public final class TableRenderer {

    private TableRenderer() {
        // Utility class; prevent direct instantiation
    }

    /**
     * Renders columns and records into an aligned table string matching FQL specifications.
     *
     * @param columns list of table column definitions
     * @param rows    list of data rows
     * @return formatted multi-line ASCII table string
     */
    public static String render(List<Column> columns, List<Row> rows) {
        if (columns == null || columns.isEmpty()) {
            return "";
        }

        int numCols = columns.size();
        int[] colWidths = new int[numCols];

        // 1. Calculate maximum column width considering headers and formatted values
        for (int i = 0; i < numCols; i++) {
            colWidths[i] = columns.get(i).getName().length();
        }

        if (rows != null) {
            for (Row row : rows) {
                for (int i = 0; i < numCols; i++) {
                    String colName = columns.get(i).getName();
                    String cellStr = formatCell(row.get(colName));
                    if (cellStr.length() > colWidths[i]) {
                        colWidths[i] = cellStr.length();
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        // 2. Render column headers (centered if column width exceeds header title)
        for (int i = 0; i < numCols; i++) {
            sb.append(" ").append(padCenter(columns.get(i).getName(), colWidths[i])).append(" ");
            if (i < numCols - 1) {
                sb.append("|");
            }
        }
        sb.append("\n");

        // 3. Render separator line with dashes and plus intersection markers
        for (int i = 0; i < numCols; i++) {
            sb.append("-".repeat(colWidths[i] + 2));
            if (i < numCols - 1) {
                sb.append("+");
            }
        }
        sb.append("\n");

        // 4. Render row data (numeric types right-aligned, text/time left-aligned)
        if (rows != null) {
            for (Row row : rows) {
                for (int i = 0; i < numCols; i++) {
                    Column col = columns.get(i);
                    String cellStr = formatCell(row.get(col.getName()));
                    String formattedCell = isNumeric(col.getType())
                            ? padLeft(cellStr, colWidths[i])
                            : padRight(cellStr, colWidths[i]);

                    sb.append(" ").append(formattedCell).append(" ");
                    if (i < numCols - 1) {
                        sb.append("|");
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString().stripTrailing();
    }

    private static boolean isNumeric(DataType type) {
        return type == DataType.INT || type == DataType.DBL;
    }

    private static String formatCell(Value value) {
        if (value == null || value.getRaw() == null) {
            return "null";
        }
        String str = value.toString();
        // Strip wrapping quotes for presentation if present
        if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    private static String padLeft(String text, int width) {
        return " ".repeat(Math.max(0, width - text.length())) + text;
    }

    private static String padCenter(String text, int width) {
        int totalPadding = Math.max(0, width - text.length());
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
}