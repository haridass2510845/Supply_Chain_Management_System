package com.scms.servlet;

import com.scms.dao.UserDAO;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles new account sign-up. Extends the login module with a basic
 * self-registration flow so new users don't have to be seeded manually.
 */
@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username  = trim(request.getParameter("username"));
        String password  = request.getParameter("password");
        String confirm   = request.getParameter("confirmPassword");
        String fullName  = trim(request.getParameter("fullName"));
        String email     = trim(request.getParameter("email"));
        String role      = trim(request.getParameter("role"));

        // --- Basic validation ---
        if (isEmpty(username) || isEmpty(password) || isEmpty(fullName) || isEmpty(role)) {
            fail(request, response, "All required fields must be filled in.");
            return;
        }

        if (password.length() < 8) {
            fail(request, response, "Password must be at least 8 characters long.");
            return;
        }

        if (!password.equals(confirm)) {
            fail(request, response, "Passwords do not match.");
            return;
        }

        if (!isValidRole(role)) {
            fail(request, response, "Please select a valid role.");
            return;
        }

        if (userDAO.usernameExists(username)) {
            fail(request, response, "That username is already taken. Please choose another.");
            return;
        }

        boolean created = userDAO.registerUser(username, password, fullName, email, role);

        if (!created) {
            fail(request, response, "Registration failed. Please try again.");
            return;
        }

        // Success -> send back to login with a confirmation flag
        response.sendRedirect("login.jsp?registered=true");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("register.jsp");
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

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        RequestDispatcher rd = request.getRequestDispatcher("register.jsp");
        rd.forward(request, response);
    }
}
