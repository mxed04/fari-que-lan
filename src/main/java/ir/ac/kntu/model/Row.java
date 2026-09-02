package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Row {
    private final Map<String, Value> values = new LinkedHashMap<>();

    public Row() {}

    public Row(Map<String, Value> initialValues) {
        if (initialValues != null) {
            initialValues.forEach((k, v) -> values.put(k.toLowerCase(), v));
        }
    }

    public void set(String columnName, Value value) {
        values.put(columnName.toLowerCase(), value);
    }

    public Value get(String columnName) {
        String key = columnName.toLowerCase();
        if (!values.containsKey(key)) {
            throw new ValidationException("ستون '" + columnName + "' در این رکورد وجود ندارد");
        }
        return values.get(key);
    }

    public boolean contains(String columnName) {
        return values.containsKey(columnName.toLowerCase());
    }

    public Map<String, Value> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public Row copy() {
        return new Row(new LinkedHashMap<>(this.values));
    }

    @Override
    public String toString() {
        return values.toString();
    }
}