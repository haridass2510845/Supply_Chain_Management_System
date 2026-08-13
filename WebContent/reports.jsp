<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.Supplier" %>
<%@ page import="com.scms.model.InventoryItem" %>
<%@ page import="com.scms.model.Shipment" %>
<%@ page import="com.scms.dao.SupplierDAO" %>
<%@ page import="com.scms.dao.PurchaseOrderDAO" %>
<%@ page import="com.scms.dao.InventoryDAO" %>
<%@ page import="com.scms.dao.LogisticsDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.ArrayList" %>
<%
    User user = (session != null) ? (User) session.getAttribute("user") : null;
    if (user == null) {
        response.sendRedirect("login.jsp?sessionExpired=true");
        return;
    }
    if (!"ADMIN".equals(user.getRole())) {
        response.sendRedirect("dashboard_admin.jsp");
        return;
    }

    SupplierDAO supplierDAO = new SupplierDAO();
    PurchaseOrderDAO poDAO = new PurchaseOrderDAO();
    InventoryDAO inventoryDAO = new InventoryDAO();
    LogisticsDAO logisticsDAO = new LogisticsDAO();

    List<Supplier> suppliers = supplierDAO.getAllSuppliers();
    List<Map<String, Object>> supplierPerformance = poDAO.getSupplierPerformance();
    Map<String, Object> poSummary = poDAO.getSummary();
    Map<String, Object> inventorySummary = inventoryDAO.getSummary();
    Map<String, Object> logisticsSummary = logisticsDAO.getSummary();

    List<InventoryItem> lowStockItems = new ArrayList<>();
    for (InventoryItem item : inventoryDAO.getAllInventory()) {
        if (item.isLowStock()) {
            lowStockItems.add(item);
        }
    }

    List<Shipment> inTransitShipments = logisticsDAO.getShipmentsByStatus("IN_TRANSIT");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Reports - SCMS</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="topbar">
        <div class="brand-mark"><span class="crate"></span> SCMS &middot; Control Tower</div>
        <div class="user-info">
            <span><%= user.getFullName() %></span>
            <span class="role-tag">Administrator</span>
            <a class="logout" href="change_password.jsp" style="margin-right:6px;">Change Password</a>
            <a class="logout" href="LogoutServlet">Logout</a>
        </div>
    </div>

    <div class="container">
        <div class="welcome-card">
            <div>
                <span class="eyebrow">Cross-Module Reporting</span>
                <h3>Reports</h3>
                <p>Supplier, procurement, inventory, and logistics activity across the whole system.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> <%= suppliers.size() %> Suppliers</div>
        </div>

        <!-- System-wide summary -->
        <div class="summary-strip">
            <div class="summary-tile">
                <span class="summary-label">Suppliers</span>
                <span class="summary-value"><%= suppliers.size() %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Purchase Orders</span>
                <span class="summary-value"><%= poSummary.get("totalOrders") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Inventory Items</span>
                <span class="summary-value"><%= inventorySummary.get("totalItems") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Shipments In Transit</span>
                <span class="summary-value"><%= logisticsSummary.get("inTransitCount") %></span>
            </div>
        </div>

        <!-- Procurement Report -->
        <div class="section-heading" style="margin-top:8px;">
            <span class="eyebrow">Procurement Report</span>
        </div>
        <div class="summary-strip">
            <div class="summary-tile">
                <span class="summary-label">Pending</span>
                <span class="summary-value"><%= poSummary.get("pendingCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Approved</span>
                <span class="summary-value"><%= poSummary.get("approvedCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Cancelled</span>
                <span class="summary-value"><%= poSummary.get("cancelledCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Approved Spend</span>
                <span class="summary-value">&#8377;<%= poSummary.get("totalSpend") %></span>
            </div>
        </div>

        <!-- Supplier Performance Report -->
        <div class="section-heading" style="margin-top:8px;">
            <span class="eyebrow">Supplier Performance Report</span>
        </div>
        <div class="panel-card panel-card-wide">
            <% if (supplierPerformance.isEmpty()) { %>
                <p class="empty-state">No supplier activity recorded yet.</p>
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
                        <% for (Map<String, Object> row : supplierPerformance) { %>
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

        <div class="panel-grid" style="margin-top:24px;">
            <!-- Inventory / Warehouse Report -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">Inventory Report &mdash; Low Stock Alerts</span>
                </div>
                <% if (lowStockItems.isEmpty()) { %>
                    <p class="empty-state">No items are currently below their reorder level.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th>On Hand</th>
                                <th>Reorder Level</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (InventoryItem item : lowStockItems) { %>
                                <tr>
                                    <td><%= item.getItemName() %></td>
                                    <td><%= item.getQuantityOnHand() %></td>
                                    <td><%= item.getReorderLevel() %></td>
                                    <td><span class="badge badge-low">Low Stock</span></td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
                <p style="margin-top:14px; font-size:13px; color:var(--slate);">
                    <%= inventorySummary.get("totalUnits") %> total units on hand across <%= inventorySummary.get("totalItems") %> items.
                </p>
            </div>

            <!-- Logistics Report -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">Logistics Report &mdash; In Transit</span>
                </div>
                <% if (inTransitShipments.isEmpty()) { %>
                    <p class="empty-state">No shipments are currently in transit.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Item</th>
                                <th>Destination</th>
                                <th>Carrier</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Shipment s : inTransitShipments) { %>
                                <tr>
                                    <td><span class="tag">SH-<%= s.getShipmentId() %></span></td>
                                    <td><%= s.getItemName() %></td>
                                    <td><%= s.getDestination() %></td>
                                    <td><%= s.getCarrierName() %></td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
                <p style="margin-top:14px; font-size:13px; color:var(--slate);">
                    <%= logisticsSummary.get("assignedCount") %> assigned &middot;
                    <%= logisticsSummary.get("inTransitCount") %> in transit &middot;
                    <%= logisticsSummary.get("deliveredCount") %> delivered.
                </p>
            </div>
        </div>

        <p class="demo-note" style="text-align:left; margin-top:24px;">
            <a href="dashboard_admin.jsp">&larr; Back to Dashboard</a>
        </p>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
