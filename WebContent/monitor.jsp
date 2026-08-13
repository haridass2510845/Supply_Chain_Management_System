<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.scms.model.User" %>
<%@ page import="com.scms.model.LoginAttempt" %>
<%@ page import="com.scms.dao.UserDAO" %>
<%@ page import="com.scms.dao.AuditDAO" %>
<%@ page import="com.scms.db.DBConnection" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
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

    // System health: a live check, not a cached assumption -- if the DB
    // connection fails right now, this page will actually say so.
    boolean dbHealthy;
    try (Connection testCon = DBConnection.getConnection()) {
        dbHealthy = (testCon != null && !testCon.isClosed());
    } catch (Exception e) {
        dbHealthy = false;
    }

    UserDAO userDAO = new UserDAO();
    AuditDAO auditDAO = new AuditDAO();

    List<User> allUsers = userDAO.getAllUsers();
    int activeCount = 0, inactiveCount = 0;
    for (User u : allUsers) {
        if ("ACTIVE".equals(u.getStatus())) activeCount++; else inactiveCount++;
    }

    List<LoginAttempt> recentAttempts = auditDAO.getRecentAttempts(20);
    Map<String, Object> attemptSummary = auditDAO.getRecentSummary(20);
%>
<!DOCTYPE html>
<html>
<head>
    <title>Monitor System - SCMS</title>
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
                <span class="eyebrow">System Health &amp; Audit</span>
                <h3>Monitor System</h3>
                <p>Live database status, account activity, and recent login attempts.</p>
            </div>
            <% if (dbHealthy) { %>
                <div class="status-pill"><span class="dot"></span> System Operational</div>
            <% } else { %>
                <div class="status-pill" style="background:var(--danger-bg); color:var(--danger); border-color:var(--danger);">
                    <span class="dot" style="background:var(--danger);"></span> Database Unreachable
                </div>
            <% } %>
        </div>

        <div class="summary-strip">
            <div class="summary-tile">
                <span class="summary-label">Database</span>
                <span class="summary-value" style="color:<%= dbHealthy ? "var(--success)" : "var(--danger)" %>; font-size:18px;">
                    <%= dbHealthy ? "Connected" : "Down" %>
                </span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Active Accounts</span>
                <span class="summary-value"><%= activeCount %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Inactive Accounts</span>
                <span class="summary-value"><%= inactiveCount %></span>
            </div>
            <div class="summary-tile">
                <span class="summary-label">Failed Logins (last 20)</span>
                <span class="summary-value" style="color:<%= ((Integer) attemptSummary.get("failureCount")) > 0 ? "var(--danger)" : "var(--ink)" %>;">
                    <%= attemptSummary.get("failureCount") %>
                </span>
            </div>
        </div>

        <div class="section-heading" style="margin-top:8px;">
            <span class="eyebrow">Recent Login Activity</span>
        </div>
        <div class="panel-card panel-card-wide">
            <% if (recentAttempts.isEmpty()) { %>
                <p class="empty-state">No login attempts recorded yet.</p>
            <% } else { %>
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Username</th>
                            <th>Result</th>
                            <th>IP Address</th>
                            <th>When</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (LoginAttempt a : recentAttempts) { %>
                            <tr>
                                <td><%= a.getUsername() %></td>
                                <td>
                                    <% if (a.isSuccess()) { %>
                                        <span class="badge badge-approved">Success</span>
                                    <% } else { %>
                                        <span class="badge badge-cancelled">Failed</span>
                                    <% } %>
                                </td>
                                <td><%= a.getIpAddress() != null ? a.getIpAddress() : "&mdash;" %></td>
                                <td><%= a.getAttemptedAt() %></td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } %>
        </div>

        <p class="demo-note" style="text-align:left; margin-top:24px;">
            <a href="dashboard_admin.jsp">&larr; Back to Dashboard</a>
        </p>
    </div>

    <footer class="scms-footer">SCMS &middot; Supply Chain Management System</footer>
</body>
</html>
