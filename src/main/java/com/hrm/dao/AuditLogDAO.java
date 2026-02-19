package com.hrm.dao;

import com.hrm.config.DatabaseConfig;
import com.hrm.model.AuditLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Audit Log operations
 */
public class AuditLogDAO {
    
    private static final String TABLE_NAME = "audit_logs";
    
    /**
     * Find audit log by ID
     * @param id Audit log ID
     * @return Audit log if found
     */
    public AuditLog findById(int id) {
        String sql = "SELECT al.*, u.username as username FROM " + TABLE_NAME + 
                     " al LEFT JOIN users u ON al.user_id = u.id WHERE al.id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToAuditLog(resultSet);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding audit log by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Find audit logs by user
     * @param userId User ID
     * @return List of audit logs
     */
    public List<AuditLog> findByUser(int userId) {
        List<AuditLog> auditLogs = new ArrayList<>();
        String sql = "SELECT al.*, u.username as username FROM " + TABLE_NAME + 
                     " al LEFT JOIN users u ON al.user_id = u.id " +
                     " WHERE al.user_id = ? ORDER BY al.created_at DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, userId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auditLogs.add(mapResultSetToAuditLog(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding audit logs by user: " + e.getMessage());
        }
        
        return auditLogs;
    }
    
    /**
     * Find audit logs by action
     * @param action Action type
     * @return List of audit logs
     */
    public List<AuditLog> findByAction(String action) {
        List<AuditLog> auditLogs = new ArrayList<>();
        String sql = "SELECT al.*, u.username as username FROM " + TABLE_NAME + 
                     " al LEFT JOIN users u ON al.user_id = u.id " +
                     " WHERE al.action = ? ORDER BY al.created_at DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, action);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auditLogs.add(mapResultSetToAuditLog(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding audit logs by action: " + e.getMessage());
        }
        
        return auditLogs;
    }
    
    /**
     * Find audit logs within date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of audit logs
     */
    public List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> auditLogs = new ArrayList<>();
        String sql = "SELECT al.*, u.username as username FROM " + TABLE_NAME + 
                     " al LEFT JOIN users u ON al.user_id = u.id " +
                     " WHERE al.created_at BETWEEN ? AND ? ORDER BY al.created_at DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setTimestamp(1, Timestamp.valueOf(startDate));
            statement.setTimestamp(2, Timestamp.valueOf(endDate));
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auditLogs.add(mapResultSetToAuditLog(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding audit logs by date range: " + e.getMessage());
        }
        
        return auditLogs;
    }
    
    /**
     * Get all audit logs
     * @return List of all audit logs
     */
    public List<AuditLog> findAll() {
        List<AuditLog> auditLogs = new ArrayList<>();
        String sql = "SELECT al.*, u.username as username FROM " + TABLE_NAME + 
                     " al LEFT JOIN users u ON al.user_id = u.id " +
                     " ORDER BY al.created_at DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                auditLogs.add(mapResultSetToAuditLog(resultSet));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all audit logs: " + e.getMessage());
        }
        
        return auditLogs;
    }
    
    /**
     * Create new audit log
     * @param auditLog Audit log to create
     * @return Created audit log ID, -1 if failed
     */
    public int create(AuditLog auditLog) {
        String sql = "INSERT INTO " + TABLE_NAME + " (user_id, action, details, ip_address) VALUES (?, ?, ?, ?)";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            if (auditLog.getUserId() != null) {
                statement.setInt(1, auditLog.getUserId());
            } else {
                statement.setNull(1, Types.INTEGER);
            }
            
            statement.setString(2, auditLog.getAction());
            statement.setString(3, auditLog.getDetails());
            statement.setString(4, auditLog.getIpAddress());
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating audit log: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Log an action
     * @param userId User ID (can be null for system actions)
     * @param action Action performed
     * @param details Action details
     * @param ipAddress Client IP address
     * @return Created audit log ID
     */
    public int logAction(Integer userId, String action, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        
        return create(auditLog);
    }
    
    /**
     * Log login attempt
     * @param userId User ID
     * @param username Username
     * @param success Whether login was successful
     * @param ipAddress Client IP address
     */
    public void logLoginAttempt(Integer userId, String username, boolean success, String ipAddress) {
        String action = success ? "LOGIN_SUCCESS" : "LOGIN_FAILED";
        String details = "Username: " + username + ", Success: " + success;
        logAction(userId, action, details, ipAddress);
    }
    
    /**
     * Log logout
     * @param userId User ID
     * @param ipAddress Client IP address
     */
    public void logLogout(Integer userId, String ipAddress) {
        logAction(userId, "LOGOUT", "User logged out", ipAddress);
    }
    
    /**
     * Log data modification
     * @param userId User ID
     * @param action Action type (CREATE, UPDATE, DELETE)
     * @param entityType Entity type (Employee, Department, etc.)
     * @param entityId Entity ID
     * @param details Additional details
     * @param ipAddress Client IP address
     */
    public void logDataModification(Integer userId, String action, String entityType, 
                                    Integer entityId, String details, String ipAddress) {
        String fullAction = action + "_" + entityType.toUpperCase();
        String fullDetails = "Entity ID: " + entityId + ", Details: " + details;
        logAction(userId, fullAction, fullDetails, ipAddress);
    }
    
    /**
     * Get recent logs (limited)
     * @param limit Maximum number of logs
     * @return List of recent audit logs
     */
    public List<AuditLog> getRecentLogs(int limit) {
        List<AuditLog> auditLogs = new ArrayList<>();
        String sql = "SELECT al.*, u.username as username FROM " + TABLE_NAME + 
                     " al LEFT JOIN users u ON al.user_id = u.id " +
                     " ORDER BY al.created_at DESC LIMIT ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, limit);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auditLogs.add(mapResultSetToAuditLog(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent audit logs: " + e.getMessage());
        }
        
        return auditLogs;
    }
    
    /**
     * Count audit logs
     * @return Total count
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting audit logs: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Map ResultSet to AuditLog object
     * @param resultSet ResultSet
     * @return AuditLog object
     * @throws SQLException if mapping fails
     */
    private AuditLog mapResultSetToAuditLog(ResultSet resultSet) throws SQLException {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(resultSet.getInt("id"));
        
        int userId = resultSet.getInt("user_id");
        if (!resultSet.wasNull()) {
            auditLog.setUserId(userId);
        }
        
        auditLog.setUsername(resultSet.getString("username"));
        auditLog.setAction(resultSet.getString("action"));
        auditLog.setDetails(resultSet.getString("details"));
        auditLog.setIpAddress(resultSet.getString("ip_address"));
        
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            auditLog.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return auditLog;
    }
}

