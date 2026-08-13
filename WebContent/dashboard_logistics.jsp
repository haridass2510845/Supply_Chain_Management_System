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
    <title>Logistics Dashboard - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag">Logistics Staff</span>
            <a class="logout" href="change_password.jsp" style="margin-right:6px;">Change Password</a>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <div>
                <span class="eyebrow">Transport & Delivery</span>
                <h3>Welcome back, <%= user.getFullName() %></h3>
                <p>Logistics Dashboard &mdash; here's what needs your attention.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> System Operational</div>
        </div>

        <div class="section-heading">
            <span class="eyebrow">Available Modules</span>
        </div>

        <div class="card-grid">
            <a class="module-card" href="logistics.jsp" style="text-decoration:none; display:block;">
                <h4>Assign Delivery</h4>
                <p>Assign vehicles and drivers to outgoing orders.</p>
            </a>
            <a class="module-card" href="logistics.jsp" style="text-decoration:none; display:block;">
                <h4>Track Shipment</h4>
                <p>Monitor shipment status across the network.</p>
            </a>
            <a class="module-card" href="logistics.jsp" style="text-decoration:none; display:block;">
                <h4>Update Delivery Status</h4>
                <p>Update the delivery status of in-transit orders.</p>
            </a>
            <a class="module-card" href="logistics.jsp" style="text-decoration:none; display:block;">
                <h4>Confirm Delivery</h4>
                <p>Mark orders as successfully delivered.</p>
            </a>
        </div>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
