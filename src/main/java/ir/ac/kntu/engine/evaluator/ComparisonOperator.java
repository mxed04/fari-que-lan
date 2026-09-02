package ir.ac.kntu.engine.evaluator;

import ir.ac.kntu.exception.ValidationException;

/**
 * Enumeration of supported comparison operators in FQL filter conditions.
 */
public enum ComparisonOperator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN(">"),
    LESS_THAN("<");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * Evaluates a comparison result based on Comparable compareTo outcome.
     *
     * @param compareResult integer result from Value.compareTo
     * @return true if the operator condition is satisfied, false otherwise
     */
    public boolean apply(int compareResult) {
        return switch (this) {
            case EQUALS -> compareResult == 0;
            case NOT_EQUALS -> compareResult != 0;
            case GREATER_THAN -> compareResult > 0;
            case LESS_THAN -> compareResult < 0;
            case GREATER_THAN_OR_EQUAL -> compareResult >= 0;
            case LESS_THAN_OR_EQUAL -> compareResult <= 0;
        };
    }

    /**
     * Identifies the matching comparison operator inside an expression string.
     *
     * @param condition raw condition text
     * @return detected ComparisonOperator
     */
    public static ComparisonOperator extractFrom(String condition) {
        // Multi-character operators evaluated first to avoid prefix collisions
        if (condition.contains("!=")) return NOT_EQUALS;
        if (condition.contains(">=")) return GREATER_THAN_OR_EQUAL;
        if (condition.contains("<=")) return LESS_THAN_OR_EQUAL;
        if (condition.contains("=")) return EQUALS;
        if (condition.contains(">")) return GREATER_THAN;
        if (condition.contains("<")) return LESS_THAN;
        throw new ValidationException("Missing or unsupported comparison operator in: " + condition);
    }
}