/*
 * InsufficientFundsException.java - Thrown when withdrawal/transfer exceeds balance.
 */
package atm.exceptions;

public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) { super(message); }
}
