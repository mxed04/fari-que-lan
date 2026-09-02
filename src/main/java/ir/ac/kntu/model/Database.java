package ir.ac.kntu.model;

import ir.ac.kntu.exception.ValidationException;
import java.util.HashMap;
import java.util.Map;

public class Database {
    private final Map<String, Table> tables = new HashMap<>();

    public synchronized void createTable(Table table) {
        String tableName = table.getName().toLowerCase();
        if (tables.containsKey(tableName)) {
            throw new ValidationException("جدول با نام '" + tableName + "' قبلاً وجود دارد");
        }
        tables.put(tableName, table);
    }

    public synchronized void dropTable(String tableName) {
        String key = tableName.toLowerCase();
        if (!tables.containsKey(key)) {
            throw new ValidationException("جدول '" + tableName + "' برای حذف یافت نشد");
        }
        tables.remove(key);
    }

    public synchronized Table getTable(String tableName) {
        String key = tableName.toLowerCase();
        Table table = tables.get(key);
        if (table == null) {
            throw new ValidationException("جدول '" + tableName + "' وجود ندارد");
        }
        return table;
    }

    public synchronized boolean hasTable(String tableName) {
        return tables.containsKey(tableName.toLowerCase());
    }
}