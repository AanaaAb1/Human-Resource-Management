package com.hrm.model;

import java.time.LocalDateTime;

/**
 * User model class representing system users
 */
public class User {
    private int id;
    private String username;
    private String password;
    private UserRole role;
    private Integer employeeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum UserRole {
        ADMIN,
        HR_MANAGER,
        EMPLOYEE
    }
    
    // Constructors
    public User() {}
    
    public User(int id, String username, String password, UserRole role, Integer employeeId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.employeeId = employeeId;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public boolean isHRManager() {
        return role == UserRole.HR_MANAGER;
    }
    
    public boolean isEmployee() {
        return role == UserRole.EMPLOYEE;
    }
    
    public boolean hasAccess(String requiredRole) {
        if ("ADMIN".equals(requiredRole)) {
            return isAdmin();
        } else if ("HR_MANAGER".equals(requiredRole)) {
            return isAdmin() || isHRManager();
        } else if ("EMPLOYEE".equals(requiredRole)) {
            return true; // All authenticated users have employee access
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", employeeId=" + employeeId +
                '}';
    }
}

