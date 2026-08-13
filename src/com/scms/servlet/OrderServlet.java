package com.scms.servlet;

import com.scms.dao.InventoryDAO;
import com.scms.dao.OrderDAO;
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
 * Implements Module 6 - Order Fulfillment (Admin / Warehouse Manager
 * facing): OF-01 Receive Customer Order, OF-02 Verify Inventory,
 * OF-03 Process Order, OF-04 Deliver Order.
 */
@WebServlet("/OrderServlet")
public class OrderServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireOrderAccess(request, response);
        if (user == null) {
            return;
        }

        loadPageData(request);
        forwardToOrders(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireOrderAccess(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");
        action = (action == null) ? "" : action;

        switch (action) {
            case "create":
                handleCreate(request, user, response);
                return;

            case "verify":
                handleVerify(request, response);
                return;

            case "process":
                handleProcess(request, user, response);
                return;

            case "deliver":
                handleDeliver(request, response);
                return;

            case "cancel":
                handleCancel(request, response);
                return;

            default:
                loadPageData(request);
                forwardToOrders(request, response);
        }
    }

    /**
     * OF-01: Records a new customer order for an existing inventory item.
     */
    private void handleCreate(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        String customerName = trim(request.getParameter("customerName"));
        int itemId = parseInt(request.getParameter("itemId"));
        int quantity = parseInt(request.getParameter("quantity"));

        if (customerName == null || customerName.isEmpty() || itemId <= 0 || quantity <= 0) {
            redirectToOrders(request, response, null, "Please provide a customer name, item, and valid quantity.");
            return;
        }

        boolean created = orderDAO.createOrder(customerName, itemId, quantity, user.getUsername());

        redirectToOrders(request, response,
                created ? "Order received for " + customerName + "." : null,
                created ? null : "Could not create the order.");
    }

    /**
     * OF-02: Checks a pending order against current stock before it can
     * be processed.
     */
    private void handleVerify(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"));
        boolean verified = orderId > 0 && orderDAO.verifyOrder(orderId);

        redirectToOrders(request, response,
                verified ? "Order #" + orderId + " verified against stock." : null,
                verified ? null : "Not enough stock on hand to verify this order (or it isn't pending).");
    }

    /**
     * OF-03: Deducts stock and moves a verified order to processed.
     */
    private void handleProcess(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        int orderId = parseInt(request.getParameter("orderId"));
        boolean processed = orderId > 0 && orderDAO.processOrder(orderId, user.getUsername());

        redirectToOrders(request, response,
                processed ? "Order #" + orderId + " processed and stock dispatched." : null,
                processed ? null : "Could not process this order (it must be verified first, with stock still available).");
    }

    /**
     * OF-04: Marks a processed order as delivered to the customer.
     */
    private void handleDeliver(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"));
        boolean delivered = orderId > 0 && orderDAO.markDelivered(orderId);

        redirectToOrders(request, response,
                delivered ? "Order #" + orderId + " marked as delivered." : null,
                delivered ? null : "Could not mark this order delivered (it must be processed first).");
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"));
        boolean cancelled = orderId > 0 && orderDAO.cancelOrder(orderId);

        redirectToOrders(request, response,
                cancelled ? "Order #" + orderId + " cancelled." : null,
                cancelled ? null : "Could not cancel this order (it may already be processed).");
    }

    private void loadPageData(HttpServletRequest request) {
        request.setAttribute("orders", orderDAO.getAllOrders());
        request.setAttribute("summary", orderDAO.getSummary());
        request.setAttribute("inventory", inventoryDAO.getAllInventory());
    }

    /**
     * ADMIN and WAREHOUSE_MANAGER handle order fulfillment -- same
     * roles that already handle warehouse stock, since fulfilling an
     * order is ultimately a warehouse action (verify + dispatch stock).
     */
    private User requireOrderAccess(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?sessionExpired=true");
            return null;
        }

        String role = user.getRole();
        if (!"ADMIN".equals(role) && !"WAREHOUSE_MANAGER".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/dashboard_admin.jsp");
            return null;
        }

        return user;
    }

    private void redirectToOrders(HttpServletRequest request, HttpServletResponse response,
                                   String successMessage, String errorMessage) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/orders.jsp?");
        if (successMessage != null) {
            url.append("success=").append(URLEncoder.encode(successMessage, "UTF-8"));
        } else if (errorMessage != null) {
            url.append("error=").append(URLEncoder.encode(errorMessage, "UTF-8"));
        }
        response.sendRedirect(url.toString());
    }

    private void forwardToOrders(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("orders.jsp");
        rd.forward(request, response);
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
