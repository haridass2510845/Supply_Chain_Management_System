package com.scms.servlet;

import com.scms.dao.InventoryDAO;
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
 * Implements Module 4 - Warehouse Management (Admin / Warehouse Manager
 * facing): WH-01 Receive Goods, WH-02 Store Inventory, WH-03 Update Stock,
 * WH-04 Dispatch Goods, WH-05 Warehouse Reports.
 */
@WebServlet("/WarehouseServlet")
public class WarehouseServlet extends HttpServlet {

    private final InventoryDAO inventoryDAO = new InventoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireWarehouseAccess(request, response);
        if (user == null) {
            return;
        }

        loadPageData(request);
        forwardToInventory(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireWarehouseAccess(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");
        action = (action == null) ? "" : action;

        switch (action) {
            case "receive":
                handleReceive(request, user, response);
                return;

            case "dispatch":
                handleDispatch(request, user, response);
                return;

            case "adjust":
                handleAdjust(request, user, response);
                return;

            default:
                loadPageData(request);
                forwardToInventory(request, response);
        }
    }

    /**
     * WH-01: Pulls one completed purchase order into the warehouse as
     * stock -- the bridge between Procurement finishing an order and
     * Warehouse actually having the goods on the shelf.
     */
    private void handleReceive(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        int poId = parseInt(request.getParameter("poId"));
        boolean received = poId > 0 && inventoryDAO.receiveFromPO(poId, user.getUsername());

        redirectToInventory(request, response,
                received ? "PO-" + poId + " received into inventory." : null,
                received ? null : "Could not receive this order (it may already be received, or isn't completed yet).");
    }

    /**
     * WH-04: Releases stock for an outgoing dispatch. Quantity is entered
     * as a positive number by the user; InventoryDAO applies it as a
     * negative adjustment internally.
     */
    private void handleDispatch(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        int itemId = parseInt(request.getParameter("itemId"));
        int quantity = parseInt(request.getParameter("quantity"));
        String remarks = trim(request.getParameter("remarks"));

        if (itemId <= 0 || quantity <= 0) {
            redirectToInventory(request, response, null, "Please choose an item and a valid quantity to dispatch.");
            return;
        }

        boolean dispatched = inventoryDAO.adjustStock(itemId, -quantity, "DISPATCH", user.getUsername(), remarks);

        redirectToInventory(request, response,
                dispatched ? "Dispatched " + quantity + " unit(s)." : null,
                dispatched ? null : "Not enough stock on hand for that dispatch quantity.");
    }

    /**
     * WH-03: Manual stock correction, positive or negative (e.g. after a
     * physical stock count finds a discrepancy).
     */
    private void handleAdjust(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        int itemId = parseInt(request.getParameter("itemId"));
        int delta = parseInt(request.getParameter("delta"));
        String remarks = trim(request.getParameter("remarks"));

        if (itemId <= 0 || delta == 0) {
            redirectToInventory(request, response, null, "Please choose an item and a non-zero adjustment amount.");
            return;
        }

        boolean adjusted = inventoryDAO.adjustStock(itemId, delta, "ADJUSTMENT", user.getUsername(), remarks);

        redirectToInventory(request, response,
                adjusted ? "Stock adjusted." : null,
                adjusted ? null : "That adjustment would take stock below zero.");
    }

    private void loadPageData(HttpServletRequest request) {
        request.setAttribute("inventory", inventoryDAO.getAllInventory());
        request.setAttribute("receivablePOs", inventoryDAO.getReceivablePOs());
        request.setAttribute("transactions", inventoryDAO.getRecentTransactions(15));
        request.setAttribute("summary", inventoryDAO.getSummary());
    }

    /**
     * ADMIN and WAREHOUSE_MANAGER may manage inventory. Anyone else is
     * bounced to their own dashboard or to login.
     */
    private User requireWarehouseAccess(HttpServletRequest request, HttpServletResponse response)
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

    private void redirectToInventory(HttpServletRequest request, HttpServletResponse response,
                                      String successMessage, String errorMessage) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/warehouse.jsp?");
        if (successMessage != null) {
            url.append("success=").append(URLEncoder.encode(successMessage, "UTF-8"));
        } else if (errorMessage != null) {
            url.append("error=").append(URLEncoder.encode(errorMessage, "UTF-8"));
        }
        response.sendRedirect(url.toString());
    }

    private void forwardToInventory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("warehouse.jsp");
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
