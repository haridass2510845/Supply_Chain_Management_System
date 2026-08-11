package com.scms.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A short-lived, session-scoped one-time-passcode challenge.
 *
 * Both the "verify your email to register" flow and the "forgot password"
 * flow need the exact same thing: generate a code, email it, remember it
 * for a few minutes, and check what the user types back in. Nothing here
 * is ever written to the database -- it lives only in the browser's
 * HttpSession (server-side memory) until it is verified, resent, or
 * expires, so a half-finished registration/reset never leaves stray data
 * behind.
 *
 * The generic key/value {@code data} map lets each servlet stash whatever
 * it needs to complete the job once the code is verified (e.g. the pending
 * registration's username/password/fullName/email/role, or the userId
 * being reset).
 */
public class OtpChallenge implements Serializable {

    private static final long serialVersionUID = 1L;

    /** How long a code stays valid after it is (re)generated. */
    private static final long VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    /** How many wrong guesses are allowed before the code must be resent. */
    private static final int MAX_ATTEMPTS = 5;

    private String otp;
    private final String email;
    private long expiresAt;
    private int attempts;
    private final Map<String, String> data = new HashMap<>();

    public OtpChallenge(String otp, String email) {
        this.otp = otp;
        this.email = email;
        this.expiresAt = System.currentTimeMillis() + VALIDITY_MS;
        this.attempts = 0;
    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.get(key);
    }

    public String getEmail() {
        return email;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isLocked() {
        return attempts >= MAX_ATTEMPTS;
    }

    public int getAttemptsRemaining() {
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    /**
     * Checks a user-submitted code against this challenge. Every call
     * (right or wrong) counts as an attempt, so brute-forcing burns
     * through the limit.
     */
    public boolean matches(String candidate) {
        if (isExpired() || isLocked() || candidate == null) {
            attempts++;
            return false;
        }
        attempts++;
        return otp.equals(candidate.trim());
    }

    /**
     * Issues a fresh code for this same challenge (used by "Resend code"),
     * resetting the expiry and attempt counter.
     */
    public void regenerate(String newOtp) {
        this.otp = newOtp;
        this.expiresAt = System.currentTimeMillis() + VALIDITY_MS;
        this.attempts = 0;
    }

    public long getSecondsRemaining() {
        long ms = expiresAt - System.currentTimeMillis();
        return Math.max(0, ms / 1000);
    }
}
