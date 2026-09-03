package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;

/**
 * Represents a database table column schema or a computed projection column.
 */
public class Column {
    private final String name;
    private final DataType type;

    public Column(String name, DataType type) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Column name cannot be null or empty");
        }

        // Relaxed regex validation to support both standard identifiers
        // and computed arithmetic expressions (e.g., 'score + bonus', 'score - 1.0')
        if (!name.matches("^[a-zA-Z0-9_ +\\-.\"]+$")) {
            throw new ValidationException("Invalid column name: " + name);
        }

        this.name = name.trim();
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }
}