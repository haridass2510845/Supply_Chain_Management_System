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
    <title>Admin Dashboard - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag">Administrator</span>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <div>
                <span class="eyebrow">Full System Access</span>
                <h3>Welcome back, <%= user.getFullName() %></h3>
                <p>Admin Dashboard &mdash; here's what needs your attention.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> System Operational</div>
        </div>

        <div class="section-heading">
            <span class="eyebrow">Available Modules</span>
        </div>

        <div class="card-grid">
            <div class="module-card">
                <span class="tag">MOD-01</span>
                <h4>Manage Users</h4>
                <p>Create, update, or deactivate user accounts across all roles.</p>
            </div>
            <div class="module-card">
                <span class="tag">MOD-02</span>
                <h4>Manage Suppliers</h4>
                <p>Add, update, delete, and search supplier records.</p>
            </div>
            <div class="module-card">
                <span class="tag">MOD-03</span>
                <h4>Procurement Overview</h4>
                <p>Monitor purchase orders across all procurement managers.</p>
            </div>
            <div class="module-card">
                <span class="tag">MOD-04</span>
                <h4>Warehouse Overview</h4>
                <p>Monitor stock levels and warehouse operations.</p>
            </div>
            <div class="module-card">
                <span class="tag">MOD-05</span>
                <h4>Logistics Overview</h4>
                <p>Track shipments and delivery status across the network.</p>
            </div>
            <div class="module-card">
                <span class="tag">MOD-06</span>
                <h4>Reports</h4>
                <p>Generate supplier, procurement, inventory, warehouse, logistics, and order reports.</p>
            </div>
            <div class="module-card">
                <span class="tag">MOD-07</span>
                <h4>Monitor System</h4>
                <p>View system health, availability, and audit activity.</p>
            </div>
        </div>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
