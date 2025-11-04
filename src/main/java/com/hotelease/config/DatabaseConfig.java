package com.hotelease.config;

import org.h2.tools.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides access to the embedded H2 database using JDBC.
 */
public final class DatabaseConfig {

    private static final String JDBC_URL = "jdbc:h2:./database/hotelease;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    private static Server tcpServer;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    public static void startTcpServer() {
        if (!Objects.isNull(tcpServer) && tcpServer.isRunning(false)) {
            return;
        }
        try {
            tcpServer = Server.createTcpServer("-tcpAllowOthers", "-ifNotExists").start();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to start H2 TCP server", e);
        }
    }

    public static void stopTcpServer() {
        if (Objects.isNull(tcpServer)) {
            return;
        }
        tcpServer.stop();
        tcpServer = null;
    }

    public static void initializeSchema() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : loadSchemaStatements()) {
                if (sql.isBlank()) {
                    continue;
                }
                statement.execute(sql.trim());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize database schema", ex);
        }
    }

    private static String[] loadSchemaStatements() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("db/schema.sql")) {
            if (inputStream == null) {
                throw new IllegalStateException("Schema resource db/schema.sql not found on classpath");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String sql = reader.lines().collect(Collectors.joining("\n"));
                return sql.split(";\\s*(?=\n|$)");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema resource", e);
        }
    }
}
