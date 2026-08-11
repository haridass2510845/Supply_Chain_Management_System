<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Forgot Password - Supply Chain Management System</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="login-wrapper">
        <div class="login-box">
            <div class="brand-strip">
                <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
                <h1>Forgot your password?</h1>
                <p class="subtitle">Enter your username or email and we'll send a verification code to reset it.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-error"><%= request.getAttribute("errorMessage") %></div>
            <% } %>
            <% if ("true".equals(request.getParameter("expired"))) { %>
                <div class="alert alert-error">Your reset session expired or was lost. Please try again.</div>
            <% } %>

            <form action="ForgotPasswordServlet" method="post">
                <input type="hidden" name="action" value="request">
                <div class="form-group">
                    <label for="identifier">Username or Email</label>
                    <input type="text" id="identifier" name="identifier" required autofocus>
                </div>
                <button type="submit" class="btn-login">Send Verification Code</button>
            </form>

            <p class="demo-note">
                <a href="login.jsp">&larr; Back to sign in</a>
            </p>
        </div>
    </div>
</body>
</html>
