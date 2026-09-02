package ir.ac.kntu.parser;

import ir.ac.kntu.exception.SyntaxErrorException;

/**
 * Enumeration of supported FQL command types.
 */
public enum CommandType {
    CREATE,
    DROP,
    ADD,
    GET,
    SET,
    DEL,
    QUIT;

    /**
     * Resolves a case-insensitive string token to its corresponding CommandType.
     *
     * @param token the command string token
     * @return matching CommandType
     * @throws SyntaxErrorException if command is unrecognized
     */
    public static CommandType fromString(String token) {
        if (token == null || token.isBlank()) {
            throw new SyntaxErrorException("Command token cannot be empty");
        }
        try {
            return CommandType.valueOf(token.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SyntaxErrorException("Unknown command: '" + token.trim() + "'");
        }
    }
}