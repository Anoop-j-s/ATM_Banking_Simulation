/*
 * Account.java - Bank account entity using BigDecimal for money.
 */
package atm;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Account {
    public enum Role { USER, ADMIN }

    private final String accountNumber;
    private String name;
    private Role role;
    private BigDecimal balance;
    private String pinHash;
    private String salt;
    private boolean active;

    public Account(String accountNumber, String name, Role role,
                   BigDecimal balance, String pinHash, String salt, boolean active) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.role = role;
        this.balance = normalize(balance);
        this.pinHash = pinHash;
        this.salt = salt;
        this.active = active;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public BigDecimal getBalance() { return balance; }
    public String getPinHash() { return pinHash; }
    public String getSalt() { return salt; }
    public boolean isActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setRole(Role role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void setPin(String pinHash, String salt) { this.pinHash = pinHash; this.salt = salt; }

    public void deposit(BigDecimal amount) {
        balance = balance.add(normalize(amount));
    }

    public void withdraw(BigDecimal amount) {
        balance = balance.subtract(normalize(amount));
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
