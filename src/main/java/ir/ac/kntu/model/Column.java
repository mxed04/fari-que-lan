package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;
import java.util.Objects;
import java.util.regex.Pattern;

public class Column {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final String name;
    private final DataType type;

    public Column(String name, DataType type) {
        if (name == null || !IDENTIFIER_PATTERN.matcher(name.trim()).matches()) {
            throw new ValidationException("نام ستون نامعتبر است: " + name);
        }
        this.name = name.trim().toLowerCase(); // ذخیره یکدست برای نادیده گرفتن حساسیت به حروف بزرگ/کوچک
        this.type = Objects.requireNonNull(type, "نوع ستون الزامی است");
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Column column = (Column) o;
        return Objects.equals(name, column.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " " + type.name().toLowerCase();
    }
}