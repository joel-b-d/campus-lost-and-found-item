package com.campus.lostfound.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Turns a password like "student123" into a scrambled, fixed-length
 * string that can't be reversed back into the original password.
 * We never store the real password — only this scrambled version.
 *
 * Why: if someone ever got access to the database, they'd see
 * "a94a8fe5ccb19ba61c4c..." instead of "student123".
 *
 * This uses SHA-256, a hashing algorithm built into Java itself —
 * no extra library needed, so there's nothing hidden here.
 */
public class PasswordUtil {

    public static String hash(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(plainPassword.getBytes());

            // Convert the scrambled bytes into a readable string of letters/numbers
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 always exists in Java, so this never actually happens.
            throw new RuntimeException(e);
        }
    }

    // Checks a typed-in password against the stored hash by hashing the
    // typed-in one and comparing the two scrambled strings.
    public static boolean matches(String plainPassword, String storedHash) {
        return hash(plainPassword).equals(storedHash);
    }
}
