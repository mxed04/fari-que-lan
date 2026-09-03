package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public enum DataType {
    INT,
    STR,
    DBL,
    TIME;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm[:ss]");

    public static DataType fromString(String typeStr) {
        return switch (typeStr.toLowerCase().trim()) {
            case "int" -> INT;
            case "str" -> STR;
            case "dbl" -> DBL;
            case "time" -> TIME;
            default -> throw new ValidationException("Invalid data type: " + typeStr);
        };
    }

    public Object parse(String rawValue) {
        if (rawValue == null) {
            return getDefaultValue();
        }
        String trimmed = rawValue.trim();

        // Remove bounding quotes for all input values (especially for STR and TIME types)
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        try {
            return switch (this) {
                case INT -> Long.parseLong(trimmed);
                case DBL -> Double.parseDouble(trimmed);
                case STR -> trimmed;
                case TIME -> LocalTime.parse(trimmed, TIME_FORMATTER);
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ValidationException("Value mismatch: '" + rawValue + "' is not compatible with type " + this.name());
        }
    }

    public Object getDefaultValue() {
        return switch (this) {
            case INT -> 0L;
            case DBL -> 0.0;
            case STR -> "";
            case TIME -> LocalTime.of(0, 0, 0);
        };
    }
}