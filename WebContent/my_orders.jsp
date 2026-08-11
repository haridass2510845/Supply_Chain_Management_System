<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.PurchaseOrder" %>
<%@ page import="com.scms.dao.PurchaseOrderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    User user = (session != null) ? (User) session.getAttribute("user") : null;
    if (user == null) {
        response.sendRedirect("login.jsp?sessionExpired=true");
        return;
    }
    if (!"SUPPLIER".equals(user.getRole())) {
        response.sendRedirect("dashboard_admin.jsp");
        return;
    }

    PurchaseOrderDAO poDAO = new PurchaseOrderDAO();

    List<PurchaseOrder> orders = (List<PurchaseOrder>) request.getAttribute("orders");
    Map<String, Object> performance = (Map<String, Object>) request.getAttribute("performance");

    // Direct page load (not forwarded from SupplierPortalServlet) -> load ourselves.
    if (user.hasLinkedSupplier() && orders == null) {
        orders = poDAO.getOrdersBySupplier(user.getSupplierId());
        performance = poDAO.getPerformanceForSupplier(user.getSupplierId());
    }

    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null) errorMessage = request.getParameter("error");

    String successMessage = request.getParameter("success");
%>
<!DOCTYPE html>
<html>
<head>
    <title>My Purchase Orders - SCMS</title>
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
                <h3>My Purchase Orders</h3>
                <p>View orders assigned to your company, update shipment status, and confirm delivery.</p>
            </div>
            <% if (user.hasLinkedSupplier()) { %>
                <div class="status-pill"><span class="dot"></span> <%= orders.size() %> Total</div>
            <% } %>
        </div>

        <% if (!user.hasLinkedSupplier()) { %>
            <div class="alert alert-error">
                Your account isn't linked to a supplier company record yet, so there are no
                purchase orders to show. Please contact your administrator to have your login
                linked to your company's supplier record.
            </div>
        <% } else { %>

            <% if (errorMessage != null) { %>
                <div class="alert alert-error" style="margin:0 0 20px;"><%= errorMessage %></div>
            <% } %>
            <% if (successMessage != null) { %>
                <div class="alert alert-success" style="margin:0 0 20px;"><%= successMessage %></div>
            <% } %>

            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">All My Orders</span>
                </div>

                <% if (orders.isEmpty()) { %>
                    <p class="empty-state">No purchase orders have been raised against your company yet.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Item</th>
                                <th>Qty</th>
                                <th>Total</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (PurchaseOrder po : orders) {
                                String badgeClass = "badge-" + po.getStatus().toLowerCase();
                            %>
                                <tr>
                                    <td><span class="tag">PO-<%= po.getPoId() %></span></td>
                                    <td><%= po.getItemName() %></td>
                                    <td><%= po.getQuantity() %></td>
                                    <td>&#8377;<%= po.getTotalAmount() %></td>
                                    <td><span class="badge <%= badgeClass %>"><%= po.getStatus() %></span></td>
                                    <td class="row-actions">
                                        <% if (po.isApproved()) { %>
                                            <a href="SupplierPortalServlet?action=ship&poId=<%= po.getPoId() %>"
                                               onclick="return confirm('Mark PO-<%= po.getPoId() %> as shipped?');">Mark Shipped</a>
                                        <% } else if (po.isShipped()) { %>
                                            <a href="SupplierPortalServlet?action=deliver&poId=<%= po.getPoId() %>"
                                               onclick="return confirm('Confirm delivery for PO-<%= po.getPoId() %>?');">Confirm Delivered</a>
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

            <!-- PO-10: My Performance History -->
            <div class="section-heading" style="margin-top:32px;" id="performance">
                <span class="eyebrow">My Performance History</span>
            </div>
            <div class="summary-strip">
                <div class="summary-tile">
                    <span class="summary-label">Total Orders</span>
                    <span class="summary-value"><%= performance.get("totalOrders") %></span>
                </div>
                <div class="summary-tile">
                    <span class="summary-label">Delivered</span>
                    <span class="summary-value"><%= performance.get("fulfilledOrders") %></span>
                </div>
                <div class="summary-tile">
                    <span class="summary-label">Cancelled</span>
                    <span class="summary-value"><%= performance.get("cancelledOrders") %></span>
                </div>
                <div class="summary-tile">
                    <span class="summary-label">Total Order Value</span>
                    <span class="summary-value">&#8377;<%= performance.get("totalValue") %></span>
                </div>
            </div>
        <% } %>

        <p class="demo-note" style="text-align:left; margin-top:24px;">
            <a href="dashboard_supplier.jsp">&larr; Back to Dashboard</a>
        </p>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
