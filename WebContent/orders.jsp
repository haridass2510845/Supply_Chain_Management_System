<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.CustomerOrder" %>
<%@ page import="com.scms.model.InventoryItem" %>
<%@ page import="com.scms.dao.OrderDAO" %>
<%@ page import="com.scms.dao.InventoryDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    User user = (session != null) ? (User) session.getAttribute("user") : null;
    if (user == null) {
        response.sendRedirect("login.jsp?sessionExpired=true");
        return;
    }
    String role = user.getRole();
    if (!"ADMIN".equals(role) && !"WAREHOUSE_MANAGER".equals(role)) {
        response.sendRedirect("dashboard_admin.jsp");
        return;
    }
    String roleLabel = "ADMIN".equals(role) ? "Administrator" : "Warehouse Manager";
    String dashboardLink = "ADMIN".equals(role) ? "dashboard_admin.jsp" : "dashboard_warehouse.jsp";

    OrderDAO orderDAO = new OrderDAO();
    InventoryDAO inventoryDAO = new InventoryDAO();

    // Direct page load (not forwarded from OrderServlet) -> load ourselves.
    List<CustomerOrder> orders = (List<CustomerOrder>) request.getAttribute("orders");
    if (orders == null) orders = orderDAO.getAllOrders();

    Map<String, Object> summary = (Map<String, Object>) request.getAttribute("summary");
    if (summary == null) summary = orderDAO.getSummary();

    List<InventoryItem> inventory = (List<InventoryItem>) request.getAttribute("inventory");
    if (inventory == null) inventory = inventoryDAO.getAllInventory();

    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null) errorMessage = request.getParameter("error");

    String successMessage = request.getParameter("success");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Order Fulfillment - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag"><%= roleLabel %></span>
            <a class="logout" href="change_password.jsp" style="margin-right:6px;">Change Password</a>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <div>
                <span class="eyebrow">Order Fulfillment</span>
                <h3>Customer Orders</h3>
                <p>Receive customer orders, verify stock, process, and confirm delivery.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> <%= orders.size() %> Orders</div>
        </div>

        <% if (errorMessage != null) { %>
            <div class="alert alert-error" style="margin:0 0 20px;"><%= errorMessage %></div>
        <% } %>
        <% if (successMessage != null) { %>
            <div class="alert alert-success" style="margin:0 0 20px;"><%= successMessage %></div>
        <% } %>

        <!-- OF summary strip -->
        <div class="summary-strip">
            <div class="summary-tile">
                <span class="summary-label">Pending</span>
                <span class="summary-value"><%= summary.get("pending") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Verified</span>
                <span class="summary-value"><%= summary.get("verified") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Processed</span>
                <span class="summary-value"><%= summary.get("processed") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Delivered</span>
                <span class="summary-value"><%= summary.get("delivered") %></span>
            </div>
        </div>

        <div class="panel-grid">
            <!-- OF-01: Receive Customer Order -->
            <div class="panel-card">
                <span class="eyebrow">Receive Order</span>
                <h4 class="panel-title">New Customer Order</h4>

                <form action="OrderServlet" method="post">
                    <input type="hidden" name="action" value="create">

                    <div class="form-group">
                        <label>Customer Name</label>
                        <input type="text" name="customerName" required placeholder="e.g. Chennai Retail Traders">
                    </div>

                    <div class="form-group">
                        <label>Item</label>
                        <select name="itemId" required>
                            <option value="">-- Select item --</option>
                            <% for (InventoryItem item : inventory) { %>
                                <option value="<%= item.getItemId() %>">
                                    <%= item.getItemName() %> (<%= item.getQuantityOnHand() %> in stock)
                                </option>
                            <% } %>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Quantity</label>
                        <input type="number" name="quantity" min="1" required>
                    </div>

                    <button type="submit" class="btn-login" style="width:auto; padding:9px 16px;">Receive Order</button>
                </form>
            </div>

            <!-- OF-02/03/04: order pipeline -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">Order Pipeline</span>
                </div>

                <% if (orders.isEmpty()) { %>
                    <p class="empty-state">No customer orders yet. Receive one to get started.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Order</th>
                                <th>Customer</th>
                                <th>Item</th>
                                <th>Qty</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (CustomerOrder o : orders) {
                                String badgeClass = "badge-" + o.getStatus().toLowerCase();
                            %>
                                <tr>
                                    <td><span class="tag">ORD-<%= o.getOrderId() %></span></td>
                                    <td><%= o.getCustomerName() %></td>
                                    <td><%= o.getItemName() %></td>
                                    <td><%= o.getQuantity() %></td>
                                    <td><span class="badge <%= badgeClass %>"><%= o.getStatus() %></span></td>
                                    <td class="row-actions">
                                        <% if ("PENDING".equals(o.getStatus())) { %>
                                            <form action="OrderServlet" method="post" style="display:inline;">
                                                <input type="hidden" name="action" value="verify">
                                                <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                                                <button type="submit" class="link-btn">Verify</button>
                                            </form>
                                            <form action="OrderServlet" method="post" style="display:inline;">
                                                <input type="hidden" name="action" value="cancel">
                                                <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                                                <button type="submit" class="link-btn">Cancel</button>
                                            </form>
                                        <% } else if ("VERIFIED".equals(o.getStatus())) { %>
                                            <form action="OrderServlet" method="post" style="display:inline;">
                                                <input type="hidden" name="action" value="process">
                                                <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                                                <button type="submit" class="link-btn">Process</button>
                                            </form>
                                            <form action="OrderServlet" method="post" style="display:inline;">
                                                <input type="hidden" name="action" value="cancel">
                                                <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                                                <button type="submit" class="link-btn">Cancel</button>
                                            </form>
                                        <% } else if ("PROCESSED".equals(o.getStatus())) { %>
                                            <form action="OrderServlet" method="post" style="display:inline;">
                                                <input type="hidden" name="action" value="deliver">
                                                <input type="hidden" name="orderId" value="<%= o.getOrderId() %>">
                                                <button type="submit" class="link-btn">Mark Delivered</button>
                                            </form>
                                        <% } else { %>
                                            &mdash;
                                        <% } %>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        </div>

        <p class="demo-note" style="text-align:left; margin-top:24px;">
            <a href="<%= dashboardLink %>">&larr; Back to Dashboard</a>
        </p>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
