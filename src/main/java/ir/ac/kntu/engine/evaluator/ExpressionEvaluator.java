package ir.ac.kntu.engine.evaluator;

import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates filter conditions and arithmetic expressions against database rows.
 */
public class ExpressionEvaluator {

    /**
     * Evaluates whether a given row satisfies the condition expression.
     *
     * @param condition raw condition string from parameters
     * @param row       target row
     * @param table     table schema metadata
     * @return true if matched, false otherwise
     */
    public boolean evaluateCondition(String condition, Row row, Table table) {
        if (condition == null || condition.isBlank()) {
            return true;
        }

        ComparisonOperator op = ComparisonOperator.extractFrom(condition);
        int opIdx = condition.indexOf(op.getSymbol());
        String leftExpr = condition.substring(0, opIdx).trim();
        String rightExpr = condition.substring(opIdx + op.getSymbol().length()).trim();

        if (leftExpr.isEmpty() || rightExpr.isEmpty()) {
            throw new ValidationException("Incomplete comparison condition: " + condition);
        }

        Value leftValue = evaluateExpression(leftExpr, row, table);
        Value rightValue = evaluateExpression(rightExpr, row, table);

        return op.apply(leftValue.compareTo(rightValue));
    }

    /**
     * Evaluates an arithmetic expression or literal value for a specific row.
     */
    public Value evaluateExpression(String expr, Row row, Table table) {
        String trimmed = expr.trim();

        // Detect if this is a literal string
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return Value.of(DataType.STR, trimmed);
        }

        // Check for arithmetic operators '+' and '-'
        List<String> tokens = tokenizeArithmetic(trimmed);
        if (tokens.size() == 1) {
            return resolveOperand(tokens.get(0), row, table);
        }

        // Arithmetic evaluation across numeric operands
        double result = 0.0;
        boolean hasDouble = false;
        char currentOp = '+';

        for (String token : tokens) {
            if (token.equals("+") || token.equals("-")) {
                currentOp = token.charAt(0);
            } else {
                Value operandValue = resolveOperand(token, row, table);
                if (operandValue.getType() != DataType.INT && operandValue.getType() != DataType.DBL) {
                    throw new ValidationException("Arithmetic operations are only valid for numeric types: " + token);
                }

                if (operandValue.getType() == DataType.DBL) {
                    hasDouble = true;
                }

                double val = operandValue.asDouble();
                if (currentOp == '+') {
                    result += val;
                } else {
                    result -= val;
                }
            }
        }

        if (hasDouble) {
            return new Value(DataType.DBL, result);
        } else {
            return new Value(DataType.INT, (long) result);
        }
    }

    private Value resolveOperand(String operand, Row row, Table table) {
        String token = operand.trim();
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
            return Value.of(DataType.STR, token);
        }

        // Column reference
        if (table.hasColumn(token)) {
            return row.get(token);
        }

        // Number literal: Integer or Double
        try {
            if (token.contains(".")) {
                return Value.of(DataType.DBL, token);
            } else {
                return Value.of(DataType.INT, token);
            }
        } catch (Exception e) {
            // Check if user queried an invalid/non-existent column name
            throw new ValidationException("Column '" + token + "' not found in table '" + table.getName() + "'");
        }
    }

    private List<String> tokenizeArithmetic(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean b = !current.toString().trim().isEmpty();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-') {
                if (b) {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }

        if (b) {
            tokens.add(current.toString().trim());
        }

        return tokens;
    }
}