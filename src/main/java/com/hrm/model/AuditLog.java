package com.hrm.model;

import java.time.LocalDateTime;

/**
 * AuditLog model class for tracking system activities
 */
public class AuditLog {
    private int id;
    private Integer userId;
    private String username; // For display purposes
    private String action;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
    
    // Constructors
    public AuditLog() {}
    
    public AuditLog(int id, Integer userId, String action, String details, String ipAddress) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

