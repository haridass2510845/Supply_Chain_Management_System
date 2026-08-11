<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.InventoryItem" %>
<%@ page import="com.scms.model.InventoryTransaction" %>
<%@ page import="com.scms.model.PurchaseOrder" %>
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

    InventoryDAO inventoryDAO = new InventoryDAO();

    // Direct page load (not forwarded from WarehouseServlet) -> load ourselves.
    List<InventoryItem> inventory = (List<InventoryItem>) request.getAttribute("inventory");
    if (inventory == null) inventory = inventoryDAO.getAllInventory();

    List<PurchaseOrder> receivablePOs = (List<PurchaseOrder>) request.getAttribute("receivablePOs");
    if (receivablePOs == null) receivablePOs = inventoryDAO.getReceivablePOs();

    List<InventoryTransaction> transactions = (List<InventoryTransaction>) request.getAttribute("transactions");
    if (transactions == null) transactions = inventoryDAO.getRecentTransactions(15);

    Map<String, Object> summary = (Map<String, Object>) request.getAttribute("summary");
    if (summary == null) summary = inventoryDAO.getSummary();

    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null) errorMessage = request.getParameter("error");

    String successMessage = request.getParameter("success");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Warehouse - SCMS</title>
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
                <span class="eyebrow">Inventory &amp; Storage</span>
                <h3>Warehouse</h3>
                <p>Receive completed orders into stock, track inventory, and dispatch goods.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> <%= summary.get("totalItems") %> Items</div>
        </div>

        <% if (errorMessage != null) { %>
            <div class="alert alert-error" style="margin:0 0 20px;"><%= errorMessage %></div>
        <% } %>
        <% if (successMessage != null) { %>
            <div class="alert alert-success" style="margin:0 0 20px;"><%= successMessage %></div>
        <% } %>

        <!-- WH-05: Warehouse Reports -->
        <div class="summary-strip">
            <div class="summary-tile">
                <span class="summary-label">Items Tracked</span>
                <span class="summary-value"><%= summary.get("totalItems") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Total Units on Hand</span>
                <span class="summary-value"><%= summary.get("totalUnits") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Low Stock Alerts</span>
                <span class="summary-value"><%= summary.get("lowStockCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Awaiting Receipt</span>
                <span class="summary-value"><%= receivablePOs.size() %></span>
            </div>
        </div>

        <div class="panel-grid">
            <!-- WH-01: Receive Goods -->
            <div class="panel-card">
                <span class="eyebrow">Receive Goods</span>
                <h4 class="panel-title">Completed Orders Awaiting Receipt</h4>

                <% if (receivablePOs.isEmpty()) { %>
                    <p class="empty-state">No completed purchase orders waiting to be received right now.</p>
                <% } else { %>
                    <% for (PurchaseOrder po : receivablePOs) { %>
                        <form action="WarehouseServlet" method="post" style="margin-bottom:14px; padding-bottom:14px; border-bottom:1px solid var(--line);">
                            <input type="hidden" name="action" value="receive">
                            <input type="hidden" name="poId" value="<%= po.getPoId() %>">
                            <strong><span class="tag">PO-<%= po.getPoId() %></span> <%= po.getItemName() %></strong><br>
                            <span style="font-size:13px; color:var(--slate);">
                                <%= po.getQuantity() %> units &middot; from <%= po.getSupplierName() %>
                            </span><br>
                            <button type="submit" class="btn-login" style="margin-top:8px; padding:8px 14px; width:auto;">Receive Into Stock</button>
                        </form>
                    <% } %>
                <% } %>
            </div>

            <!-- WH-02: Store Inventory -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">Current Inventory</span>
                </div>

                <% if (inventory.isEmpty()) { %>
                    <p class="empty-state">No inventory recorded yet. Receive a completed order to get started.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th>On Hand</th>
                                <th>Reorder Level</th>
                                <th>Location</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (InventoryItem item : inventory) { %>
                                <tr>
                                    <td><%= item.getItemName() %></td>
                                    <td><%= item.getQuantityOnHand() %></td>
                                    <td><%= item.getReorderLevel() %></td>
                                    <td><%= item.getLocation() != null ? item.getLocation() : "&mdash;" %></td>
                                    <td>
                                        <% if (item.isLowStock()) { %>
                                            <span class="badge badge-low">Low Stock</span>
                                        <% } else { %>
                                            <span class="badge badge-ok">OK</span>
                                        <% } %>
                                    </td>
                                    <td class="row-actions">
                                        <a href="#dispatch-<%= item.getItemId() %>"
                                           onclick="document.getElementById('dispatch-<%= item.getItemId() %>').style.display='block'; return false;">Dispatch</a>
                                        <a href="#adjust-<%= item.getItemId() %>"
                                           onclick="document.getElementById('adjust-<%= item.getItemId() %>').style.display='block'; return false;">Adjust</a>
                                    </td>
                                </tr>
                                <tr id="dispatch-<%= item.getItemId() %>" style="display:none;">
                                    <td colspan="6" style="background:var(--paper);">
                                        <!-- WH-04: Dispatch Goods -->
                                        <form action="WarehouseServlet" method="post" style="display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; padding:10px 0;">
                                            <input type="hidden" name="action" value="dispatch">
                                            <input type="hidden" name="itemId" value="<%= item.getItemId() %>">
                                            <div class="form-group" style="margin:0;">
                                                <label>Dispatch Qty</label>
                                                <input type="number" name="quantity" min="1" max="<%= item.getQuantityOnHand() %>" required style="width:100px;">
                                            </div>
                                            <div class="form-group" style="margin:0; flex:1; min-width:160px;">
                                                <label>Remarks</label>
                                                <input type="text" name="remarks" placeholder="e.g. Outbound to Warehouse B">
                                            </div>
                                            <button type="submit" class="btn-login" style="width:auto; padding:9px 16px;">Confirm Dispatch</button>
                                        </form>
                                    </td>
                                </tr>
                                <tr id="adjust-<%= item.getItemId() %>" style="display:none;">
                                    <td colspan="6" style="background:var(--paper);">
                                        <!-- WH-03: Update Stock -->
                                        <form action="WarehouseServlet" method="post" style="display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; padding:10px 0;">
                                            <input type="hidden" name="action" value="adjust">
                                            <input type="hidden" name="itemId" value="<%= item.getItemId() %>">
                                            <div class="form-group" style="margin:0;">
                                                <label>Adjustment (+/-)</label>
                                                <input type="number" name="delta" required style="width:100px;" placeholder="e.g. -5 or 20">
                                            </div>
                                            <div class="form-group" style="margin:0; flex:1; min-width:160px;">
                                                <label>Reason</label>
                                                <input type="text" name="remarks" placeholder="e.g. Physical count correction">
                                            </div>
                                            <button type="submit" class="btn-login" style="width:auto; padding:9px 16px;">Apply Adjustment</button>
                                        </form>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        </div>

        <!-- WH-05: Recent Activity -->
        <div class="section-heading" style="margin-top:32px;">
            <span class="eyebrow">Recent Warehouse Activity</span>
        </div>
        <div class="panel-card panel-card-wide">
            <% if (transactions.isEmpty()) { %>
                <p class="empty-state">No stock movements recorded yet.</p>
            <% } else { %>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Item</th>
                            <th>Type</th>
                            <th>Qty</th>
                            <th>By</th>
                            <th>Remarks</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (InventoryTransaction t : transactions) {
                            String badgeClass = "badge-" + t.getTxnType().toLowerCase();
                        %>
                            <tr>
                                <td><%= t.getItemName() %></td>
                                <td><span class="badge <%= badgeClass %>"><%= t.getTxnType() %></span></td>
                                <td><%= t.getQuantity() %></td>
                                <td><%= t.getPerformedBy() %></td>
                                <td><%= t.getRemarks() != null ? t.getRemarks() : "&mdash;" %></td>
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
