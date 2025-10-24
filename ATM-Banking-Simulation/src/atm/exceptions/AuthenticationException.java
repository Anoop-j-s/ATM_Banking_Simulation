/*
 * AuthenticationException.java - Thrown when login fails.
 */
package atm.exceptions;

public class AuthenticationException extends Exception {
    public AuthenticationException(String message) { super(message); }
}
