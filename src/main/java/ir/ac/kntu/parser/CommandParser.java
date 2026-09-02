package ir.ac.kntu.parser;

import ir.ac.kntu.exception.SyntaxErrorException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw input query strings into structured ParsedCommand instances.
 */
public class CommandParser {
    private static final Pattern PAREN_PATTERN = Pattern.compile("\\((.*?)\\)");
    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\[(.*?)]");
    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{(.*?)}");
    private static final Pattern ANGLE_PATTERN = Pattern.compile("<(.*?)>");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    /**
     * Parses a single line of raw input into a ParsedCommand.
     *
     * @param rawInput raw line received from user or console
     * @return structured ParsedCommand object
     */
    public ParsedCommand parse(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            throw new SyntaxErrorException("Query input cannot be null or empty");
        }

        String input = rawInput.trim();

        // Handle termination command
        if (input.equalsIgnoreCase("quit")) {
            return new ParsedCommand(CommandType.QUIT, null, List.of(), List.of(), Map.of(), List.of());
        }

        // Extract and isolate bracketed clauses
        String paramsContent = extractMatch(PAREN_PATTERN, input);
        String argsContent = extractMatch(BRACKET_PATTERN, input);
        String varsContent = extractMatch(BRACE_PATTERN, input);
        String typesContent = extractMatch(ANGLE_PATTERN, input);

        // Remove matched clauses to leave only command keyword and table identifier
        String[] tokens = getStrings(input);

        CommandType commandType = CommandType.fromString(tokens[0]);
        String tableName = null;

        if (commandType != CommandType.QUIT) {
            if (tokens.length < 2) {
                throw new SyntaxErrorException("Table name is missing for command: " + commandType);
            }
            tableName = tokens[1].trim();
            if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
                throw new SyntaxErrorException("Invalid table identifier: " + tableName);
            }
            if (tokens.length > 2) {
                throw new SyntaxErrorException("Unexpected extra tokens in command header: " + tokens[2]);
            }
        }

        List<String> parameters = splitRespectingQuotes(paramsContent, ',');
        List<String> arguments = splitRespectingQuotes(argsContent, ',');
        List<String> types = splitRespectingQuotes(typesContent, ',');
        Map<String, String> variables = parseVariables(varsContent);

        return new ParsedCommand(commandType, tableName, parameters, arguments, variables, types);
    }

    private static String[] getStrings(String input) {
        String baseClause = input
                .replaceAll("\\(.*?\\)", " ")
                .replaceAll("\\[.*?]", " ")
                .replaceAll("\\{.*?}", " ")
                .replaceAll("<.*?>", " ")
                .trim();

        String[] tokens = baseClause.split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            throw new SyntaxErrorException("Missing command identifier");
        }
        return tokens;
    }

    private String extractMatch(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Splits comma-separated elements while preserving commas enclosed inside quotation marks.
     */
    public List<String> splitRespectingQuotes(String source, char delimiter) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                insideQuotes = !insideQuotes;
                current.append(c);
            } else if (c == delimiter && !insideQuotes) {
                String token = current.toString().trim();
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (insideQuotes) {
            throw new SyntaxErrorException("Unclosed string quotation in: " + source);
        }

        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            tokens.add(remaining);
        }

        return tokens;
    }

    /**
     * Parses variable assignments inside curly braces ({key value} or {key=value}).
     */
    private Map<String, String> parseVariables(String varsContent) {
        if (varsContent == null || varsContent.isBlank()) {
            return Map.of();
        }

        List<String> entries = splitRespectingQuotes(varsContent, ',');
        Map<String, String> variableMap = new LinkedHashMap<>();

        for (String entry : entries) {
            String trimmed = entry.trim();
            String key;
            String value;

            if (trimmed.contains("=")) {
                int eqIdx = trimmed.indexOf('=');
                key = trimmed.substring(0, eqIdx).trim();
                value = trimmed.substring(eqIdx + 1).trim();
            } else {
                // Split on first whitespace sequence
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length < 2) {
                    throw new SyntaxErrorException("Malformed variable assignment: '" + trimmed + "'");
                }
                key = parts[0].trim();
                value = parts[1].trim();
            }

            if (!IDENTIFIER_PATTERN.matcher(key).matches()) {
                throw new SyntaxErrorException("Invalid column name in variable assignment: " + key);
            }

            variableMap.put(key.toLowerCase(), value);
        }

        return variableMap;
    }
}