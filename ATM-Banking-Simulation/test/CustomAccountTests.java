/*
 * CustomAccountTests.java - Minimal tests using built-in assertions.
 * Run with: javac ... && java -ea -cp out CustomAccountTests
 */
import atm.*;
import atm.exceptions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class CustomAccountTests {
    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger("atm-tests");
        FileDataStore store = new FileDataStore(java.nio.file.Path.of("data")); // uses existing CSV
        Bank bank = new Bank(store, logger);
        ATM atm = new ATM(bank);

        // Login existing sample account
        User u1 = atm.login("100001", "1111");

        BigDecimal start = new BigDecimal(atm.balance(u1));
        atm.deposit(u1, new BigDecimal("10.00"));
        BigDecimal afterDep = new BigDecimal(atm.balance(u1));
        assert afterDep.compareTo(start.add(new BigDecimal("10.00"))) == 0 : "Deposit failed";

        atm.withdraw(u1, new BigDecimal("5.00"));
        BigDecimal afterWd = new BigDecimal(atm.balance(u1));
        assert afterWd.compareTo(afterDep.subtract(new BigDecimal("5.00"))) == 0 : "Withdraw failed";

        // Transfer 1.00 to 100002
        atm.transfer(u1, "100002", new BigDecimal("1.00"));
        BigDecimal u1After = new BigDecimal(atm.balance(u1));
        assert u1After.compareTo(afterWd.subtract(new BigDecimal("1.00"))) == 0 : "Transfer out mismatch";

        User u2 = atm.login("100002", "2222");
        BigDecimal u2Bal = new BigDecimal(atm.balance(u2));
        assert u2Bal.compareTo(Account.normalize(new BigDecimal("501.00"))) >= 0 : "Transfer in missing?";

        java.util.List<Transaction> tx = atm.lastN(u1, 3);
        assert !tx.isEmpty() : "Expected some transactions";

        System.out.println("All custom tests passed ✔");
    }
}
