package com.scms.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Handles password hashing so that plain-text passwords are never
 * stored in the database (SRS Non-Functional Requirement: Encrypted
 * Password Storage).
 */
public class PasswordUtil {

    /**
     * Hashes a plain-text password using SHA-256 and returns it
     * as a lowercase hexadecimal string.
     */
    public static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Error while hashing password", e);
        }
    }

    /**
     * Compares a plain-text password against a stored hash.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        String hashOfInput = hashPassword(plainPassword);
        return hashOfInput.equals(storedHash);
    }
}
