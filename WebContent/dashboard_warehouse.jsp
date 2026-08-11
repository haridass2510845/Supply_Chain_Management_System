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
    <title>Warehouse Dashboard - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag">Warehouse Manager</span>
            <a class="logout" href="change_password.jsp" style="margin-right:6px;">Change Password</a>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <div>
                <span class="eyebrow">Inventory & Storage</span>
                <h3>Welcome back, <%= user.getFullName() %></h3>
                <p>Warehouse Dashboard &mdash; here's what needs your attention.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> System Operational</div>
        </div>

        <div class="section-heading">
            <span class="eyebrow">Available Modules</span>
        </div>

        <div class="card-grid">
            <a class="module-card" href="warehouse.jsp" style="text-decoration:none; display:block;">
                <h4>Receive Goods</h4>
                <p>Record incoming goods from suppliers into the warehouse.</p>
            </a>
            <a class="module-card" href="warehouse.jsp" style="text-decoration:none; display:block;">
                <h4>Store Inventory</h4>
                <p>View current stock levels and warehouse locations.</p>
            </a>
            <a class="module-card" href="warehouse.jsp" style="text-decoration:none; display:block;">
                <h4>Update Stock</h4>
                <p>Adjust stock quantities and generate stock alerts.</p>
            </a>
            <a class="module-card" href="warehouse.jsp" style="text-decoration:none; display:block;">
                <h4>Dispatch Goods</h4>
                <p>Release goods for outgoing orders and shipments.</p>
            </a>
            <a class="module-card" href="warehouse.jsp" style="text-decoration:none; display:block;">
                <h4>Warehouse Reports</h4>
                <p>Generate warehouse operations and inventory status reports.</p>
            </a>
        </div>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
