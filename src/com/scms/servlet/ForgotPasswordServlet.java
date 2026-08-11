package com.scms.servlet;

import com.scms.dao.UserDAO;
import com.scms.model.User;
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
 * "Forgot Password" self-service flow:
 *
 *   1. action=request (the forgot_password.jsp form) - looks the account
 *      up by username or email, generates a 6-digit code, and emails it
 *      to the address on file for that account.
 *   2. action=verify  (the reset_password_otp.jsp form) - checks the code
 *      and, if correct, sets the new password.
 *   3. action=resend  - issues a fresh code for the same pending reset.
 *   4. action=cancel  - discards the pending reset.
 */
@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {

    public static final String SESSION_KEY = "pendingResetOtp";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        action = (action == null) ? "request" : action;

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
            case "request":
            default:
                handleRequest(request, response);
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
        response.sendRedirect("forgot_password.jsp");
    }

    // ------------------------------------------------------------------

    private void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String identifier = trim(request.getParameter("identifier"));

        if (isEmpty(identifier)) {
            fail(request, response, "Please enter your username or email address.");
            return;
        }

        User user = userDAO.getUserByUsernameOrEmail(identifier);

        if (user == null) {
            fail(request, response, "No account was found with that username or email.");
            return;
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            fail(request, response, "This account has no email on file. Please contact your administrator.");
            return;
        }

        String otp = OtpUtil.generateOtp();
        OtpChallenge challenge = new OtpChallenge(otp, user.getEmail());
        challenge.put("userId", String.valueOf(user.getUserId()));
        challenge.put("username", user.getUsername());
        challenge.put("fullName", user.getFullName());

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_KEY, challenge);

        EmailUtil.sendOtpEmail(user.getEmail(), user.getFullName(), otp, "resetting your SCMS password");

        response.sendRedirect("reset_password_otp.jsp");
    }

    private void handleVerify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        OtpChallenge challenge = (session != null) ? (OtpChallenge) session.getAttribute(SESSION_KEY) : null;

        if (challenge == null) {
            response.sendRedirect("forgot_password.jsp?expired=true");
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
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (isEmpty(newPassword) || isEmpty(confirmPassword)) {
            failOtp(request, response, "Please enter and confirm your new password.");
            return;
        }
        if (newPassword.length() < 8) {
            failOtp(request, response, "New password must be at least 8 characters long.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            failOtp(request, response, "New password and confirmation do not match.");
            return;
        }

        if (!challenge.matches(otp)) {
            failOtp(request, response,
                    "Incorrect code. " + challenge.getAttemptsRemaining() + " attempt(s) remaining.");
            return;
        }

        int userId = parseInt(challenge.get("userId"));
        boolean updated = userId > 0 && userDAO.changePassword(userId, newPassword);

        session.removeAttribute(SESSION_KEY);

        if (!updated) {
            fail(request, response, "Could not reset your password. Please try again.");
            return;
        }

        response.sendRedirect("login.jsp?resetSuccess=true");
    }

    private void handleResend(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        OtpChallenge challenge = (session != null) ? (OtpChallenge) session.getAttribute(SESSION_KEY) : null;

        if (challenge == null) {
            response.sendRedirect("forgot_password.jsp?expired=true");
            return;
        }

        String newOtp = OtpUtil.generateOtp();
        challenge.regenerate(newOtp);
        EmailUtil.sendOtpEmail(challenge.getEmail(), challenge.get("fullName"), newOtp, "resetting your SCMS password");

        request.setAttribute("successMessage", "A new code has been sent to " + challenge.getEmail() + ".");
        RequestDispatcher rd = request.getRequestDispatcher("reset_password_otp.jsp");
        rd.forward(request, response);
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
        response.sendRedirect("forgot_password.jsp");
    }

    // ------------------------------------------------------------------

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        RequestDispatcher rd = request.getRequestDispatcher("forgot_password.jsp");
        rd.forward(request, response);
    }

    private void failOtp(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        RequestDispatcher rd = request.getRequestDispatcher("reset_password_otp.jsp");
        rd.forward(request, response);
    }
}
