package com.scms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class that provides a JDBC Connection to the Oracle database
 * used by the Supply Chain Management System.
 *
 * Update DB_URL, DB_USER and DB_PASSWORD to match your local Oracle XE setup.
 *
 * IMPORTANT: connections now come from a HikariCP pool instead of being
 * opened fresh (DriverManager) on every call. Opening a raw Oracle
 * connection means a TCP handshake + auth + session setup every time,
 * which is the single biggest source of per-request latency in this app.
 * The pool keeps a handful of connections open and hands them out/back,
 * so getConnection() becomes a cheap in-memory borrow instead of a
 * network round trip.
 */
public class DBConnection {

    // Oracle XE 21c default: listener port 1521, pluggable DB service name XEPDB1
    // (the CDB itself is normally called XE - use XEPDB1 unless you changed it at install time)
    private static final String DB_URL =
            "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String DB_USER = "scms_user";
    private static final String DB_PASSWORD = "scms_pass"; // change to match schema.sql

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setDriverClassName("oracle.jdbc.OracleDriver");

        // Tune these to your machine/DB; 10 is plenty for a small app.
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);   // ms to wait for a free connection
        config.setIdleTimeout(300_000);        // ms before an idle connection is closed
        config.setMaxLifetime(1_800_000);      // ms before a connection is recycled
        config.setPoolName("scms-pool");

        dataSource = new HikariDataSource(config);
    }

    /**
     * @return a pooled JDBC connection to the Oracle scms_user schema.
     *         Callers should still use try-with-resources as before --
     *         close() now returns the connection to the pool instead of
     *         actually closing the socket.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
