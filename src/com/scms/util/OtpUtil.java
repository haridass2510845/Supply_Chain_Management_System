package com.scms.util;

import java.security.SecureRandom;

/**
 * Generates one-time verification codes used by the email-verification
 * (registration) and forgot-password flows.
 */
public class OtpUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * @return a random 6-digit numeric code as a String, e.g. "042917".
     *         Always exactly 6 digits (range 000000-999999).
     */
    public static String generateOtp() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private OtpUtil() {
        // static utility class
    }
}
