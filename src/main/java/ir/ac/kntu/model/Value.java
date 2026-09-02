package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;
import java.time.LocalTime;
import java.util.Objects;

public class Value implements Comparable<Value> {
    private final DataType type;
    private final Object raw;

    public Value(DataType type, Object raw) {
        this.type = Objects.requireNonNull(type, "نوع مقدار نمی‌تواند null باشد");
        this.raw = raw == null ? type.getDefaultValue() : raw;
    }

    public static Value of(DataType type, String rawStr) {
        return new Value(type, type.parse(rawStr));
    }

    public static Value defaultValue(DataType type) {
        return new Value(type, type.getDefaultValue());
    }

    public DataType getType() {
        return type;
    }

    public Object getRaw() {
        return raw;
    }

    public double asDouble() {
        if (type == DataType.INT) {
            return ((Long) raw).doubleValue();
        } else if (type == DataType.DBL) {
            return (Double) raw;
        }
        throw new ValidationException("تبدیل نوع " + type + " به عدد امکان‌پذیر نیست");
    }

    @Override
    public int compareTo(Value other) {
        if (this.type == DataType.INT && other.type == DataType.INT) {
            return Long.compare((Long) this.raw, (Long) other.raw);
        }
        if ((this.type == DataType.INT || this.type == DataType.DBL) &&
                (other.type == DataType.INT || other.type == DataType.DBL)) {
            return Double.compare(this.asDouble(), other.asDouble());
        }
        if (this.type == DataType.STR && other.type == DataType.STR) {
            return ((String) this.raw).compareTo((String) other.raw);
        }
        if (this.type == DataType.TIME && other.type == DataType.TIME) {
            return ((LocalTime) this.raw).compareTo((LocalTime) other.raw);
        }
        throw new ValidationException("عدم تطابق نوع‌ها برای مقایسه: " + this.type + " با " + other.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value value = (Value) o;
        if ((this.type == DataType.INT || this.type == DataType.DBL) &&
                (value.type == DataType.INT || value.type == DataType.DBL)) {
            return Double.compare(this.asDouble(), value.asDouble()) == 0;
        }
        return type == value.type && Objects.equals(raw, value.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, raw);
    }

    @Override
    public String toString() {
        return String.valueOf(raw);
    }
}