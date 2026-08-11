package com.scms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.net.ssl.SSLSocketFactory;

/**
 * Sends the OTP verification/reset emails using a minimal hand-rolled
 * SMTP client (STARTTLS + AUTH LOGIN) built entirely on the standard
 * JDK -- no JavaMail / Jakarta Mail jar required, so there is nothing
 * extra to add to WEB-INF/lib.
 *
 * ------------------------------------------------------------------
 * SETUP (do this before going live):
 * ------------------------------------------------------------------
 * 1. Fill in SMTP_USERNAME and SMTP_PASSWORD below with a real mailbox.
 * For Gmail:
 * - SMTP_HOST = "smtp.gmail.com", SMTP_PORT = 587
 * - SMTP_USERNAME = your full Gmail address
 * - SMTP_PASSWORD = a 16-character "App Password"
 * (Google Account -> Security -> 2-Step Verification -> App
 * Passwords). Your normal Gmail password will NOT work here.
 * Outlook/Office365 works the same way with smtp.office365.com:587
 * and your normal account password (or an app password if MFA is on).
 *
 * 2. Until you do that, DEV_MODE stays on automatically (see below) and
 * the app will NOT fail -- every OTP is simply printed to the Tomcat
 * console instead of emailed, so you can still test registration and
 * password reset end-to-end without any mail server at all.
 * ------------------------------------------------------------------
 */
public class EmailUtil {

    // ---- SMTP configuration - EDIT THESE 4 LINES ----
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USERNAME = System.getenv("SMTP_USERNAME");

    private static final String SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");

    private static final String FROM_NAME = "SCMS Control Tower";
    private static final int SOCKET_TIMEOUT_MS = 15000;

    /**
     * True until the two placeholder values above are replaced with a
     * real mailbox. While true, no network call is made and the OTP is
     * printed to the server console instead.
     */
    private static final boolean DEV_MODE = SMTP_USERNAME == null || SMTP_USERNAME.isBlank()
            || SMTP_PASSWORD == null || SMTP_PASSWORD.isBlank();

    public static boolean isDevMode() {
        return DEV_MODE;
    }

    /**
     * Sends (or, in dev mode, prints) a one-time code to the given address.
     *
     * @param toEmail recipient address
     * @param toName  recipient display name (may be null)
     * @param otp     the 6-digit code
     * @param purpose short phrase used in the email body, e.g.
     *                "verifying your new account" or "resetting your password"
     * @return true if the email was sent (or printed, in dev mode) successfully
     */
    public static boolean sendOtpEmail(String toEmail, String toName, String otp, String purpose) {
        String subject = "SCMS - Your verification code";
        String body = "Hello " + (toName != null && !toName.isEmpty() ? toName : "there") + ",\r\n\r\n"
                + "Your one-time verification code for " + purpose + " is:\r\n\r\n"
                + "        " + otp + "\r\n\r\n"
                + "This code expires in 5 minutes. If you did not request this, you can safely ignore this email.\r\n\r\n"
                + "- SCMS Control Tower\r\n";

        if (DEV_MODE) {
            System.out.println("=========================================================");
            System.out.println(" [SCMS DEV MODE] SMTP is not configured (see EmailUtil.java).");
            System.out.println(" No real email was sent - printing the code here instead:");
            System.out.println(" To      : " + toEmail);
            System.out.println(" Purpose : " + purpose);
            System.out.println(" OTP CODE: " + otp);
            System.out.println("=========================================================");
            return true;
        }

        try {
            sendViaSmtp(toEmail, subject, body);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Raw SMTP client
    // ------------------------------------------------------------------

    private static void sendViaSmtp(String toEmail, String subject, String body) throws IOException {
        try (Socket plainSocket = new Socket(SMTP_HOST, SMTP_PORT)) {
            plainSocket.setSoTimeout(SOCKET_TIMEOUT_MS);

            BufferedReader plainIn = reader(plainSocket);
            PrintWriter plainOut = writer(plainSocket);

            readResponse(plainIn, 220);
            command(plainOut, plainIn, "EHLO localhost", 250);
            command(plainOut, plainIn, "STARTTLS", 220);

            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            Socket tlsSocket = sslFactory.createSocket(plainSocket, SMTP_HOST, SMTP_PORT, true);
            tlsSocket.setSoTimeout(SOCKET_TIMEOUT_MS);

            BufferedReader in = reader(tlsSocket);
            PrintWriter out = writer(tlsSocket);

            command(out, in, "EHLO localhost", 250);
            command(out, in, "AUTH LOGIN", 334);
            command(out, in, base64(SMTP_USERNAME), 334);
            command(out, in, base64(SMTP_PASSWORD), 235);
            command(out, in, "MAIL FROM:<" + SMTP_USERNAME + ">", 250);
            command(out, in, "RCPT TO:<" + toEmail + ">", 250);
            command(out, in, "DATA", 354);

            String message = "From: " + FROM_NAME + " <" + SMTP_USERNAME + ">\r\n"
                    + "To: <" + toEmail + ">\r\n"
                    + "Subject: " + subject + "\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\n"
                    + "\r\n"
                    + body + "\r\n"
                    + ".";

            command(out, in, message, 250);
            command(out, in, "QUIT", 221);

            tlsSocket.close();
        }
    }

    private static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private static PrintWriter writer(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
    }

    private static String base64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static void command(PrintWriter out, BufferedReader in, String cmd, int expectedCode) throws IOException {
        out.print(cmd + "\r\n");
        out.flush();
        readResponse(in, expectedCode);
    }

    /**
     * Reads one full (possibly multi-line) SMTP response and checks its status
     * code.
     */
    private static String readResponse(BufferedReader in, int expectedCode) throws IOException {
        String line;
        String last = null;
        while ((line = in.readLine()) != null) {
            last = line;
            // multi-line responses look like "250-First" ... "250 Last";
            // a space (not a dash) after the 3-digit code marks the final line.
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
        if (last == null) {
            throw new IOException("SMTP server closed the connection unexpectedly.");
        }
        int code;
        try {
            code = Integer.parseInt(last.substring(0, 3));
        } catch (NumberFormatException e) {
            throw new IOException("Unexpected SMTP response: " + last);
        }
        if (code != expectedCode) {
            throw new IOException("SMTP error, expected " + expectedCode + " but got: " + last);
        }
        return last;
    }

    private EmailUtil() {
        // static utility class
    }
}
