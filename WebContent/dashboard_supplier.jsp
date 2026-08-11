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
    <title>Supplier Dashboard - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag">Supplier</span>
            <a class="logout" href="change_password.jsp" style="margin-right:6px;">Change Password</a>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <div>
                <span class="eyebrow">Fulfillment Partner</span>
                <h3>Welcome back, <%= user.getFullName() %></h3>
                <p>Supplier Dashboard &mdash; here's what needs your attention.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> System Operational</div>
        </div>

        <div class="section-heading">
            <span class="eyebrow">Available Modules</span>
        </div>

        <div class="card-grid">
            <a class="module-card" href="my_orders.jsp" style="text-decoration:none; display:block;">
                <h4>View Purchase Orders</h4>
                <p>See all purchase orders assigned to your company.</p>
            </a>
            <a class="module-card" href="my_orders.jsp" style="text-decoration:none; display:block;">
                <h4>Update Shipment Status</h4>
                <p>Mark approved orders as shipped.</p>
            </a>
            <a class="module-card" href="my_orders.jsp" style="text-decoration:none; display:block;">
                <h4>Deliver Goods</h4>
                <p>Confirm delivery of goods against a purchase order.</p>
            </a>
            <a class="module-card" href="my_orders.jsp#performance" style="text-decoration:none; display:block;">
                <h4>Performance History</h4>
                <p>View your delivery performance and order history.</p>
            </a>
        </div>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
