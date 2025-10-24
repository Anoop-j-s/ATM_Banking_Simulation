/*
 * LockManager.java - Provides per-account locks for safe concurrent operations.
 */
package atm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock lockFor(String accountNumber) {
        return locks.computeIfAbsent(accountNumber, k -> new ReentrantLock(true));
    }
}
