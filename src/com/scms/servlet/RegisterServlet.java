package com.scms.servlet;

import com.scms.dao.UserDAO;
import com.scms.util.EmailUtil;
import com.scms.util.OtpChallenge;
import com.scms.util.OtpUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Handles new account sign-up, now gated behind email OTP verification:
 *
 *   1. action=start  (the register.jsp form) - validates the details,
 *      generates a 6-digit code, emails it, and stashes the still-pending
 *      registration in the session (nothing is written to the database yet).
 *   2. action=verify (the verify_otp.jsp form) - checks the code; on
 *      success the account is finally created.
 *   3. action=resend - issues a fresh code for the same pending registration.
 *   4. action=cancel - discards the pending registration.
 */
@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    public static final String SESSION_KEY = "pendingRegistrationOtp";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        action = (action == null) ? "start" : action;

        switch (action) {
            case "verify":
                handleVerify(request, response);
                break;
            case "resend":
                handleResend(request, response);
                break;
            case "cancel":
                handleCancel(request, response);
                break;
            case "start":
            default:
                handleStart(request, response);
                break;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("cancel".equals(request.getParameter("action"))) {
            handleCancel(request, response);
            return;
        }
        response.sendRedirect("register.jsp");
    }

    // ------------------------------------------------------------------

    private void handleStart(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username  = trim(request.getParameter("username"));
        String password  = request.getParameter("password");
        String confirm   = request.getParameter("confirmPassword");
        String fullName  = trim(request.getParameter("fullName"));
        String email     = trim(request.getParameter("email"));
        String role      = trim(request.getParameter("role"));

        // --- Basic validation ---
        if (isEmpty(username) || isEmpty(password) || isEmpty(fullName) || isEmpty(email) || isEmpty(role)) {
            fail(request, response, "All fields are required, including a valid email for verification.");
            return;
        }

        if (password.length() < 8) {
            fail(request, response, "Password must be at least 8 characters long.");
            return;
        }

        if (!password.equals(confirm)) {
            fail(request, response, "Passwords do not match.");
            return;
        }

        if (!isValidEmail(email)) {
            fail(request, response, "Please enter a valid email address - we'll send a verification code to it.");
            return;
        }

        if (!isValidRole(role)) {
            fail(request, response, "Please select a valid role.");
            return;
        }

        if (userDAO.usernameExists(username)) {
            fail(request, response, "That username is already taken. Please choose another.");
            return;
        }

        if (userDAO.emailExists(email)) {
            fail(request, response, "That email address is already registered. Try signing in or use a different email.");
            return;
        }

        // --- Everything checks out: hold the registration and email a code ---
        String otp = OtpUtil.generateOtp();
        OtpChallenge challenge = new OtpChallenge(otp, email);
        challenge.put("username", username);
        challenge.put("password", password);
        challenge.put("fullName", fullName);
        challenge.put("email", email);
        challenge.put("role", role);

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_KEY, challenge);

        EmailUtil.sendOtpEmail(email, fullName, otp, "verifying your new SCMS account");

        response.sendRedirect("verify_otp.jsp");
    }

    private void handleVerify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        OtpChallenge challenge = (session != null) ? (OtpChallenge) session.getAttribute(SESSION_KEY) : null;

        if (challenge == null) {
            response.sendRedirect("register.jsp?expired=true");
            return;
        }

        if (challenge.isExpired()) {
            failOtp(request, response, "This code has expired. Please request a new one.");
            return;
        }

        if (challenge.isLocked()) {
            failOtp(request, response, "Too many incorrect attempts. Please request a new code.");
            return;
        }

        String otp = trim(request.getParameter("otp"));

        if (!challenge.matches(otp)) {
            failOtp(request, response,
                    "Incorrect code. " + challenge.getAttemptsRemaining() + " attempt(s) remaining.");
            return;
        }

        // Re-check for a race with another registration completed in the meantime.
        String username = challenge.get("username");
        String email = challenge.get("email");
        if (userDAO.usernameExists(username)) {
            session.removeAttribute(SESSION_KEY);
            fail(request, response, "That username was just taken by someone else. Please register again.");
            return;
        }
        if (userDAO.emailExists(email)) {
            session.removeAttribute(SESSION_KEY);
            fail(request, response, "That email was just registered by someone else. Please register again.");
            return;
        }

        boolean created = userDAO.registerUser(
                username,
                challenge.get("password"),
                challenge.get("fullName"),
                email,
                challenge.get("role"));

        session.removeAttribute(SESSION_KEY);

        if (!created) {
            fail(request, response, "Registration failed. Please try again.");
            return;
        }

        response.sendRedirect("login.jsp?registered=true");
    }

    private void handleResend(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        OtpChallenge challenge = (session != null) ? (OtpChallenge) session.getAttribute(SESSION_KEY) : null;

        if (challenge == null) {
            response.sendRedirect("register.jsp?expired=true");
            return;
        }

        String newOtp = OtpUtil.generateOtp();
        challenge.regenerate(newOtp);
        EmailUtil.sendOtpEmail(challenge.getEmail(), challenge.get("fullName"), newOtp, "verifying your new SCMS account");

        request.setAttribute("successMessage", "A new code has been sent to " + challenge.getEmail() + ".");
        RequestDispatcher rd = request.getRequestDispatcher("verify_otp.jsp");
        rd.forward(request, response);
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
        response.sendRedirect("register.jsp");
    }

    // ------------------------------------------------------------------

    private boolean isValidRole(String role) {
        switch (role) {
            case "ADMIN":
            case "PROCUREMENT_MANAGER":
            case "WAREHOUSE_MANAGER":
            case "SUPPLIER":
            case "LOGISTICS_STAFF":
                return true;
            default:
                return false;
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        RequestDispatcher rd = request.getRequestDispatcher("register.jsp");
        rd.forward(request, response);
    }

    private void failOtp(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        RequestDispatcher rd = request.getRequestDispatcher("verify_otp.jsp");
        rd.forward(request, response);
    }
}
