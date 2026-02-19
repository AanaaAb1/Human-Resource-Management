package com.hrm.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database configuration and connection management
 * Supports both MySQL and PostgreSQL databases
 */
public class DatabaseConfig {
    
    private static final String PROPERTIES_FILE = "/database.properties";
    private static DatabaseConfig instance;
    
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String dbDriver;
    private String dbType;
    
    private DatabaseConfig() {
        loadConfiguration();
    }
    
    /**
     * Singleton pattern for database configuration
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    /**
     * Load database configuration from properties file
     */
    private void loadConfiguration() {
        Properties props = new Properties();
        
        try (InputStream input = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            System.err.println("Properties file not found, using default configuration");
        }
        
        // Determine database type from URL or properties
        String url = System.getenv("DB_URL");
        if (url == null || url.isEmpty()) {
            url = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/hrm_system");
        }
        
        dbUrl = url;
        dbUser = System.getenv("DB_USER");
        if (dbUser == null || dbUser.isEmpty()) {
            dbUser = props.getProperty("db.username", "postgres");
        }
        
        dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null || dbPassword.isEmpty()) {
            dbPassword = props.getProperty("db.password", "password");
        }
        
        // Detect database type from URL
        if (dbUrl.contains("postgresql")) {
            dbType = "postgresql";
            dbDriver = "org.postgresql.Driver";
        } else if (dbUrl.contains("mysql")) {
            dbType = "mysql";
            dbDriver = "com.mysql.cj.jdbc.Driver";
        } else {
            // Default to PostgreSQL
            dbType = "postgresql";
            dbDriver = "org.postgresql.Driver";
        }
        
        System.out.println("Database configured: " + dbType.toUpperCase());
        System.out.println("Database URL: " + dbUrl);
    }
    
    /**
     * Get database connection
     */
    public Connection getConnection() throws SQLException {
        try {
            // Load driver class
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            System.err.println("Database driver not found: " + dbDriver);
            throw new SQLException("Database driver not found", e);
        }
        
        // Set connection properties
        java.util.Properties connProps = new java.util.Properties();
        connProps.setProperty("user", dbUser);
        connProps.setProperty("password", dbPassword);
        
        // Add PostgreSQL specific SSL settings for production
        if ("postgresql".equals(dbType)) {
            connProps.setProperty("ssl", "false");
            connProps.setProperty("sslmode", "prefer");
        } else if ("mysql".equals(dbType)) {
            connProps.setProperty("useSSL", "false");
            connProps.setProperty("allowPublicKeyRetrieval", "true");
            connProps.setProperty("serverTimezone", "UTC");
        }
        
        return DriverManager.getConnection(dbUrl, connProps);
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get database type
     */
    public String getDbType() {
        return dbType;
    }
    
    /**
     * Get database URL
     */
    public String getDbUrl() {
        return dbUrl;
    }
    
    /**
     * Close all resources (for application shutdown)
     */
    public void close() {
        // Connection pooling could be implemented here
        // For now, connections are managed by the calling code
    }
    
    /**
     * Get SQL dialect for the current database
     */
    public String getSqlDialect() {
        return dbType;
    }
    
    /**
     * Get limit clause for pagination
     */
    public String getLimitClause(int limit) {
        if ("mysql".equals(dbType)) {
            return "LIMIT " + limit;
        } else {
            return "LIMIT " + limit;
        }
    }
    
    /**
     * Get offset clause for pagination
     */
    public String getOffsetClause(int offset) {
        if ("mysql".equals(dbType)) {
            return "OFFSET " + offset;
        } else {
            return "OFFSET " + offset;
        }
    }
    
    /**
     * Get current timestamp function
     */
    public String getCurrentTimestamp() {
        if ("mysql".equals(dbType)) {
            return "NOW()";
        } else {
            return "CURRENT_TIMESTAMP";
        }
    }
    
    /**
     * Get last inserted ID function
     */
    public String getLastInsertedId() {
        if ("mysql".equals(dbType)) {
            return "LAST_INSERT_ID()";
        } else {
            return "LASTVAL()";
        }
    }
    
    /**
     * Get auto-increment keyword
     */
    public String getAutoIncrement() {
        if ("mysql".equals(dbType)) {
            return "AUTO_INCREMENT";
        } else {
            return "SERIAL";
        }
    }
}

