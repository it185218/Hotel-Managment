package com.hotel.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Runs schema.sql on first startup to create tables if they don't exist.
 */
public class DatabaseInitializer {

    public static void initialize() {
        try (InputStream in = DatabaseInitializer.class
                .getClassLoader().getResourceAsStream("schema.sql")) {

            if (in == null) throw new RuntimeException("schema.sql not found.");

            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Connection conn = DatabaseConnection.getInstance().getConnection();

            // Execute each statement separately (split on semicolon)
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }

            System.out.println("[DB] Schema initialised successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise database schema: " + e.getMessage(), e);
        }
    }
}
