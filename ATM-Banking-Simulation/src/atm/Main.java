/*
 * Main.java - Entry point. Wires DataStore, Bank, ATM, Logger; starts CLI.
 */
package atm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.*;

public class Main {
    public static void main(String[] args) {
        try {
            java.nio.file.Path dataDir = java.nio.file.Path.of("data");
            java.nio.file.Path logsDir = java.nio.file.Path.of("logs");
            if (!logsDir.toFile().exists()) logsDir.toFile().mkdirs();

            // Logger
            Logger logger = Logger.getLogger("atm");
            logger.setUseParentHandlers(false);
            Handler file = new FileHandler(logsDir.resolve("atm.log").toString(), true);
            file.setFormatter(new SimpleFormatter());
            logger.addHandler(file);
            logger.setLevel(Level.INFO);

            FileDataStore store = new FileDataStore(dataDir);
            Bank bank = new Bank(store, logger);
            ATM atm = new ATM(bank);
            new ConsoleUI(atm, logger).startInteractive();

            // Safe exit: nothing to flush beyond what FileDataStore already persists per op.
            logger.info("ATM shut down cleanly.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Fatal I/O: " + e.getMessage());
        }
    }
}
