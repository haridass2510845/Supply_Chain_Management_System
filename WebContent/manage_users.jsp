<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.Supplier" %>
<%@ page import="com.scms.dao.UserDAO" %>
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

    // Direct page load (not forwarded from UserServlet) -> load the full list ourselves.
    List<User> users = (List<User>) request.getAttribute("users");
    if (users == null) {
        users = new UserDAO().getAllUsers();
    }

    User editUser = (User) request.getAttribute("editUser");

    // Direct page load -> load the supplier list ourselves too (used by the
    // "Linked Supplier" field, shown only when the Supplier role is picked).
    List<Supplier> suppliers = (List<Supplier>) request.getAttribute("suppliers");
    if (suppliers == null) {
        suppliers = new SupplierDAO().getAllSuppliers();
    }

    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage == null) errorMessage = request.getParameter("error");

    String successMessage = request.getParameter("success");

    String keyword = (String) request.getAttribute("keyword");
    if (keyword == null) keyword = "";
%>
<!DOCTYPE html>
<html>
<head>
    <title>Manage Users - SCMS</title>
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
                <span class="eyebrow">Manage Users</span>
                <h3>User Accounts</h3>
                <p>Create, update, activate/deactivate, and remove accounts across all roles.</p>
            </div>
            <div class="status-pill"><span class="dot"></span> <%= users.size() %> Accounts</div>
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
                <span class="eyebrow"><%= editUser != null ? "Update User" : "Add User" %></span>
                <h4 class="panel-title"><%= editUser != null ? "Edit " + editUser.getUsername() : "New User" %></h4>

                <form action="UserServlet" method="post">
                    <% if (editUser != null) { %>
                        <input type="hidden" name="action" value="update">
                        <input type="hidden" name="userId" value="<%= editUser.getUserId() %>">

                        <div class="form-group">
                            <label>Username</label>
                            <input type="text" value="<%= editUser.getUsername() %>" disabled>
                        </div>
                    <% } else { %>
                        <input type="hidden" name="action" value="add">

                        <div class="form-group">
                            <label for="username">Username</label>
                            <input type="text" id="username" name="username" required>
                        </div>
                    <% } %>

                    <div class="form-group">
                        <label for="fullName">Full Name</label>
                        <input type="text" id="fullName" name="fullName" required
                               value="<%= editUser != null ? editUser.getFullName() : "" %>">
                    </div>
                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" required
                               value="<%= editUser != null && editUser.getEmail() != null ? editUser.getEmail() : "" %>">
                    </div>
                    <div class="form-group">
                        <label for="role">Role</label>
                        <select id="role" name="role" required>
                            <%
                                String currentRole = (editUser != null) ? editUser.getRole() : "";
                                String[][] roles = {
                                    {"ADMIN", "Administrator"},
                                    {"PROCUREMENT_MANAGER", "Procurement Manager"},
                                    {"WAREHOUSE_MANAGER", "Warehouse Manager"},
                                    {"SUPPLIER", "Supplier"},
                                    {"LOGISTICS_STAFF", "Logistics Staff"}
                                };
                                for (String[] r : roles) {
                            %>
                                <option value="<%= r[0] %>" <%= r[0].equals(currentRole) ? "selected" : "" %>><%= r[1] %></option>
                            <% } %>
                        </select>
                    </div>

                    <% Integer currentSupplierId = (editUser != null) ? editUser.getSupplierId() : null; %>
                    <div class="form-group" id="supplierLinkGroup" style="<%= "SUPPLIER".equals(currentRole) ? "" : "display:none;" %>">
                        <label for="supplierId">Linked Supplier</label>
                        <select id="supplierId" name="supplierId">
                            <option value="">-- Not linked --</option>
                            <% for (Supplier s : suppliers) { %>
                                <option value="<%= s.getSupplierId() %>"
                                    <%= (currentSupplierId != null && currentSupplierId == s.getSupplierId()) ? "selected" : "" %>>
                                    <%= s.getSupplierName() %>
                                </option>
                            <% } %>
                        </select>
                    </div>

                    <% if (editUser == null) { %>
                        <div class="form-group">
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password" minlength="8" required>
                        </div>
                        <div class="form-group">
                            <label for="confirmPassword">Confirm Password</label>
                            <input type="password" id="confirmPassword" name="confirmPassword" minlength="8" required>
                        </div>
                    <% } %>

                    <button type="submit" class="btn-login">
                        <%= editUser != null ? "Update User" : "Create User" %>
                    </button>
                    <% if (editUser != null) { %>
                        <a class="btn-secondary" href="manage_users.jsp">Cancel</a>
                    <% } %>
                </form>
            </div>

            <!-- List + search -->
            <div class="panel-card panel-card-wide">
                <div class="section-heading" style="margin-bottom:16px;">
                    <span class="eyebrow">All Users</span>
                    <form action="UserServlet" method="get" class="search-form">
                        <input type="hidden" name="action" value="search">
                        <input type="text" name="keyword" placeholder="Search username, name, or email"
                               value="<%= keyword %>">
                        <button type="submit" class="btn-search">Search</button>
                        <% if (!keyword.isEmpty()) { %>
                            <a class="btn-secondary" href="manage_users.jsp">Clear</a>
                        <% } %>
                    </form>
                </div>

                <% if (users.isEmpty()) { %>
                    <p class="empty-state">No users found. Add one using the form on the left.</p>
                <% } else { %>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Username</th>
                                <th>Full Name</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (User u : users) { %>
                                <tr>
                                    <td><span class="tag"><%= u.getUserId() %></span></td>
                                    <td><%= u.getUsername() %></td>
                                    <td><%= u.getFullName() %></td>
                                    <td><%= u.getEmail() != null ? u.getEmail() : "&mdash;" %></td>
                                    <td>
                                        <%= u.getRole().replace("_", " ") %>
                                        <% if ("SUPPLIER".equals(u.getRole()) && !u.hasLinkedSupplier()) { %>
                                            <br><span style="color: var(--danger); font-size: 11px;">Not linked</span>
                                        <% } %>
                                    </td>
                                    <td>
                                        <span class="badge <%= "ACTIVE".equals(u.getStatus()) ? "badge-active" : "badge-inactive" %>">
                                            <%= u.getStatus() %>
                                        </span>
                                    </td>
                                    <td class="row-actions">
                                        <a href="UserServlet?action=edit&userId=<%= u.getUserId() %>">Edit</a>
                                        <% if (u.getUserId() != user.getUserId()) { %>
                                            <a href="UserServlet?action=toggleStatus&userId=<%= u.getUserId() %>"
                                               onclick="return confirm('<%= "ACTIVE".equals(u.getStatus()) ? "Deactivate" : "Activate" %> user &quot;<%= u.getUsername() %>&quot;?');">
                                               <%= "ACTIVE".equals(u.getStatus()) ? "Deactivate" : "Activate" %>
                                            </a>
                                            <a href="UserServlet?action=delete&userId=<%= u.getUserId() %>"
                                               class="danger-link"
                                               onclick="return confirm('Delete user &quot;<%= u.getUsername() %>&quot;? This cannot be undone.');">Delete</a>
                                        <% } else { %>
                                            <span style="color: var(--slate); font-size: 12.5px;">(you)</span>
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
            <a href="dashboard_admin.jsp">&larr; Back to Dashboard</a>
        </p>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>

    <script>
        // Show the "Linked Supplier" dropdown only while the Supplier role is selected.
        var roleSelect = document.getElementById('role');
        var supplierGroup = document.getElementById('supplierLinkGroup');
        if (roleSelect && supplierGroup) {
            roleSelect.addEventListener('change', function () {
                supplierGroup.style.display = (roleSelect.value === 'SUPPLIER') ? '' : 'none';
            });
        }
    </script>
</body>
</html>
