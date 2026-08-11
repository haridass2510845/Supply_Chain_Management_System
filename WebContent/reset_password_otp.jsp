<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.util.OtpChallenge" %>
<%@ page import="com.scms.util.EmailUtil" %>
<%@ page import="com.scms.servlet.ForgotPasswordServlet" %>
<%
    OtpChallenge challenge = (session != null) ? (OtpChallenge) session.getAttribute(ForgotPasswordServlet.SESSION_KEY) : null;
    if (challenge == null) {
        response.sendRedirect("forgot_password.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>Reset Password - Supply Chain Management System</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="login-wrapper">
        <div class="login-box">
            <div class="brand-strip">
                <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
                <h1>Reset your password</h1>
                <p class="subtitle">Enter the 6-digit code we sent to <strong><%= challenge.getEmail() %></strong>, then choose a new password.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-error"><%= request.getAttribute("errorMessage") %></div>
            <% } %>
            <% if (request.getAttribute("successMessage") != null) { %>
                <div class="alert alert-success"><%= request.getAttribute("successMessage") %></div>
            <% } %>
            <% if (EmailUtil.isDevMode()) { %>
                <div class="alert alert-success">Dev mode: SMTP isn't configured yet, so the code was printed to the Tomcat console instead of emailed.</div>
            <% } %>

            <form action="ForgotPasswordServlet" method="post">
                <input type="hidden" name="action" value="verify">
                <div class="form-group">
                    <label for="otp">Verification Code</label>
                    <input type="text" id="otp" name="otp" class="otp-input"
                           inputmode="numeric" pattern="[0-9]{6}" maxlength="6"
                           autocomplete="one-time-code" required autofocus>
                </div>
                <div class="form-group">
                    <label for="newPassword">New Password</label>
                    <input type="password" id="newPassword" name="newPassword" minlength="8" required>
                </div>
                <div class="form-group">
                    <label for="confirmPassword">Confirm New Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" minlength="8" required>
                </div>
                <button type="submit" class="btn-login">Reset Password</button>
            </form>

            <form action="ForgotPasswordServlet" method="post" style="margin-top:10px;">
                <input type="hidden" name="action" value="resend">
                <button type="submit" class="btn-secondary" style="width:100%; text-align:center;">Resend Code</button>
            </form>

            <p class="demo-note">
                <a href="ForgotPasswordServlet?action=cancel">Cancel and start over</a>
            </p>
        </div>
    </div>
</body>
</html>
