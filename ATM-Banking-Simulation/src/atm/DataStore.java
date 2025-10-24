/*
 * DataStore.java - Persistence abstraction for accounts and transactions.
 */
package atm;

import java.io.IOException;
import java.util.List;

public interface DataStore {
    java.util.List<Account> loadAccounts() throws IOException;
    void saveAccounts(java.util.List<Account> accounts) throws IOException;

    void appendTransaction(Transaction tx) throws IOException;

    java.util.List<Transaction> loadLastNTransactions(String accountNumber, int n) throws IOException;
}
