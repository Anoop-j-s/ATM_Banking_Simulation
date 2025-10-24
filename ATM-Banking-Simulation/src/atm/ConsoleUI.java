/*
 * ConsoleUI.java - Menu-driven CLI for the ATM.
 */
package atm;

import atm.exceptions.AuthenticationException;
import atm.exceptions.InsufficientFundsException;
import atm.exceptions.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class ConsoleUI {
    private final ATM atm;
    private final Logger logger;

    public ConsoleUI(ATM atm, Logger logger) {
        this.atm = atm; this.logger = logger;
    }

    public void startInteractive() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("=== Welcome to ATM Banking Simulation ===");

            while (true) {
                System.out.print("\nEnter Account Number (or 'exit' to quit): ");
                String acc = sc.next().trim();
                if (acc.equalsIgnoreCase("exit")) break;

                System.out.print("Enter PIN: ");
                String pin = sc.next().trim();

                User user;
                try {
                    user = atm.login(acc, pin);
                } catch (AuthenticationException | IOException e) {
                    System.out.println("Login failed: " + e.getMessage());
                    continue;
                }

                System.out.printf("Hello, %s!%n", user.getDisplayName());

                boolean session = true;
                while (session) {
                    if (user.isAdmin()) {
                        session = adminMenu(sc, user);
                    } else {
                        session = userMenu(sc, user);
                    }
                }
            }
            System.out.println("Goodbye!");
        }
    }

    private boolean userMenu(Scanner sc, User user) {
        System.out.println("\n1) Balance  2) Deposit  3) Withdraw  4) Transfer  5) Last N tx  6) Logout");
        System.out.print("Choose: ");
        String choice = sc.next().trim();
        try {
            switch (choice) {
                case "1" -> {
                    String bal = atm.balance(user);
                    System.out.println("Current Balance: " + bal);
                }
                case "2" -> {
                    BigDecimal amt = readAmount(sc, "Amount to deposit: ");
                    atm.deposit(user, amt);
                    System.out.println("Deposit successful.");
                }
                case "3" -> {
                    BigDecimal amt = readAmount(sc, "Amount to withdraw: ");
                    atm.withdraw(user, amt);
                    System.out.println("Withdrawal successful.");
                }
                case "4" -> {
                    System.out.print("To Account #: ");
                    String to = sc.next().trim();
                    BigDecimal amt = readAmount(sc, "Amount to transfer: ");
                    atm.transfer(user, to, amt);
                    System.out.println("Transfer successful.");
                }
                case "5" -> {
                    System.out.print("Show last how many transactions? ");
                    int n = Integer.parseInt(sc.next().trim());
                    List<Transaction> list = atm.lastN(user, n);
                    if (list.isEmpty()) System.out.println("(No transactions yet)");
                    else {
                        System.out.println("Recent Transactions:");
                        list.forEach(tx -> System.out.printf("%s  %-14s  %10s  Bal:%s  %s%n",
                                tx.getTimestamp(), tx.getType(), tx.getAmount(),
                                tx.getBalanceAfter(), tx.getDetails()));
                    }
                }
                case "6" -> {
                    System.out.println("Logged out.");
                    return false;
                }
                default -> System.out.println("Invalid choice.");
            }
        } catch (ValidationException | InsufficientFundsException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
            logger.severe("I/O: " + e);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
        return true;
    }

    private boolean adminMenu(Scanner sc, User user) {
        System.out.println("\nADMIN: 1) Balance  2) Deposit  3) Withdraw  4) Transfer  5) Last N  6) Create Acct  7) Delete Acct  8) Simulate  9) Logout");
        System.out.print("Choose: ");
        String choice = sc.next().trim();
        try {
            switch (choice) {
                case "1","2","3","4","5" -> { return userMenu(sc, user); }
                case "6" -> {
                    System.out.print("New customer name: ");
                    String name = sc.next();
                    System.out.print("Role (USER/ADMIN): ");
                    String role = sc.next().toUpperCase();
                    BigDecimal init = readAmount(sc, "Initial balance: ");
                    System.out.print("Set PIN (digits only): ");
                    String pin = sc.next().trim();
                    Account created = atm.adminCreate(name, Account.Role.valueOf(role), init, pin);
                    System.out.println("Created account #: " + created.getAccountNumber());
                }
                case "7" -> {
                    System.out.print("Account to deactivate: ");
                    String del = sc.next().trim();
                    atm.adminDelete(del);
                    System.out.println("Account deactivated.");
                }
                case "8" -> {
                    System.out.print("Threads to simulate (e.g., 5): ");
                    int threads = Integer.parseInt(sc.next().trim());
                    simulateConcurrent(threads);
                }
                case "9" -> {
                    System.out.println("Logged out.");
                    return false;
                }
                default -> System.out.println("Invalid choice.");
            }
        } catch (Exception e) {
            System.out.println("Admin error: " + e.getMessage());
            logger.severe("Admin error: " + e);
        }
        return true;
    }

    private static BigDecimal readAmount(Scanner sc, String prompt) {
        System.out.print(prompt);
        return new BigDecimal(sc.next().trim());
    }

    // Optional multithreading simulation (small demo)
    private void simulateConcurrent(int threads) {
        System.out.println("Starting " + threads + " concurrent simulated users...");
        String[] demoAcc = {"100001","100002","100003"};
        java.util.Random rnd = new java.util.Random();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    String acc = demoAcc[rnd.nextInt(demoAcc.length)];
                    // all demo accounts use their known pins from accounts.csv comment
                    User u = atm.login(acc, switch (acc) {
                        case "100001" -> "1111";
                        case "100002" -> "2222";
                        case "100003" -> "3333";
                        default -> "0000";
                    });
                    for (int k = 0; k < 5; k++) {
                        int op = rnd.nextInt(3);
                        switch (op) {
                            case 0 -> atm.deposit(u, new BigDecimal("1.00"));
                            case 1 -> { try { atm.withdraw(u, new BigDecimal("1.00")); } catch (Exception ignore) {} }
                            case 2 -> atm.balance(u);
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            }, "SimUser-" + i).start();
        }
        try { latch.await(); } catch (InterruptedException ignored) {}
        System.out.println("Simulation complete. Check logs/atm.log");
    }
}
