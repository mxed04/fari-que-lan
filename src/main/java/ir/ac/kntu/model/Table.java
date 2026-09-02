package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;
import java.util.*;
import java.util.regex.Pattern;

public class Table {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final String name;
    private final List<Column> columns = new ArrayList<>();
    private final Map<String, Column> columnMap = new LinkedHashMap<>();
    private final List<Row> rows = new ArrayList<>();

    public Table(String name, List<Column> columns) {
        if (name == null || !IDENTIFIER_PATTERN.matcher(name.trim()).matches()) {
            throw new ValidationException("نام جدول نامعتبر است: " + name);
        }
        if (columns == null || columns.isEmpty()) {
            throw new ValidationException("جدول باید حداقل دارای یک ستون باشد");
        }

        this.name = name.trim().toLowerCase();

        for (Column col : columns) {
            String colName = col.getName();
            if (columnMap.containsKey(colName)) {
                throw new ValidationException("ستون تکراری مجاز نیست: " + colName);
            }
            this.columns.add(col);
            this.columnMap.put(colName, col);
        }
    }

    public String getName() {
        return name;
    }

    public List<Column> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public boolean hasColumn(String colName) {
        return columnMap.containsKey(colName.toLowerCase());
    }

    public Column getColumn(String colName) {
        Column col = columnMap.get(colName.toLowerCase());
        if (col == null) {
            throw new ValidationException("ستون '" + colName + "' در جدول '" + name + "' یافت نشد");
        }
        return col;
    }

    public void addRow(Row row) {
        // پر کردن فیلدهای ذکرنشده با مقدار پیش‌فرض
        Row completeRow = new Row();
        for (Column col : columns) {
            String colName = col.getName();
            if (row.contains(colName)) {
                Value val = row.get(colName);
                if (val.getType() != col.getType()) {
                    throw new ValidationException("عدم تطابق نوع داده برای ستون " + colName);
                }
                completeRow.set(colName, val);
            } else {
                completeRow.set(colName, Value.defaultValue(col.getType()));
            }
        }
        this.rows.add(completeRow);
    }

    public List<Row> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public void removeRows(List<Row> rowsToRemove) {
        this.rows.removeAll(rowsToRemove);
    }
}