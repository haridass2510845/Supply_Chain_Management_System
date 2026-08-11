package com.scms.servlet;

import com.scms.dao.UserDAO;
import com.scms.model.User;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * Implements the Admin "Manage Users" module: create, update,
 * activate/deactivate, and delete user accounts across all roles.
 *
 * Follows the same single "action" parameter, forward/redirect pattern
 * that SupplierServlet and PurchaseOrderServlet use, so it fits the rest
 * of the codebase.
 */
@WebServlet("/UserServlet")
public class UserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        String action = request.getParameter("action");
        action = (action == null) ? "list" : action;

        switch (action) {
            case "delete":
                handleDelete(request, response);
                return; // handleDelete already redirects

            case "toggleStatus":
                handleToggleStatus(request, response);
                return; // already redirects

            case "edit":
                handleEdit(request);
                break;

            case "search":
                handleSearch(request);
                break;

            case "list":
            default:
                request.setAttribute("users", userDAO.getAllUsers());
                break;
        }

        forwardToUsers(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        String action = request.getParameter("action");
        action = (action == null) ? "add" : action;

        if ("update".equals(action)) {
            handleUpdate(request, response);
        } else {
            handleAdd(request, response);
        }
    }

    // ------------------------------------------------------------------

    private void handleAdd(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = trim(request.getParameter("username"));
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirmPassword");
        String fullName = trim(request.getParameter("fullName"));
        String email = trim(request.getParameter("email"));
        String role = trim(request.getParameter("role"));

        if (isEmpty(username) || isEmpty(password) || isEmpty(fullName) || isEmpty(email) || isEmpty(role)) {
            redirectToList(request, response, null, "All fields are required to add a new user.");
            return;
        }
        if (password.length() < 8) {
            redirectToList(request, response, null, "Password must be at least 8 characters long.");
            return;
        }
        if (!password.equals(confirm)) {
            redirectToList(request, response, null, "Password and confirmation do not match.");
            return;
        }
        if (!isValidRole(role)) {
            redirectToList(request, response, null, "Please choose a valid role.");
            return;
        }
        if (userDAO.usernameExists(username)) {
            redirectToList(request, response, null, "That username is already taken.");
            return;
        }
        if (userDAO.emailExists(email)) {
            redirectToList(request, response, null, "That email address is already registered to another account.");
            return;
        }

        boolean created = userDAO.registerUser(username, password, fullName, email, role);
        redirectToList(request, response,
                created ? "User \"" + username + "\" created successfully." : null,
                created ? null : "Could not create the user. Please try again.");
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int userId = parseInt(request.getParameter("userId"));
        String fullName = trim(request.getParameter("fullName"));
        String email = trim(request.getParameter("email"));
        String role = trim(request.getParameter("role"));

        if (userId <= 0 || isEmpty(fullName) || isEmpty(email) || isEmpty(role)) {
            redirectToList(request, response, null, "All fields are required to update a user.");
            return;
        }
        if (!isValidRole(role)) {
            redirectToList(request, response, null, "Please choose a valid role.");
            return;
        }

        User existing = userDAO.getUserById(userId);
        if (existing != null && !email.equalsIgnoreCase(existing.getEmail()) && userDAO.emailExists(email)) {
            redirectToList(request, response, null, "That email address is already registered to another account.");
            return;
        }

        boolean updated = userDAO.updateUserDetails(userId, fullName, email, role);
        redirectToList(request, response,
                updated ? "User updated successfully." : null,
                updated ? null : "Could not update the user. Please try again.");
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int userId = parseInt(request.getParameter("userId"));
        User currentUser = currentUser(request);

        if (userId <= 0) {
            redirectToList(request, response, null, "Invalid user.");
            return;
        }
        if (currentUser != null && userId == currentUser.getUserId()) {
            redirectToList(request, response, null, "You cannot delete your own account while logged in.");
            return;
        }

        User target = userDAO.getUserById(userId);
        if (target != null && "ADMIN".equals(target.getRole()) && "ACTIVE".equals(target.getStatus())
                && userDAO.countActiveAdmins() <= 1) {
            redirectToList(request, response, null, "Cannot delete the last active administrator account.");
            return;
        }

        boolean deleted = userDAO.deleteUser(userId);
        redirectToList(request, response,
                deleted ? "User deleted successfully." : null,
                deleted ? null : "Could not delete this user.");
    }

    private void handleToggleStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int userId = parseInt(request.getParameter("userId"));
        User currentUser = currentUser(request);

        if (userId <= 0) {
            redirectToList(request, response, null, "Invalid user.");
            return;
        }
        if (currentUser != null && userId == currentUser.getUserId()) {
            redirectToList(request, response, null, "You cannot deactivate your own account while logged in.");
            return;
        }

        User target = userDAO.getUserById(userId);
        if (target == null) {
            redirectToList(request, response, null, "User not found.");
            return;
        }

        boolean makingInactive = "ACTIVE".equals(target.getStatus());
        if (makingInactive && "ADMIN".equals(target.getRole()) && userDAO.countActiveAdmins() <= 1) {
            redirectToList(request, response, null, "Cannot deactivate the last active administrator account.");
            return;
        }

        String newStatus = makingInactive ? "INACTIVE" : "ACTIVE";
        boolean updated = userDAO.updateStatus(userId, newStatus);
        redirectToList(request, response,
                updated ? "User \"" + target.getUsername() + "\" is now " + newStatus + "." : null,
                updated ? null : "Could not update this user's status.");
    }

    private void handleEdit(HttpServletRequest request) {
        int userId = parseInt(request.getParameter("userId"));
        User editUser = userDAO.getUserById(userId);
        request.setAttribute("editUser", editUser);
        request.setAttribute("users", userDAO.getAllUsers());
    }

    private void handleSearch(HttpServletRequest request) {
        String keyword = trim(request.getParameter("keyword"));
        request.setAttribute("keyword", keyword);
        if (isEmpty(keyword)) {
            request.setAttribute("users", userDAO.getAllUsers());
        } else {
            request.setAttribute("users", userDAO.searchUsers(keyword));
        }
    }

    /**
     * Only an authenticated ADMIN may manage users.
     */
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = currentUser(request);

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?sessionExpired=true");
            return false;
        }
        if (!"ADMIN".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/dashboard_admin.jsp");
            return false;
        }
        return true;
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (User) session.getAttribute("user") : null;
    }

    private boolean isValidRole(String role) {
        switch (role) {
            case "ADMIN":
            case "PROCUREMENT_MANAGER":
            case "WAREHOUSE_MANAGER":
            case "SUPPLIER":
            case "LOGISTICS_STAFF":
                return true;
            default:
                return false;
        }
    }

    private void redirectToList(HttpServletRequest request, HttpServletResponse response,
                                 String successMessage, String errorMessage) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/manage_users.jsp?");
        if (successMessage != null) {
            url.append("success=").append(URLEncoder.encode(successMessage, "UTF-8"));
        } else if (errorMessage != null) {
            url.append("error=").append(URLEncoder.encode(errorMessage, "UTF-8"));
        }
        response.sendRedirect(url.toString());
    }

    private void forwardToUsers(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("manage_users.jsp");
        rd.forward(request, response);
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
