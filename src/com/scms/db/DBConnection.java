package com.scms.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class that provides a JDBC Connection to the Oracle database
 * used by the Supply Chain Management System.
 *
 * Update DB_URL, DB_USER and DB_PASSWORD to match your local Oracle XE setup.
 */
public class DBConnection {

    // Oracle XE 21c default: listener port 1521, pluggable DB service name XEPDB1
    // (the CDB itself is normally called XE - use XEPDB1 unless you changed it at install time)
    private static final String DB_URL =
            "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String DB_USER = "scms_user";
    private static final String DB_PASSWORD = "scms_pass"; // change to match schema.sql

    static {
        try {
            // Oracle JDBC (ojdbc) driver
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Oracle JDBC Driver not found. "
                    + "Add ojdbc11.jar (or ojdbc8.jar) to WEB-INF/lib.", e);
        }
    }

    /**
     * @return a new live JDBC connection to the Oracle scms_user schema.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
