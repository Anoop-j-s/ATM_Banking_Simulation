/*
 * SecurityUtil.java - PIN hashing and salt generation using SHA-256.
 */
package atm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class SecurityUtil {
    private static final SecureRandom RAND = new SecureRandom();

    public static String generateSaltHex(int bytes) {
        byte[] b = new byte[bytes];
        RAND.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    public static String hashPin(String pin, String saltHex) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = (pin + ":" + saltHex).getBytes(StandardCharsets.UTF_8);
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte x : digest) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash PIN", e);
        }
    }
}
