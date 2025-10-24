/*
 * Transaction.java - Immutable record for a banking transaction.
 */
package atm;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private final LocalDateTime timestamp;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;         // 0 for non-monetary events like LOGIN
    private final BigDecimal balanceAfter;   // balance after applying the tx (if any)
    private final String details;            // free text
    private final String counterparty;       // other account for transfers (can be null)

    public Transaction(LocalDateTime timestamp,
                       String accountNumber,
                       TransactionType type,
                       BigDecimal amount,
                       BigDecimal balanceAfter,
                       String details,
                       String counterparty) {
        this.timestamp = timestamp;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.details = details == null ? "" : details;
        this.counterparty = counterparty == null ? "" : counterparty;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDetails() { return details; }
    public String getCounterparty() { return counterparty; }

    public String toCsvLine() {
        // timestamp,account,type,amount,balanceAfter,details,counterparty
        String ts = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String amt = amount == null ? "" : amount.toPlainString();
        String bal = balanceAfter == null ? "" : balanceAfter.toPlainString();
        // Escape embedded commas by wrapping in quotes if needed
        String safeDetails = details.contains(",") ? "\"" + details.replace("\"", "\"\"") + "\"" : details;
        String safeCp = counterparty.contains(",") ? "\"" + counterparty.replace("\"", "\"\"") + "\"" : counterparty;
        return String.join(",", ts, accountNumber, type.name(), amt, bal, safeDetails, safeCp);
    }
}
