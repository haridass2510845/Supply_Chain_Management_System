package com.scms.servlet;

import com.scms.dao.SupplierDAO;
import com.scms.model.Supplier;
import com.scms.model.User;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * Implements FR2 - Supplier Management (Admin-facing):
 * Add / Update / Delete / Search Supplier.
 *
 * All requests are routed through a single "action" parameter and always
 * forward/redirect back to suppliers.jsp, the same pattern LoginServlet
 * uses for role-based dashboard routing.
 */
@WebServlet("/SupplierServlet")
public class SupplierServlet extends HttpServlet {

    private final SupplierDAO supplierDAO = new SupplierDAO();

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
                handleDelete(request);
                redirectToList(request, response, "Supplier deleted successfully.", null);
                return;

            case "edit":
                handleEdit(request);
                break;

            case "search":
                handleSearch(request);
                break;

            case "list":
            default:
                request.setAttribute("suppliers", supplierDAO.getAllSuppliers());
                break;
        }

        forwardToSuppliers(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        String action = request.getParameter("action");
        action = (action == null) ? "add" : action;

        String supplierName = trim(request.getParameter("supplierName"));
        String contactNo    = trim(request.getParameter("contactNo"));
        String email        = trim(request.getParameter("email"));
        String address      = trim(request.getParameter("address"));

        if (isEmpty(supplierName) || isEmpty(contactNo)) {
            request.setAttribute("errorMessage", "Supplier name and contact number are required.");
            request.setAttribute("suppliers", supplierDAO.getAllSuppliers());
            forwardToSuppliers(request, response);
            return;
        }

        if ("update".equals(action)) {
            int supplierId = parseInt(request.getParameter("supplierId"));
            Supplier supplier = new Supplier(supplierId, supplierName, contactNo, email, address);
            boolean updated = supplierDAO.updateSupplier(supplier);

            redirectToList(request, response,
                    updated ? "Supplier updated successfully." : null,
                    updated ? null : "Could not update supplier. Please try again.");
            return;
        }

        // default: add
        Supplier supplier = new Supplier(0, supplierName, contactNo, email, address);
        boolean added = supplierDAO.addSupplier(supplier);

        redirectToList(request, response,
                added ? "Supplier registered successfully." : null,
                added ? null : "Could not add supplier. Please try again.");
    }

    private void handleDelete(HttpServletRequest request) {
        int supplierId = parseInt(request.getParameter("supplierId"));
        if (supplierId > 0) {
            supplierDAO.deleteSupplier(supplierId);
        }
    }

    private void handleEdit(HttpServletRequest request) {
        int supplierId = parseInt(request.getParameter("supplierId"));
        Supplier editSupplier = supplierDAO.getSupplierById(supplierId);
        request.setAttribute("editSupplier", editSupplier);
        request.setAttribute("suppliers", supplierDAO.getAllSuppliers());
    }

    private void handleSearch(HttpServletRequest request) {
        String keyword = trim(request.getParameter("keyword"));
        request.setAttribute("keyword", keyword);
        if (isEmpty(keyword)) {
            request.setAttribute("suppliers", supplierDAO.getAllSuppliers());
        } else {
            request.setAttribute("suppliers", supplierDAO.searchSuppliers(keyword));
        }
    }

    /**
     * Only an authenticated ADMIN may manage suppliers (SRS 2.3: Administrator ->
     * "Manage suppliers"). Anyone else is bounced to their own dashboard or to login.
     */
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

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

    private void redirectToList(HttpServletRequest request, HttpServletResponse response,
                                 String successMessage, String errorMessage) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/suppliers.jsp?");
        if (successMessage != null) {
            url.append("success=").append(java.net.URLEncoder.encode(successMessage, "UTF-8"));
        } else if (errorMessage != null) {
            url.append("error=").append(java.net.URLEncoder.encode(errorMessage, "UTF-8"));
        }
        response.sendRedirect(url.toString());
    }

    private void forwardToSuppliers(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("suppliers.jsp");
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
