/*
 * Bank.java - Core domain service: authentication and money operations.
 */
package atm;

import atm.exceptions.AuthenticationException;
import atm.exceptions.InsufficientFundsException;
import atm.exceptions.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class Bank {
    private final java.util.Map<String, Account> accounts = new java.util.HashMap<>();
    private final DataStore store;
    private final LockManager lockManager = new LockManager();
    private final Logger logger;

    public Bank(DataStore store, Logger logger) throws IOException {
        this.store = store;
        this.logger = logger;
        for (Account a : store.loadAccounts()) {
            accounts.put(a.getAccountNumber(), a);
        }
    }

    public synchronized java.util.List<Account> snapshotAccounts() {
        return new java.util.ArrayList<>(accounts.values());
    }

    public User authenticate(String accountNumber, String pin) throws AuthenticationException, IOException {
        Account acc;
        synchronized (this) {
            acc = accounts.get(accountNumber);
        }
        if (acc == null || !acc.isActive()) {
            logger.warning("Auth failed: unknown or inactive account " + accountNumber);
            throw new AuthenticationException("Invalid credentials.");
        }
        String expected = SecurityUtil.hashPin(pin, acc.getSalt());
        if (!expected.equals(acc.getPinHash())) {
            logger.warning("Auth failed: wrong PIN for " + accountNumber);
            throw new AuthenticationException("Invalid credentials.");
        }
        // record login transaction (amount 0)
        Transaction tx = new Transaction(LocalDateTime.now(), accountNumber, TransactionType.LOGIN,
                BigDecimal.ZERO, acc.getBalance(), "Successful login", "");
        store.appendTransaction(tx);
        logger.info("Login success for " + accountNumber);
        return new User(acc);
    }

    public BigDecimal balance(String accountNumber) {
        synchronized (this) {
            return accounts.get(accountNumber).getBalance();
        }
    }

    public void deposit(String accountNumber, BigDecimal amount) throws ValidationException, IOException {
        checkAmount(amount);
        ReentrantLock lock = lockManager.lockFor(accountNumber);
        lock.lock();
        try {
            Account acc = accounts.get(accountNumber);
            acc.deposit(amount);
            store.saveAccounts(snapshotAccounts());
            store.appendTransaction(new Transaction(LocalDateTime.now(), accountNumber,
                    TransactionType.DEPOSIT, amount, acc.getBalance(), "Cash deposit", ""));
            logger.info("Deposit " + amount + " to " + accountNumber);
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(String accountNumber, BigDecimal amount)
            throws ValidationException, InsufficientFundsException, IOException {
        checkAmount(amount);
        ReentrantLock lock = lockManager.lockFor(accountNumber);
        lock.lock();
        try {
            Account acc = accounts.get(accountNumber);
            if (acc.getBalance().compareTo(amount) < 0) {
                logger.warning("Insufficient funds: " + accountNumber);
                throw new InsufficientFundsException("Insufficient balance.");
            }
            acc.withdraw(amount);
            store.saveAccounts(snapshotAccounts());
            store.appendTransaction(new Transaction(LocalDateTime.now(), accountNumber,
                    TransactionType.WITHDRAW, amount, acc.getBalance(), "Cash withdrawal", ""));
            logger.info("Withdraw " + amount + " from " + accountNumber);
        } finally {
            lock.unlock();
        }
    }

    public void transfer(String fromAcc, String toAcc, BigDecimal amount)
            throws ValidationException, InsufficientFundsException, IOException {
        if (java.util.Objects.equals(fromAcc, toAcc)) throw new ValidationException("Cannot transfer to same account.");
        checkAmount(amount);

        // order locks by account number to avoid deadlocks
        String a = fromAcc.compareTo(toAcc) < 0 ? fromAcc : toAcc;
        String b = fromAcc.compareTo(toAcc) < 0 ? toAcc : fromAcc;

        ReentrantLock lockA = lockManager.lockFor(a);
        ReentrantLock lockB = lockManager.lockFor(b);

        lockA.lock();
        lockB.lock();
        try {
            Account src = accounts.get(fromAcc);
            Account dst = accounts.get(toAcc);
            if (src == null || dst == null || !src.isActive() || !dst.isActive()) {
                throw new ValidationException("Invalid or inactive destination/source account.");
            }
            if (src.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient balance for transfer.");
            }
            src.withdraw(amount);
            dst.deposit(amount);
            store.saveAccounts(snapshotAccounts());
            store.appendTransaction(new Transaction(LocalDateTime.now(), fromAcc,
                    TransactionType.TRANSFER_OUT, amount, src.getBalance(),
                    "Transfer to " + toAcc, toAcc));
            store.appendTransaction(new Transaction(LocalDateTime.now(), toAcc,
                    TransactionType.TRANSFER_IN, amount, dst.getBalance(),
                    "Transfer from " + fromAcc, fromAcc));
            logger.info("Transfer " + amount + " from " + fromAcc + " to " + toAcc);
        } finally {
            lockB.unlock();
            lockA.unlock();
        }
    }

    public java.util.List<Transaction> lastN(String accountNumber, int n) throws IOException, ValidationException {
        if (n <= 0) throw new ValidationException("N must be positive.");
        return store.loadLastNTransactions(accountNumber, n);
    }

    // --- Admin operations ---

    public synchronized Account createAccount(String name, Account.Role role, BigDecimal initialBalance, String pin)
            throws ValidationException, IOException {
        if (name == null || name.isBlank()) throw new ValidationException("Name is required.");
        checkAmount(initialBalance);
        String newAcc = generateAccountNumber();
        String salt = SecurityUtil.generateSaltHex(8);
        String hash = SecurityUtil.hashPin(pin, salt);
        Account acc = new Account(newAcc, name.trim(), role, Account.normalize(initialBalance), hash, salt, true);
        accounts.put(newAcc, acc);
        store.saveAccounts(snapshotAccounts());
        store.appendTransaction(new Transaction(LocalDateTime.now(), newAcc,
                TransactionType.ACCOUNT_CREATE, BigDecimal.ZERO, acc.getBalance(), "Account created", ""));
        logger.info("Admin created account " + newAcc + " (" + role + ")");
        return acc;
    }

    public synchronized void deleteAccount(String accountNumber) throws ValidationException, IOException {
        Account acc = accounts.get(accountNumber);
        if (acc == null) throw new ValidationException("Account does not exist.");
        acc.setActive(false);
        store.saveAccounts(snapshotAccounts());
        store.appendTransaction(new Transaction(LocalDateTime.now(), accountNumber,
                TransactionType.ACCOUNT_DELETE, BigDecimal.ZERO, acc.getBalance(), "Account deactivated", ""));
        logger.info("Admin deactivated account " + accountNumber);
    }

    // --- helpers ---

    private String generateAccountNumber() {
        // naive generator: 6-digit starting at 100000+
        int base = 100000 + accounts.size() + new java.util.Random().nextInt(900);
        while (accounts.containsKey(String.valueOf(base))) base++;
        return String.valueOf(base);
    }

    private static void checkAmount(BigDecimal amount) throws ValidationException {
        if (amount == null) throw new ValidationException("Amount is required.");
        if (amount.scale() > 2) throw new ValidationException("Use two decimal places at most.");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new ValidationException("Amount must be > 0.");
    }
}
