/*
 * ValidationException.java - Thrown on invalid input or business rule violation.
 */
package atm.exceptions;

public class ValidationException extends Exception {
    public ValidationException(String message) { super(message); }
}
