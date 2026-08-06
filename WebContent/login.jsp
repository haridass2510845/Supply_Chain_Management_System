<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Login - Supply Chain Management System</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="login-wrapper">
        <div class="login-box">
            <div class="brand-strip">
                <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
                <h1>Sign in to your dashboard</h1>
                <p class="subtitle">Track, manage, and move freight end to end.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-error"><%= request.getAttribute("errorMessage") %></div>
            <% } %>

            <% if ("true".equals(request.getParameter("loggedOut"))) { %>
                <div class="alert alert-success">You have been logged out successfully.</div>
            <% } %>

            <% if ("true".equals(request.getParameter("sessionExpired"))) { %>
                <div class="alert alert-error">Your session has expired. Please log in again.</div>
            <% } %>

            <% if ("true".equals(request.getParameter("registered"))) { %>
                <div class="alert alert-success">Account created successfully! You can now sign in.</div>
            <% } %>

            <form action="LoginServlet" method="post">
                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username" required autofocus>
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <button type="submit" class="btn-login">Sign In</button>
            </form>


            <p class="demo-note">
                Don't have an account? Then <a href="register.jsp">register here</a>
            </p>
        </div>
    </div>
</body>
</html>
