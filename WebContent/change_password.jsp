<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%
    User user = (session != null) ? (User) session.getAttribute("user") : null;
    if (user == null) {
        response.sendRedirect("login.jsp?sessionExpired=true");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>Change Password - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag"><%= user.getRole().replace("_", " ") %></span>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="login-wrapper" style="min-height: calc(100vh - 66px);">
        <div class="login-box">
            <div class="brand-strip">
                <div class="brand-mark"><span class="crate"></span> Account Settings</div>
                <h1>Change Password</h1>
                <p class="subtitle">Update the password for your account.</p>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="alert alert-error"><%= request.getAttribute("errorMessage") %></div>
            <% } %>

            <% if (request.getAttribute("successMessage") != null) { %>
                <div class="alert alert-success"><%= request.getAttribute("successMessage") %></div>
            <% } %>

            <form action="ChangePasswordServlet" method="post">
                <div class="form-group">
                    <label for="currentPassword">Current Password</label>
                    <input type="password" id="currentPassword" name="currentPassword" required autofocus>
                </div>
                <div class="form-group">
                    <label for="newPassword">New Password</label>
                    <input type="password" id="newPassword" name="newPassword" minlength="8" required>
                </div>
                <div class="form-group">
                    <label for="confirmPassword">Confirm New Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" minlength="8" required>
                </div>
                <button type="submit" class="btn-login">Update Password</button>
            </form>

            <p class="demo-note">
                <a href="<%= "ADMIN".equals(user.getRole()) ? "dashboard_admin.jsp" :
                             "PROCUREMENT_MANAGER".equals(user.getRole()) ? "dashboard_procurement.jsp" :
                             "WAREHOUSE_MANAGER".equals(user.getRole()) ? "dashboard_warehouse.jsp" :
                             "SUPPLIER".equals(user.getRole()) ? "dashboard_supplier.jsp" :
                             "dashboard_logistics.jsp" %>">&larr; Back to Dashboard</a>
            </p>
        </div>
    </div>
</body>
</html>
