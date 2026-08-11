<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.Supplier" %>
<%@ page import="com.scms.dao.SupplierDAO" %>
<%@ page import="java.util.List" %>
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

    // Direct page load (not forwarded from SupplierServlet) -> load the full list ourselves.
    List<Supplier> suppliers = (List<Supplier>) request.getAttribute("suppliers");
    if (suppliers == null) {
        suppliers = new SupplierDAO().getAllSuppliers();
    }

    Supplier editSupplier = (Supplier) request.getAttribute("editSupplier");

    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null) errorMessage = request.getParameter("error");

    String successMessage = request.getParameter("success");

    String keyword = (String) request.getAttribute("keyword");
    if (keyword == null) keyword = "";
%>
<!DOCTYPE html>
<html>
<head>
    <title>Supplier Management - SCMS</title>
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
                <span class="eyebrow">MOD-02 &middot; Supplier Management</span>
                <h3>Suppliers</h3>
                <p>Add, update, delete, and search supplier records (SRS FR2).</p>
            </div>
            <div class="status-pill"><span class="dot"></span> <%= suppliers.size() %> On File</div>
        </div>

        <% if (errorMessage != null) { %>
            <div class="alert alert-error" style="margin:0 0 20px;"><%= errorMessage %></div>
        <% } %>
        <% if (successMessage != null) { %>
            <div class="alert alert-success" style="margin:0 0 20px;"><%= successMessage %></div>
        <% } %>

        <div class="panel-grid">
            <!-- Add / Edit form -->
            <div class="panel-card">
                <span class="eyebrow"><%= editSupplier != null ? "Update Supplier" : "Add Supplier" %></span>
                <h4 class="panel-title"><%= editSupplier != null ? "Edit Supplier #" + editSupplier.getSupplierId() : "New Supplier" %></h4>

                <form action="SupplierServlet" method="post">
                    <% if (editSupplier != null) { %>
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="supplierId" value="<%= editSupplier.getSupplierId() %>">
                    <% } else { %>
                        <input type="hidden" name="action" value="add">
                    <% } %>

                    <div class="form-group">
                        <label for="supplierName">Supplier Name</label>
                        <input type="text" id="supplierName" name="supplierName" required
                               value="<%= editSupplier != null ? editSupplier.getSupplierName() : "" %>">
                    </div>
                    <div class="form-group">
                        <label for="contactNo">Contact Number</label>
                        <input type="text" id="contactNo" name="contactNo" required
                               value="<%= editSupplier != null ? editSupplier.getContactNo() : "" %>">
                    </div>
                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email"
                               value="<%= editSupplier != null && editSupplier.getEmail() != null ? editSupplier.getEmail() : "" %>">
                    </div>
                    <div class="form-group">
                        <label for="address">Address</label>
                        <input type="text" id="address" name="address"
                               value="<%= editSupplier != null && editSupplier.getAddress() != null ? editSupplier.getAddress() : "" %>">
                    </div>

                    <button type="submit" class="btn-login">
                        <%= editSupplier != null ? "Update Supplier" : "Register Supplier" %>
                    </button>
                    <% if (editSupplier != null) { %>
                        <a class="btn-secondary" href="suppliers.jsp">Cancel</a>
                    <% } %>
                </form>
            </div>

            <!-- List + search -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">All Suppliers</span>
                    <form action="SupplierServlet" method="get" class="search-form">
                        <input type="hidden" name="action" value="search">
                        <input type="text" name="keyword" placeholder="Search name, phone, or email"
                               value="<%= keyword %>">
                        <button type="submit" class="btn-search">Search</button>
                        <% if (!keyword.isEmpty()) { %>
                            <a class="btn-secondary" href="suppliers.jsp">Clear</a>
                        <% } %>
                    </form>
                </div>

                <% if (suppliers.isEmpty()) { %>
                    <p class="empty-state">No suppliers found. Add one using the form on the left.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Contact No.</th>
                                <th>Email</th>
                                <th>Address</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Supplier s : suppliers) { %>
                                <tr>
                                    <td><span class="tag"><%= s.getSupplierId() %></span></td>
                                    <td><%= s.getSupplierName() %></td>
                                    <td><%= s.getContactNo() %></td>
                                    <td><%= s.getEmail() != null ? s.getEmail() : "&mdash;" %></td>
                                    <td><%= s.getAddress() != null ? s.getAddress() : "&mdash;" %></td>
                                    <td class="row-actions">
                                        <a href="SupplierServlet?action=edit&supplierId=<%= s.getSupplierId() %>">Edit</a>
                                        <a href="SupplierServlet?action=delete&supplierId=<%= s.getSupplierId() %>"
                                           class="danger-link"
                                           onclick="return confirm('Delete supplier &quot;<%= s.getSupplierName() %>&quot;? This cannot be undone.');">Delete</a>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        </div>

        <p class="demo-note" style="text-align:left; margin-top:24px;">
            <a href="dashboard_admin.jsp">&larr; Back to Dashboard</a>
        </p>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
