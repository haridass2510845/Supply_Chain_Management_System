<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ page import="com.scms.model.User" %>
        <% User user=(session !=null) ? (User) session.getAttribute("user") : null; if (user==null) {
            response.sendRedirect("login.jsp?sessionExpired=true"); return; } %>
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
                        <span>
                            <%= user.getFullName() %>
                        </span>
                        <span class="role-tag">Administrator</span>
                        <a class="logout" href="change_password.jsp" style="margin-right:6px;">Change Password</a>
                        <a class="logout" href="LogoutServlet">Logout</a>
                    </div>
                </div>

                <div class="container">
                    <div class="welcome-card">
                        <div>
                            <span class="eyebrow">Full System Access</span>
                            <h3>Welcome back, <%= user.getFullName() %>
                            </h3>
                            <p>Admin Dashboard &mdash; here's what needs your attention.</p>
                        </div>
                        <div class="status-pill"><span class="dot"></span> System Operational</div>
                    </div>

                    <div class="section-heading">
                        <span class="eyebrow">Available Modules</span>
                    </div>

                    <div class="card-grid">
                        <a class="module-card" href="manage_users.jsp" style="text-decoration:none; display:block;">
                            <h4>Manage Users</h4>
                            <p>Create, update, or deactivate user accounts across all roles.</p>
                        </a>
                        <a class="module-card" href="suppliers.jsp" style="text-decoration:none; display:block;">
                            <h4>Manage Suppliers</h4>
                            <p>Add, update, delete, and search supplier records.</p>
                        </a>
                        <a class="module-card" href="purchase_orders.jsp" style="text-decoration:none; display:block;">
                            <h4>Procurement Overview</h4>
                            <p>Monitor purchase orders across all procurement managers.</p>
                        </a>
                        <a class="module-card" href="warehouse.jsp" style="text-decoration:none; display:block;">
                            <h4>Warehouse Overview</h4>
                            <p>Monitor stock levels and warehouse operations.</p>
                        </a>
                        <a class="module-card" href="logistics.jsp" style="text-decoration:none; display:block;">
                            <h4>Logistics Overview</h4>
                            <p>Track shipments and delivery status across the network.</p>
                        </a>
                        <a class="module-card" href="orders.jsp" style="text-decoration:none; display:block;">
                            <h4>Order Fulfillment</h4>
                            <p>Receive customer orders, verify stock, process, and confirm delivery.</p>
                        </a>
                        <a class="module-card" href="reports.jsp" style="text-decoration:none; display:block;">
                            <h4>Reports</h4>
                            <p>Generate supplier, procurement, inventory, warehouse, logistics, and order reports.</p>
                        </a>
                        <a class="module-card" href="monitor.jsp" style="text-decoration:none; display:block;">
                            <h4>Monitor System</h4>
                            <p>View system health, availability, and audit activity.</p>
                        </a>
                    </div>
                </div>

                <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
            </body>

            </html>