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
 * Implements FR1 - User Login.
 * Authenticates the user and routes to the correct role-based dashboard
 * as described in SRS section 2.3 (User Characteristics).
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Username and password are required.");
            forwardToLogin(request, response);
            return;
        }

        User user = userDAO.authenticate(username.trim(), password);

        if (user == null) {
            // Output: "Invalid Credentials" (SRS FR1)
            request.setAttribute("errorMessage", "Invalid username or password.");
            forwardToLogin(request, response);
            return;
        }

        // Output: "Login Successful" (SRS FR1) -> create session
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setAttribute("role", user.getRole());
        session.setMaxInactiveInterval(30 * 60); // 30 minutes

        // Role-based redirection to the correct dashboard
        String targetPage;
        switch (user.getRole()) {
            case "ADMIN":
                targetPage = "dashboard_admin.jsp";
                break;
            case "PROCUREMENT_MANAGER":
                targetPage = "dashboard_procurement.jsp";
                break;
            case "WAREHOUSE_MANAGER":
                targetPage = "dashboard_warehouse.jsp";
                break;
            case "SUPPLIER":
                targetPage = "dashboard_supplier.jsp";
                break;
            case "LOGISTICS_STAFF":
                targetPage = "dashboard_logistics.jsp";
                break;
            default:
                targetPage = "login.jsp";
        }

        response.sendRedirect(targetPage);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("login.jsp");
    }

    private void forwardToLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("login.jsp");
        rd.forward(request, response);
    }
}
