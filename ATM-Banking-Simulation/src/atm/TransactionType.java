/*
 * TransactionType.java - Enum for transaction categories.
 */
package atm;

public enum TransactionType {
    LOGIN,
    BALANCE_INQUIRY,
    DEPOSIT,
    WITHDRAW,
    TRANSFER_IN,
    TRANSFER_OUT,
    ACCOUNT_CREATE,
    ACCOUNT_DELETE
}
