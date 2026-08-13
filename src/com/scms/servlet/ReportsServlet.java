package com.scms.servlet;

import com.scms.dao.InventoryDAO;
import com.scms.dao.LogisticsDAO;
import com.scms.dao.PurchaseOrderDAO;
import com.scms.dao.SupplierDAO;
import com.scms.dao.UserDAO;
import com.scms.model.InventoryItem;
import com.scms.model.InventoryTransaction;
import com.scms.model.PurchaseOrder;
import com.scms.model.Shipment;
import com.scms.model.User;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Implements the Admin "Reports" module: cross-cutting summaries pulled
 * from data that already exists across Suppliers, Purchase Orders,
 * Inventory, and Shipments -- plus a CSV export of each report.
 *
 * Everything is read-only: this servlet never writes to the database,
 * it only aggregates what the other modules have already recorded.
 */
@WebServlet("/ReportsServlet")
public class ReportsServlet extends HttpServlet {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final LogisticsDAO logisticsDAO = new LogisticsDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAdmin(request, response)) {
            return;
        }

        String tab = request.getParameter("tab");
        tab = (tab == null || tab.isEmpty()) ? "overview" : tab;

        if ("true".equals(request.getParameter("export"))) {
            exportCsv(tab, response);
            return;
        }

        loadTabData(tab, request);
        request.setAttribute("activeTab", tab);

        RequestDispatcher rd = request.getRequestDispatcher("reports.jsp");
        rd.forward(request, response);
    }

    /**
     * Loads whatever data the requested report tab needs. Every case sets
     * the same attribute names reports.jsp expects, whether reached via
     * this servlet or (on direct reload) loaded by the JSP itself.
     */
    private void loadTabData(String tab, HttpServletRequest request) {
        switch (tab) {
            case "supplier":
                request.setAttribute("suppliers", supplierDAO.getAllSuppliers());
                request.setAttribute("performance", poDAO.getSupplierPerformance());
                break;

            case "procurement":
                request.setAttribute("orders", poDAO.getAllOrders());
                request.setAttribute("poSummary", poDAO.getSummary());
                break;

            case "inventory":
                request.setAttribute("inventory", inventoryDAO.getAllInventory());
                request.setAttribute("invSummary", inventoryDAO.getSummary());
                break;

            case "warehouse":
                request.setAttribute("transactions", inventoryDAO.getRecentTransactions(50));
                request.setAttribute("invSummary", inventoryDAO.getSummary());
                break;

            case "logistics":
                request.setAttribute("shipments", logisticsDAO.getAllShipments());
                request.setAttribute("logSummary", logisticsDAO.getSummary());
                break;

            case "overview":
            default:
                request.setAttribute("poSummary", poDAO.getSummary());
                request.setAttribute("invSummary", inventoryDAO.getSummary());
                request.setAttribute("logSummary", logisticsDAO.getSummary());
                request.setAttribute("supplierCount", supplierDAO.getAllSuppliers().size());
                request.setAttribute("userCount", userDAO.getAllUsers().size());
                break;
        }
    }

    /**
     * Streams the requested report back as a downloadable CSV file so it
     * can be opened in Excel/Sheets or attached to a status update --
     * the concrete, demonstrable output a "Reports" module should produce.
     */
    private void exportCsv(String tab, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        String stamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"scms_" + tab + "_report_" + stamp + ".csv\"");

        PrintWriter out = response.getWriter();

        switch (tab) {
            case "supplier":
                out.println("Supplier Name,Total Orders,Fulfilled,Cancelled,Total Value (INR)");
                for (Map<String, Object> row : poDAO.getSupplierPerformance()) {
                    out.println(csv(row.get("supplierName")) + "," + row.get("totalOrders") + ","
                            + row.get("fulfilledOrders") + "," + row.get("cancelledOrders") + ","
                            + row.get("totalValue"));
                }
                break;

            case "procurement":
                out.println("PO ID,Supplier,Item,Quantity,Unit Price,Total Amount,Status,Created By,Created At");
                for (PurchaseOrder po : poDAO.getAllOrders()) {
                    out.println(po.getPoId() + "," + csv(po.getSupplierName()) + "," + csv(po.getItemName()) + ","
                            + po.getQuantity() + "," + po.getUnitPrice() + "," + po.getTotalAmount() + ","
                            + po.getStatus() + "," + csv(po.getCreatedBy()) + "," + po.getCreatedAt());
                }
                break;

            case "inventory":
                out.println("Item ID,Item Name,Quantity On Hand,Reorder Level,Location,Status,Updated At");
                for (InventoryItem item : inventoryDAO.getAllInventory()) {
                    out.println(item.getItemId() + "," + csv(item.getItemName()) + "," + item.getQuantityOnHand()
                            + "," + item.getReorderLevel() + "," + csv(item.getLocation()) + ","
                            + (item.isLowStock() ? "LOW" : "OK") + "," + item.getUpdatedAt());
                }
                break;

            case "warehouse":
                out.println("Txn ID,Item,Type,Quantity,PO Reference,Performed By,Remarks,Created At");
                for (InventoryTransaction t : inventoryDAO.getRecentTransactions(500)) {
                    out.println(t.getTxnId() + "," + csv(t.getItemName()) + "," + t.getTxnType() + ","
                            + t.getQuantity() + "," + (t.getPoId() != null ? t.getPoId() : "") + ","
                            + csv(t.getPerformedBy()) + "," + csv(t.getRemarks()) + "," + t.getCreatedAt());
                }
                break;

            case "logistics":
                out.println("Shipment ID,Item,Quantity,Destination,Carrier,Vehicle No,Status,Assigned At,Delivered At");
                for (Shipment s : logisticsDAO.getAllShipments()) {
                    out.println(s.getShipmentId() + "," + csv(s.getItemName()) + "," + s.getQuantity() + ","
                            + csv(s.getDestination()) + "," + csv(s.getCarrierName()) + "," + csv(s.getVehicleNo())
                            + "," + s.getStatus() + "," + s.getAssignedAt() + "," + s.getDeliveredAt());
                }
                break;

            default:
                out.println("Unknown report type.");
        }

        out.flush();
    }

    /**
     * Wraps a value in quotes and escapes any embedded quotes so commas
     * or quotes inside names/remarks never break the CSV column layout.
     */
    private String csv(Object value) {
        String s = (value == null) ? "" : value.toString();
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /**
     * Only an authenticated ADMIN may view cross-module reports -- the
     * same guard SupplierServlet and UserServlet use.
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
}
