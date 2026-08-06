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

/**
 * Handles the "Change Password" function for a logged-in user
 * (SRS 2.2 Product Functions).
 */
@WebServlet("/ChangePasswordServlet")
public class ChangePasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login.jsp?sessionExpired=true");
            return;
        }

        String currentPassword = request.getParameter("currentPassword");
        String newPassword     = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (isEmpty(currentPassword) || isEmpty(newPassword) || isEmpty(confirmPassword)) {
            fail(request, response, "All fields are required.");
            return;
        }

        if (!userDAO.verifyPassword(user.getUserId(), currentPassword)) {
            fail(request, response, "Current password is incorrect.");
            return;
        }

        if (newPassword.length() < 8) {
            fail(request, response, "New password must be at least 8 characters long.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            fail(request, response, "New password and confirmation do not match.");
            return;
        }

        if (newPassword.equals(currentPassword)) {
            fail(request, response, "New password must be different from the current password.");
            return;
        }

        boolean updated = userDAO.changePassword(user.getUserId(), newPassword);

        if (!updated) {
            fail(request, response, "Could not update password. Please try again.");
            return;
        }

        request.setAttribute("successMessage", "Password updated successfully.");
        RequestDispatcher rd = request.getRequestDispatcher("change_password.jsp");
        rd.forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("change_password.jsp");
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        RequestDispatcher rd = request.getRequestDispatcher("change_password.jsp");
        rd.forward(request, response);
    }
}
