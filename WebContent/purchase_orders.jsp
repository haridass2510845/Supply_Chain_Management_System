<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.Supplier" %>
<%@ page import="com.scms.model.PurchaseOrder" %>
<%@ page import="com.scms.dao.SupplierDAO" %>
<%@ page import="com.scms.dao.PurchaseOrderDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    User user = (session != null) ? (User) session.getAttribute("user") : null;
    if (user == null) {
        response.sendRedirect("login.jsp?sessionExpired=true");
        return;
    }
    String role = user.getRole();
    if (!"ADMIN".equals(role) && !"PROCUREMENT_MANAGER".equals(role)) {
        response.sendRedirect("dashboard_admin.jsp");
        return;
    }
    String roleLabel = "ADMIN".equals(role) ? "Administrator" : "Procurement Manager";
    String dashboardLink = "ADMIN".equals(role) ? "dashboard_admin.jsp" : "dashboard_procurement.jsp";

    PurchaseOrderDAO poDAO = new PurchaseOrderDAO();

    // Direct page load (not forwarded from PurchaseOrderServlet) -> load everything ourselves.
    List<Supplier> suppliers = (List<Supplier>) request.getAttribute("suppliers");
    if (suppliers == null) suppliers = new SupplierDAO().getAllSuppliers();

    List<PurchaseOrder> orders = (List<PurchaseOrder>) request.getAttribute("orders");
    if (orders == null) orders = poDAO.getAllOrders();

    List<Map<String, Object>> performance = (List<Map<String, Object>>) request.getAttribute("performance");
    if (performance == null) performance = poDAO.getSupplierPerformance();

    Map<String, Object> summary = (Map<String, Object>) request.getAttribute("summary");
    if (summary == null) summary = poDAO.getSummary();

    String statusFilter = (String) request.getAttribute("statusFilter");
    if (statusFilter == null) statusFilter = "ALL";

    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null) errorMessage = request.getParameter("error");

    String successMessage = request.getParameter("success");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Purchase Orders - SCMS</title>
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
                <span class="eyebrow">MOD-03 &middot; Procurement Management</span>
                <h3>Purchase Orders</h3>
                <p>Create, approve, cancel, and track purchase orders raised against suppliers.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> <%= summary.get("totalOrders") %> Total</div>
        </div>

        <% if (errorMessage != null) { %>
            <div class="alert alert-error" style="margin:0 0 20px;"><%= errorMessage %></div>
        <% } %>
        <% if (successMessage != null) { %>
            <div class="alert alert-success" style="margin:0 0 20px;"><%= successMessage %></div>
        <% } %>

        <!-- PO-06: Procurement Reports -->
        <div class="summary-strip">
            <div class="summary-tile">
                <span class="summary-label">Pending</span>
                <span class="summary-value"><%= summary.get("pendingCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Approved</span>
                <span class="summary-value"><%= summary.get("approvedCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Cancelled</span>
                <span class="summary-value"><%= summary.get("cancelledCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Approved Spend</span>
                <span class="summary-value">&#8377;<%= summary.get("totalSpend") %></span>
            </div>
        </div>

        <div class="panel-grid">
            <!-- PO-01: Create Purchase Order -->
            <div class="panel-card">
                <span class="eyebrow">Raise Order</span>
                <h4 class="panel-title">New Purchase Order</h4>

                <% if (suppliers.isEmpty()) { %>
                    <p class="empty-state">No suppliers on file yet. Add one under Supplier Management first.</p>
                <% } else { %>
                    <form action="PurchaseOrderServlet" method="post">
                        <div class="form-group">
                            <label for="supplierId">Supplier</label>
                            <select id="supplierId" name="supplierId" required>
                                <option value="">-- Select supplier --</option>
                                <% for (Supplier s : suppliers) { %>
                                    <option value="<%= s.getSupplierId() %>"><%= s.getSupplierName() %></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="itemName">Item</label>
                            <input type="text" id="itemName" name="itemName" required placeholder="e.g. Steel Rods (12mm)">
                        </div>
                        <div class="form-group">
                            <label for="quantity">Quantity</label>
                            <input type="number" id="quantity" name="quantity" min="1" required>
                        </div>
                        <div class="form-group">
                            <label for="unitPrice">Unit Price (&#8377;)</label>
                            <input type="number" id="unitPrice" name="unitPrice" min="0.01" step="0.01" required>
                        </div>

                        <button type="submit" class="btn-login">Raise Purchase Order</button>
                    </form>
                <% } %>
            </div>

            <!-- PO-04: Track Status -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">All Purchase Orders</span>
                    <div class="filter-tabs">
                        <a href="PurchaseOrderServlet?action=filter&status=ALL"       class="<%= "ALL".equals(statusFilter) ? "active" : "" %>">All</a>
                        <a href="PurchaseOrderServlet?action=filter&status=PENDING"   class="<%= "PENDING".equals(statusFilter) ? "active" : "" %>">Pending</a>
                        <a href="PurchaseOrderServlet?action=filter&status=APPROVED"  class="<%= "APPROVED".equals(statusFilter) ? "active" : "" %>">Approved</a>
                        <a href="PurchaseOrderServlet?action=filter&status=CANCELLED" class="<%= "CANCELLED".equals(statusFilter) ? "active" : "" %>">Cancelled</a>
                    </div>
                </div>

                <% if (orders.isEmpty()) { %>
                    <p class="empty-state">No purchase orders found for this filter.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Supplier</th>
                                <th>Item</th>
                                <th>Qty</th>
                                <th>Unit Price</th>
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
                                    <td><%= po.getSupplierName() %></td>
                                    <td><%= po.getItemName() %></td>
                                    <td><%= po.getQuantity() %></td>
                                    <td>&#8377;<%= po.getUnitPrice() %></td>
                                    <td>&#8377;<%= po.getTotalAmount() %></td>
                                    <td><span class="badge <%= badgeClass %>"><%= po.getStatus() %></span></td>
                                    <td class="row-actions">
                                        <% if (po.isPending()) { %>
                                            <a href="PurchaseOrderServlet?action=approve&poId=<%= po.getPoId() %>"
                                               onclick="return confirm('Approve PO-<%= po.getPoId() %> for <%= po.getSupplierName() %>?');">Approve</a>
                                            <a href="PurchaseOrderServlet?action=cancel&poId=<%= po.getPoId() %>"
                                               class="danger-link"
                                               onclick="return confirm('Cancel PO-<%= po.getPoId() %>? This cannot be undone.');">Cancel</a>
                                        <% } else if ("APPROVED".equals(po.getStatus())) { %>
                                            <a href="PurchaseOrderServlet?action=cancel&poId=<%= po.getPoId() %>"
                                               class="danger-link"
                                               onclick="return confirm('Cancel PO-<%= po.getPoId() %>? This cannot be undone.');">Cancel</a>
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

        <!-- PO-05: Supplier Performance -->
        <div class="section-heading" style="margin-top:32px;" id="supplier-performance">
            <span class="eyebrow">MOD-03 &middot; Supplier Performance</span>
        </div>
        <div class="panel-card panel-card-wide">
            <% if (performance.isEmpty()) { %>
                <p class="empty-state">No supplier activity yet.</p>
            <% } else { %>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Supplier</th>
                            <th>Total Orders</th>
                            <th>Fulfilled</th>
                            <th>Cancelled</th>
                            <th>Total Order Value</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Map<String, Object> row : performance) { %>
                            <tr>
                                <td><%= row.get("supplierName") %></td>
                                <td><%= row.get("totalOrders") %></td>
                                <td><%= row.get("fulfilledOrders") %></td>
                                <td><%= row.get("cancelledOrders") %></td>
                                <td>&#8377;<%= row.get("totalValue") %></td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } %>
        </div>

        <p class="demo-note" style="text-align:left; margin-top:24px;">
            <a href="<%= dashboardLink %>">&larr; Back to Dashboard</a>
        </p>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
