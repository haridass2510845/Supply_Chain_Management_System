package com.scms.servlet;

import com.scms.dao.PurchaseOrderDAO;
import com.scms.dao.SupplierDAO;
import com.scms.model.PurchaseOrder;
import com.scms.model.User;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;

/**
 * Implements Module 3 - Procurement Management (Admin / Procurement Manager facing):
 * PO-01 Create, PO-02 Approve, PO-03 Cancel, PO-04 Track Status.
 *
 * Follows the same single "action" parameter, forward/redirect pattern that
 * SupplierServlet uses.
 */
@WebServlet("/PurchaseOrderServlet")
public class PurchaseOrderServlet extends HttpServlet {

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireProcurementAccess(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");
        action = (action == null) ? "list" : action;

        switch (action) {
            case "approve":
                handleApprove(request, user, response);
                return;

            case "cancel":
                handleCancel(request, user, response);
                return;

            case "filter":
                String status = request.getParameter("status");
                request.setAttribute("statusFilter", status);
                request.setAttribute("orders", poDAO.getOrdersByStatus(status));
                break;

            case "list":
            default:
                request.setAttribute("orders", poDAO.getAllOrders());
                break;
        }

        loadPageData(request);
        forwardToOrders(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireProcurementAccess(request, response);
        if (user == null) {
            return;
        }

        int supplierId   = parseInt(request.getParameter("supplierId"));
        String itemName  = trim(request.getParameter("itemName"));
        int quantity     = parseInt(request.getParameter("quantity"));
        BigDecimal unitPrice = parseDecimal(request.getParameter("unitPrice"));

        if (supplierId <= 0 || isEmpty(itemName) || quantity <= 0 || unitPrice == null
                || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            request.setAttribute("errorMessage",
                    "Please choose a supplier and provide a valid item, quantity, and unit price.");
            request.setAttribute("orders", poDAO.getAllOrders());
            loadPageData(request);
            forwardToOrders(request, response);
            return;
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplierId(supplierId);
        po.setItemName(itemName);
        po.setQuantity(quantity);
        po.setUnitPrice(unitPrice);
        po.setCreatedBy(user.getUsername());

        boolean created = poDAO.createOrder(po);

        redirectToList(request, response,
                created ? "Purchase order raised successfully." : null,
                created ? null : "Could not create the purchase order. Please try again.");
    }

    private void handleApprove(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        int poId = parseInt(request.getParameter("poId"));
        boolean approved = poId > 0 && poDAO.approveOrder(poId, user.getUsername());

        redirectToList(request, response,
                approved ? "Purchase order #" + poId + " approved." : null,
                approved ? null : "Could not approve this order (it may no longer be pending).");
    }

    private void handleCancel(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        int poId = parseInt(request.getParameter("poId"));
        String remarks = trim(request.getParameter("remarks"));
        boolean cancelled = poId > 0 && poDAO.cancelOrder(poId, remarks);

        redirectToList(request, response,
                cancelled ? "Purchase order #" + poId + " cancelled." : null,
                cancelled ? null : "Could not cancel this order (it may already be completed).");
    }

    /**
     * Loads the supplier dropdown list, supplier performance table, and
     * summary report cards that purchase_orders.jsp needs regardless of
     * which action brought the user there.
     */
    private void loadPageData(HttpServletRequest request) {
        request.setAttribute("suppliers", supplierDAO.getAllSuppliers());
        request.setAttribute("performance", poDAO.getSupplierPerformance());
        request.setAttribute("summary", poDAO.getSummary());
    }

    /**
     * ADMIN and PROCUREMENT_MANAGER may manage purchase orders. Anyone else
     * is bounced to their own dashboard or to login.
     */
    private User requireProcurementAccess(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?sessionExpired=true");
            return null;
        }

        String role = user.getRole();
        if (!"ADMIN".equals(role) && !"PROCUREMENT_MANAGER".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/dashboard_admin.jsp");
            return null;
        }

        return user;
    }

    private void redirectToList(HttpServletRequest request, HttpServletResponse response,
                                 String successMessage, String errorMessage) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/purchase_orders.jsp?");
        if (successMessage != null) {
            url.append("success=").append(URLEncoder.encode(successMessage, "UTF-8"));
        } else if (errorMessage != null) {
            url.append("error=").append(URLEncoder.encode(errorMessage, "UTF-8"));
        }
        response.sendRedirect(url.toString());
    }

    private void forwardToOrders(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("purchase_orders.jsp");
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

    private BigDecimal parseDecimal(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
