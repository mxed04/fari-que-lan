package ir.ac.kntu.engine.evaluator;

import ir.ac.kntu.exception.ValidationException;
import ir.ac.kntu.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates filter conditions and arithmetic expressions against database rows.
 */
public class ExpressionEvaluator {

    /**
     * Evaluates whether a given row satisfies the condition expression.
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

        // Robust regex-based tokenization for arithmetic expressions
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("([+-])|([^+-]+)").matcher(trimmed);
        while (m.find()) {
            String token = m.group().trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }

        if (tokens.isEmpty()) {
            return Value.of(DataType.INT, "0");
        }

        // Single operand or column reference
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

        return hasDouble ? new Value(DataType.DBL, result) : new Value(DataType.INT, (long) result);
    }

    /**
     * Resolves a token to a column value or a raw numeric/string literal.
     */
    private Value resolveOperand(String operand, Row row, Table table) {
        String token = operand.trim();
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
            return Value.of(DataType.STR, token);
        }

        // Case-insensitive column reference
        String lowerToken = token.toLowerCase();
        if (table.hasColumn(lowerToken)) {
            return row.get(lowerToken);
        }

        // Number literal: Integer or Double
        try {
            if (token.contains(".")) {
                return Value.of(DataType.DBL, token);
            } else {
                return Value.of(DataType.INT, token);
            }
        } catch (Exception e) {
            throw new ValidationException("Column '" + token + "' not found in table '" + table.getName() + "'");
        }
    }
}