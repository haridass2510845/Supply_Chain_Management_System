<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.Shipment" %>
<%@ page import="com.scms.dao.LogisticsDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    User user = (session != null) ? (User) session.getAttribute("user") : null;
    if (user == null) {
        response.sendRedirect("login.jsp?sessionExpired=true");
        return;
    }
    String role = user.getRole();
    if (!"ADMIN".equals(role) && !"LOGISTICS_STAFF".equals(role)) {
        response.sendRedirect("dashboard_admin.jsp");
        return;
    }
    String roleLabel = "ADMIN".equals(role) ? "Administrator" : "Logistics Staff";
    String dashboardLink = "ADMIN".equals(role) ? "dashboard_admin.jsp" : "dashboard_logistics.jsp";

    LogisticsDAO logisticsDAO = new LogisticsDAO();

    // Direct page load (not forwarded from LogisticsServlet) -> load ourselves.
    List<Shipment> shipments = (List<Shipment>) request.getAttribute("shipments");
    if (shipments == null) shipments = logisticsDAO.getAllShipments();

    List<Map<String, Object>> unassignedDispatches = (List<Map<String, Object>>) request.getAttribute("unassignedDispatches");
    if (unassignedDispatches == null) unassignedDispatches = logisticsDAO.getUnassignedDispatches();

    Map<String, Object> summary = (Map<String, Object>) request.getAttribute("summary");
    if (summary == null) summary = logisticsDAO.getSummary();

    String statusFilter = (String) request.getAttribute("statusFilter");
    if (statusFilter == null) statusFilter = "ALL";

    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null) errorMessage = request.getParameter("error");

    String successMessage = request.getParameter("success");
%>
<!DOCTYPE html>
<html>
<head>
    <title>Logistics - SCMS</title>
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
                <span class="eyebrow">Transport &amp; Delivery</span>
                <h3>Logistics</h3>
                <p>Assign deliveries for dispatched goods, track shipments, and confirm delivery.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> <%= summary.get("inTransitCount") %> In Transit</div>
        </div>

        <% if (errorMessage != null) { %>
            <div class="alert alert-error" style="margin:0 0 20px;"><%= errorMessage %></div>
        <% } %>
        <% if (successMessage != null) { %>
            <div class="alert alert-success" style="margin:0 0 20px;"><%= successMessage %></div>
        <% } %>

        <div class="summary-strip">
            <div class="summary-tile">
                <span class="summary-label">Assigned</span>
                <span class="summary-value"><%= summary.get("assignedCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">In Transit</span>
                <span class="summary-value"><%= summary.get("inTransitCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Delivered</span>
                <span class="summary-value"><%= summary.get("deliveredCount") %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Awaiting Assignment</span>
                <span class="summary-value"><%= unassignedDispatches.size() %></span>
            </div>
        </div>

        <div class="panel-grid">
            <!-- LG-01: Assign Delivery -->
            <div class="panel-card">
                <span class="eyebrow">Assign Delivery</span>
                <h4 class="panel-title">Dispatches Awaiting Assignment</h4>

                <% if (unassignedDispatches.isEmpty()) { %>
                    <p class="empty-state">No dispatched goods are waiting on a delivery assignment right now.</p>
                <% } else { %>
                    <% for (Map<String, Object> d : unassignedDispatches) { %>
                        <form action="LogisticsServlet" method="post" style="margin-bottom:16px; padding-bottom:16px; border-bottom:1px solid var(--line);">
                            <input type="hidden" name="action" value="assign">
                            <input type="hidden" name="txnId" value="<%= d.get("txnId") %>">
                            <strong><%= d.get("itemName") %></strong>
                            <span style="font-size:13px; color:var(--slate);"> &middot; <%= d.get("quantity") %> units</span>

                            <div class="form-group" style="margin-top:10px;">
                                <label>Destination</label>
                                <input type="text" name="destination" required placeholder="e.g. Warehouse B, Coimbatore">
                            </div>
                            <div class="form-group">
                                <label>Carrier / Driver</label>
                                <input type="text" name="carrierName" required placeholder="e.g. Ramesh Logistics">
                            </div>
                            <div class="form-group">
                                <label>Vehicle No. (optional)</label>
                                <input type="text" name="vehicleNo" placeholder="e.g. TN 07 AB 1234">
                            </div>
                            <button type="submit" class="btn-login" style="width:auto; padding:9px 16px;">Assign Delivery</button>
                        </form>
                    <% } %>
                <% } %>
            </div>

            <!-- LG-02: Track Shipment -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">All Shipments</span>
                    <div class="filter-tabs">
                        <a href="LogisticsServlet?status=ALL"        class="<%= "ALL".equals(statusFilter) ? "active" : "" %>">All</a>
                        <a href="LogisticsServlet?status=ASSIGNED"   class="<%= "ASSIGNED".equals(statusFilter) ? "active" : "" %>">Assigned</a>
                        <a href="LogisticsServlet?status=IN_TRANSIT" class="<%= "IN_TRANSIT".equals(statusFilter) ? "active" : "" %>">In Transit</a>
                        <a href="LogisticsServlet?status=DELIVERED"  class="<%= "DELIVERED".equals(statusFilter) ? "active" : "" %>">Delivered</a>
                    </div>
                </div>

                <% if (shipments.isEmpty()) { %>
                    <p class="empty-state">No shipments found for this filter.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Item</th>
                                <th>Destination</th>
                                <th>Carrier</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Shipment s : shipments) {
                                String badgeClass = "badge-" + s.getStatus().toLowerCase();
                            %>
                                <tr>
                                    <td><span class="tag">SH-<%= s.getShipmentId() %></span></td>
                                    <td><%= s.getItemName() %> <span style="color:var(--slate); font-size:12px;">&times;<%= s.getQuantity() %></span></td>
                                    <td><%= s.getDestination() %></td>
                                    <td><%= s.getCarrierName() %><% if (s.getVehicleNo() != null && !s.getVehicleNo().isEmpty()) { %> <span style="color:var(--slate); font-size:12px;">(<%= s.getVehicleNo() %>)</span><% } %></td>
                                    <td><span class="badge <%= badgeClass %>"><%= s.getStatus().replace("_", " ") %></span></td>
                                    <td class="row-actions">
                                        <!-- LG-03: Update Delivery Status -->
                                        <% if (s.isAssigned()) { %>
                                            <a href="LogisticsServlet?action=inTransit&shipmentId=<%= s.getShipmentId() %>"
                                               onclick="return confirm('Mark SH-<%= s.getShipmentId() %> as in transit?');">Mark In Transit</a>
                                        <% } else if (s.isInTransit()) { %>
                                            <!-- LG-04: Confirm Delivery -->
                                            <a href="LogisticsServlet?action=deliver&shipmentId=<%= s.getShipmentId() %>"
                                               onclick="return confirm('Confirm delivery for SH-<%= s.getShipmentId() %>?');">Confirm Delivered</a>
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
