/*
 * FileDataStore.java - CSV-based persistence for accounts and transactions.
 */
package atm;

import atm.Account.Role;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class FileDataStore implements DataStore {
    private final Path accountsPath;
    private final Path transactionsPath;

    public FileDataStore(Path dataDir) throws IOException {
        this.accountsPath = dataDir.resolve("accounts.csv");
        this.transactionsPath = dataDir.resolve("transactions.csv");
        if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
        if (!Files.exists(accountsPath)) {
            Files.createFile(accountsPath);
            // header
            Files.writeString(accountsPath,
                    "accountNumber,name,role,balance,pinHash,salt,isActive\n",
                    StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (!Files.exists(transactionsPath)) {
            Files.createFile(transactionsPath);
            Files.writeString(transactionsPath,
                    "timestamp,accountNumber,type,amount,balanceAfter,details,counterparty\n",
                    StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    @Override
    public synchronized java.util.List<Account> loadAccounts() throws IOException {
        java.util.List<String> lines = Files.readAllLines(accountsPath, StandardCharsets.UTF_8);
        java.util.List<Account> result = new java.util.ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // skip header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = splitCsv(line, 7);
            String acc = parts[0];
            String name = parts[1];
            Role role = Role.valueOf(parts[2]);
            BigDecimal bal = new BigDecimal(parts[3]);
            String pinHash = parts[4];
            String salt = parts[5];
            boolean active = Boolean.parseBoolean(parts[6]);
            result.add(new Account(acc, name, role, bal, pinHash, salt, active));
        }
        return result;
    }

    @Override
    public synchronized void saveAccounts(java.util.List<Account> accounts) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("accountNumber,name,role,balance,pinHash,salt,isActive\n");
        for (Account a : accounts) {
            sb.append(a.getAccountNumber()).append(",")
              .append(escape(a.getName())).append(",")
              .append(a.getRole().name()).append(",")
              .append(a.getBalance().toPlainString()).append(",")
              .append(a.getPinHash()).append(",")
              .append(a.getSalt()).append(",")
              .append(a.isActive()).append("\n");
        }
        Files.writeString(accountsPath, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public synchronized void appendTransaction(Transaction tx) throws IOException {
        Files.writeString(transactionsPath, tx.toCsvLine() + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    @Override
    public synchronized java.util.List<Transaction> loadLastNTransactions(String accountNumber, int n) throws IOException {
        // read all then filter the last n for the account (simple & fine for small projects)
        java.util.List<String> lines = Files.readAllLines(transactionsPath, StandardCharsets.UTF_8);
        java.util.List<Transaction> all = new java.util.ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // skip header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] p = splitCsv(line, 7);
            if (!java.util.Objects.equals(p[1], accountNumber)) continue;
            all.add(parseTx(line));
        }
        // keep last n by time order (already chronological)
        int start = Math.max(0, all.size() - n);
        return all.subList(start, all.size());
    }

    private Transaction parseTx(String line) {
        String[] p = splitCsv(line, 7);
        LocalDateTime ts = LocalDateTime.parse(p[0]);
        String acc = p[1];
        TransactionType type = TransactionType.valueOf(p[2]);
        BigDecimal amt = p[3].isEmpty() ? BigDecimal.ZERO : new BigDecimal(p[3]);
        BigDecimal balAfter = p[4].isEmpty() ? null : new BigDecimal(p[4]);
        String details = p[5];
        String cp = p[6];
        return new Transaction(ts, acc, type, amt, balAfter, details, cp);
    }

    // --- helpers ---

    private static String escape(String s) {
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String[] splitCsv(String line, int expected) {
        java.util.List<String> out = new java.util.ArrayList<>(expected);
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++; // escaped quote
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(cur.toString()); cur.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else cur.append(c);
            }
        }
        out.add(cur.toString());
        while (out.size() < expected) out.add("");
        return out.toArray(new String[0]);
    }
}
