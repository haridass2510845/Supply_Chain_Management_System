package com.scms.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Ensures dashboard pages can only be accessed by an authenticated,
 * logged-in user (supports Security NFR: Role-Based Access Control).
 */
@WebFilter(urlPatterns = {
        "/dashboard_admin.jsp",
        "/dashboard_procurement.jsp",
        "/dashboard_warehouse.jsp",
        "/dashboard_supplier.jsp",
        "/dashboard_logistics.jsp",
        "/suppliers.jsp",
        "/purchase_orders.jsp",
        "/my_orders.jsp",
        "/manage_users.jsp",
        "/warehouse.jsp"
})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute("user") != null);

        if (loggedIn) {
            chain.doFilter(req, res);
        } else {
            response.sendRedirect(request.getContextPath() + "/login.jsp?sessionExpired=true");
        }
    }
}
