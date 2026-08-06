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
    <title>Procurement Dashboard - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag">Procurement Manager</span>
            <a class="logout" href="change_password.jsp" style="margin-right:6px;">Change Password</a>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <div>
                <span class="eyebrow">Purchasing & Sourcing</span>
                <h3>Welcome back, <%= user.getFullName() %></h3>
                <p>Procurement Dashboard &mdash; here's what needs your attention.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> System Operational</div>
        </div>

        <div class="section-heading">
            <span class="eyebrow">Available Modules</span>
        </div>

        <div class="card-grid">
            <div class="module-card">
                <span class="tag">PO-01</span>
                <h4>Create Purchase Order</h4>
                <p>Raise a new purchase order for a registered supplier.</p>
            </div>
            <div class="module-card">
                <span class="tag">PO-02</span>
                <h4>Approve Purchase Order</h4>
                <p>Review and approve pending purchase orders.</p>
            </div>
            <div class="module-card">
                <span class="tag">PO-03</span>
                <h4>Cancel Purchase Order</h4>
                <p>Cancel purchase orders that are no longer required.</p>
            </div>
            <div class="module-card">
                <span class="tag">PO-04</span>
                <h4>Track Procurement Status</h4>
                <p>Monitor the status of all active procurement requests.</p>
            </div>
            <div class="module-card">
                <span class="tag">PO-05</span>
                <h4>Supplier Performance</h4>
                <p>Track supplier reliability and delivery performance.</p>
            </div>
            <div class="module-card">
                <span class="tag">PO-06</span>
                <h4>Procurement Reports</h4>
                <p>Generate reports on procurement records.</p>
            </div>
        </div>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
