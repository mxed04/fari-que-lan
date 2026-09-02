package ir.ac.kntu.parser;

import java.util.*;

/**
 * Data Transfer Object encapsulating parsed sections of an FQL command.
 */
public class ParsedCommand {
    private final CommandType type;
    private final String tableName;
    private final List<String> parameters; // Content from (...)
    private final List<String> arguments;  // Content from [...]
    private final Map<String, String> variables; // Key-value content from {...}
    private final List<String> types;      // Content from <...>

    public ParsedCommand(CommandType type,
                         String tableName,
                         List<String> parameters,
                         List<String> arguments,
                         Map<String, String> variables,
                         List<String> types) {
        this.type = Objects.requireNonNull(type, "Command type is mandatory");
        this.tableName = tableName != null ? tableName.trim().toLowerCase() : null;
        this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : List.of();
        this.arguments = arguments != null ? Collections.unmodifiableList(arguments) : List.of();
        this.variables = variables != null ? Collections.unmodifiableMap(variables) : Map.of();
        this.types = types != null ? Collections.unmodifiableList(types) : List.of();
    }

    public CommandType getType() {
        return type;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public List<String> getTypes() {
        return types;
    }

    @Override
    public String toString() {
        return "ParsedCommand{" +
                "type=" + type +
                ", tableName='" + tableName + '\'' +
                ", parameters=" + parameters +
                ", arguments=" + arguments +
                ", variables=" + variables +
                ", types=" + types +
                '}';
    }
}