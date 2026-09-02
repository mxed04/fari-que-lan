package ir.ac.kntu.exception;

/**
 * Thrown when an input query violates FQL grammatical or structural syntax.
 */
public class SyntaxErrorException extends FqlException {
    public SyntaxErrorException(String message) {
        super(message);
    }
}