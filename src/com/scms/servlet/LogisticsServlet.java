package com.scms.servlet;

import com.scms.dao.LogisticsDAO;
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
 * Implements Module 5 - Logistics Management (Admin / Logistics Staff
 * facing): LG-01 Assign Delivery, LG-02 Track Shipment,
 * LG-03 Update Delivery Status, LG-04 Confirm Delivery.
 */
@WebServlet("/LogisticsServlet")
public class LogisticsServlet extends HttpServlet {

    private final LogisticsDAO logisticsDAO = new LogisticsDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireLogisticsAccess(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");

        if ("inTransit".equals(action)) {
            handleInTransit(request, response);
            return;
        }

        if ("deliver".equals(action)) {
            handleDeliver(request, response);
            return;
        }

        String status = request.getParameter("status");
        request.setAttribute("statusFilter", (status == null) ? "ALL" : status);
        request.setAttribute("shipments", logisticsDAO.getShipmentsByStatus(status));

        loadPageData(request);
        forwardToLogistics(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = requireLogisticsAccess(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");
        action = (action == null) ? "" : action;

        switch (action) {
            case "assign":
                handleAssign(request, user, response);
                return;

            case "inTransit":
                handleInTransit(request, response);
                return;

            case "deliver":
                handleDeliver(request, response);
                return;

            default:
                request.setAttribute("statusFilter", "ALL");
                request.setAttribute("shipments", logisticsDAO.getAllShipments());
                loadPageData(request);
                forwardToLogistics(request, response);
        }
    }

    /**
     * LG-01: Turns one unassigned warehouse dispatch into a shipment with
     * a destination and carrier attached.
     */
    private void handleAssign(HttpServletRequest request, User user, HttpServletResponse response)
            throws IOException {
        int txnId = parseInt(request.getParameter("txnId"));
        String destination = trim(request.getParameter("destination"));
        String carrierName = trim(request.getParameter("carrierName"));
        String vehicleNo = trim(request.getParameter("vehicleNo"));

        if (txnId <= 0 || isEmpty(destination) || isEmpty(carrierName)) {
            redirectToLogistics(request, response, null,
                    "Please provide a destination and carrier name for this delivery.");
            return;
        }

        boolean assigned = logisticsDAO.assignDelivery(txnId, destination, carrierName, vehicleNo, user.getUsername());

        redirectToLogistics(request, response,
                assigned ? "Delivery assigned." : null,
                assigned ? null : "Could not assign this delivery (it may already be assigned).");
    }

    /**
     * LG-03: Marks a shipment as picked up and on the way.
     */
    private void handleInTransit(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int shipmentId = parseInt(request.getParameter("shipmentId"));
        boolean updated = shipmentId > 0 && logisticsDAO.markInTransit(shipmentId);

        redirectToLogistics(request, response,
                updated ? "Shipment marked in transit." : null,
                updated ? null : "Could not update this shipment (it may no longer be pending pickup).");
    }

    /**
     * LG-04: Confirms final delivery.
     */
    private void handleDeliver(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int shipmentId = parseInt(request.getParameter("shipmentId"));
        boolean delivered = shipmentId > 0 && logisticsDAO.confirmDelivery(shipmentId);

        redirectToLogistics(request, response,
                delivered ? "Delivery confirmed." : null,
                delivered ? null : "Could not confirm delivery (shipment may not be in transit).");
    }

    private void loadPageData(HttpServletRequest request) {
        request.setAttribute("unassignedDispatches", logisticsDAO.getUnassignedDispatches());
        request.setAttribute("summary", logisticsDAO.getSummary());
    }

    /**
     * ADMIN and LOGISTICS_STAFF may manage shipments. Anyone else is
     * bounced to their own dashboard or to login.
     */
    private User requireLogisticsAccess(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?sessionExpired=true");
            return null;
        }

        String role = user.getRole();
        if (!"ADMIN".equals(role) && !"LOGISTICS_STAFF".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/dashboard_admin.jsp");
            return null;
        }

        return user;
    }

    private void redirectToLogistics(HttpServletRequest request, HttpServletResponse response,
                                      String successMessage, String errorMessage) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/logistics.jsp?");
        if (successMessage != null) {
            url.append("success=").append(URLEncoder.encode(successMessage, "UTF-8"));
        } else if (errorMessage != null) {
            url.append("error=").append(URLEncoder.encode(errorMessage, "UTF-8"));
        }
        response.sendRedirect(url.toString());
    }

    private void forwardToLogistics(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher rd = request.getRequestDispatcher("logistics.jsp");
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
