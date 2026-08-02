<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Register - Supply Chain Management System</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="login-wrapper">
        <div class="login-box">
            <div class="brand-strip">
                <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
                <h1>Create an account</h1>
                <p class="subtitle">Get access to your role's dashboard.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-error"><%= request.getAttribute("errorMessage") %></div>
            <% } %>

            <form action="RegisterServlet" method="post">
                <div class="form-group">
                    <label for="fullName">Full Name</label>
                    <input type="text" id="fullName" name="fullName"
                           value="<%= request.getParameter("fullName") != null ? request.getParameter("fullName") : "" %>"
                           required autofocus>
                </div>
                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username"
                           value="<%= request.getParameter("username") != null ? request.getParameter("username") : "" %>"
                           required>
                </div>
                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email"
                           value="<%= request.getParameter("email") != null ? request.getParameter("email") : "" %>">
                </div>
                <div class="form-group">
                    <label for="role">Role</label>
                    <select id="role" name="role" required>
                        <option value="">-- Select your role --</option>
                        <option value="ADMIN">Administrator</option>
                        <option value="PROCUREMENT_MANAGER">Procurement Manager</option>
                        <option value="WAREHOUSE_MANAGER">Warehouse Manager</option>
                        <option value="SUPPLIER">Supplier</option>
                        <option value="LOGISTICS_STAFF">Logistics Staff</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" minlength="8" required>
                </div>
                <div class="form-group">
                    <label for="confirmPassword">Confirm Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" minlength="8" required>
                </div>
                <button type="submit" class="btn-login">Create Account</button>
            </form>

            <p class="demo-note">
                Already have an account? <a href="login.jsp">Sign in here</a>
            </p>
        </div>
    </div>
</body>
</html>
