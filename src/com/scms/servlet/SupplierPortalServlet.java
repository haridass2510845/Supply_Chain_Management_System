package com.scms.servlet;

import com.scms.dao.PurchaseOrderDAO;
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
 * The Supplier Portal (self-service side of Module 3):
 * PO-07 View My Orders, PO-08 Update Shipment Status,
 * PO-09 Confirm Delivery, PO-10 My Performance History.
 *
 * Every action is scoped to the logged-in supplier's own supplier_id, so
 * one supplier can never see or modify another supplier's orders --
 * enforced in PurchaseOrderDAO's WHERE clauses, not just hidden in the UI.
 */
@WebServlet("/SupplierPortalServlet")
public class SupplierPortalServlet extends HttpServlet {

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireLinkedSupplier(request, response);
        if (user == null) {
            return;
        }

        if (!user.hasLinkedSupplier()) {
            // Account exists and is a SUPPLIER, but no admin has linked it
            // to a supplier company record yet -- let the JSP explain that.
            forwardToOrders(request, response);
            return;
        }

        String action = request.getParameter("action");
        int supplierId = user.getSupplierId();

        if ("ship".equals(action)) {
            int poId = parseInt(request.getParameter("poId"));
            boolean shipped = poId > 0 && poDAO.markShipped(poId, supplierId);
            redirectToOrders(request, response,
                    shipped ? "PO-" + poId + " marked as shipped." : null,
                    shipped ? null : "Could not update this order (it may no longer be approved).");
            return;
        }

        if ("deliver".equals(action)) {
            int poId = parseInt(request.getParameter("poId"));
            boolean delivered = poId > 0 && poDAO.markDelivered(poId, supplierId);
            redirectToOrders(request, response,
                    delivered ? "PO-" + poId + " delivery confirmed." : null,
                    delivered ? null : "Could not confirm delivery (order may not be marked shipped).");
            return;
        }

        // default: list
        request.setAttribute("orders", poDAO.getOrdersBySupplier(supplierId));
        request.setAttribute("performance", poDAO.getPerformanceForSupplier(supplierId));
        forwardToOrders(request, response);
    }

    /**
     * Only a SUPPLIER-role account that has actually been linked to a
     * supplier company record (User.supplierId) may use this portal.
     */
    private User requireLinkedSupplier(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?sessionExpired=true");
            return null;
        }

        if (!"SUPPLIER".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/dashboard_admin.jsp");
            return null;
        }

        return user;
    }

    private void redirectToOrders(HttpServletRequest request, HttpServletResponse response,
                                   String successMessage, String errorMessage) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/my_orders.jsp?");
        if (successMessage != null) {
            url.append("success=").append(URLEncoder.encode(successMessage, "UTF-8"));
        } else if (errorMessage != null) {
            url.append("error=").append(URLEncoder.encode(errorMessage, "UTF-8"));
        }
        response.sendRedirect(url.toString());
    }

    private void forwardToOrders(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("my_orders.jsp");
        rd.forward(request, response);
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
