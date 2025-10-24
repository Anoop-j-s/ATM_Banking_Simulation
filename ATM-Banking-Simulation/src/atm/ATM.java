/*
 * ATM.java - Thin application service layer used by the UI.
 */
package atm;

import atm.exceptions.AuthenticationException;
import atm.exceptions.InsufficientFundsException;
import atm.exceptions.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class ATM {
    private final Bank bank;

    public ATM(Bank bank) { this.bank = bank; }

    public User login(String accountNumber, String pin) throws AuthenticationException, IOException {
        return bank.authenticate(accountNumber, pin);
    }

    public String balance(User user) {
        return bank.balance(user.getAccount().getAccountNumber()).toPlainString();
    }

    public void deposit(User user, BigDecimal amount) throws ValidationException, IOException {
        bank.deposit(user.getAccount().getAccountNumber(), amount);
    }

    public void withdraw(User user, BigDecimal amount)
            throws ValidationException, InsufficientFundsException, IOException {
        bank.withdraw(user.getAccount().getAccountNumber(), amount);
    }

    public void transfer(User user, String toAccount, BigDecimal amount)
            throws ValidationException, InsufficientFundsException, IOException {
        bank.transfer(user.getAccount().getAccountNumber(), toAccount, amount);
    }

    public List<Transaction> lastN(User user, int n) throws IOException, ValidationException {
        return bank.lastN(user.getAccount().getAccountNumber(), n);
    }

    // Admin passthrough
    public Account adminCreate(String name, Account.Role role, BigDecimal initialBalance, String pin)
            throws ValidationException, IOException {
        return bank.createAccount(name, role, initialBalance, pin);
    }

    public void adminDelete(String accountNumber) throws ValidationException, IOException {
        bank.deleteAccount(accountNumber);
    }
}
