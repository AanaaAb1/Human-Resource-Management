package com.hrm.service;

import com.hrm.dao.AuditLogDAO;
import com.hrm.dao.UserDAO;
import com.hrm.model.User;

import java.util.Optional;

/**
 * Service class for authentication operations
 */
public class AuthService {
    
    private final UserDAO userDAO;
    private final AuditLogDAO auditLogDAO;
    
    public AuthService() {
        this.userDAO = new UserDAO();
        this.auditLogDAO = new AuditLogDAO();
    }
    
    /**
     * Authenticate user with username and password
     * @param username Username
     * @param password Plain text password
     * @param ipAddress Client IP address
     * @return Optional containing user if authentication successful
     */
    public Optional<User> authenticate(String username, String password, String ipAddress) {
        Optional<User> userOpt = userDAO.authenticate(username, password);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            auditLogDAO.logLoginAttempt(user.getId(), username, true, ipAddress);
            return Optional.of(user);
        } else {
            // Get user ID for failed login (if username exists)
            Optional<User> existingUser = userDAO.findByUsername(username);
            int userId = existingUser.map(User::getId).orElse(null);
            auditLogDAO.logLoginAttempt(userId, username, false, ipAddress);
            return Optional.empty();
        }
    }
    
    /**
     * Create new user account
     * @param username Username
     * @param password Plain text password
     * @param role User role
     * @param employeeId Associated employee ID (optional)
     * @return Created user, or empty if failed
     */
    public Optional<User> createUser(String username, String password, User.UserRole role, Integer employeeId) {
        // Check if username already exists
        if (userDAO.usernameExists(username)) {
            return Optional.empty();
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setEmployeeId(employeeId);
        
        int userId = userDAO.create(user);
        if (userId > 0) {
            return userDAO.findById(userId);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get user by ID
     * @param userId User ID
     * @return Optional containing user if found
     */
    public Optional<User> getUserById(int userId) {
        return userDAO.findById(userId);
    }
    
    /**
     * Get user by username
     * @param username Username
     * @return Optional containing user if found
     */
    public Optional<User> getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }
    
    /**
     * Log logout action
     * @param userId User ID
     * @param ipAddress Client IP address
     */
    public void logout(int userId, String ipAddress) {
        auditLogDAO.logLogout(userId, ipAddress);
    }
    
    /**
     * Update user password
     * @param userId User ID
     * @param newPassword New password (will be hashed)
     * @return true if successful
     */
    public boolean updatePassword(int userId, String newPassword) {
        return userDAO.updatePassword(userId, newPassword);
    }
    
    /**
     * Check if user has required role
     * @param user Current user
     * @param requiredRole Required role
     * @return true if user has access
     */
    public boolean hasRole(User user, String requiredRole) {
        if (user == null) {
            return false;
        }
        return user.hasAccess(requiredRole);
    }
    
    /**
     * Check if user is admin
     * @param user User to check
     * @return true if admin
     */
    public boolean isAdmin(User user) {
        return user != null && user.isAdmin();
    }
    
    /**
     * Check if user is HR Manager
     * @param user User to check
     * @return true if HR Manager or Admin
     */
    public boolean isHRManager(User user) {
        return user != null && (user.isHRManager() || user.isAdmin());
    }
    
    /**
     * Check if user can access employee data
     * @param user User to check
     * @param employeeId Employee ID to access
     * @return true if user can access
     */
    public boolean canAccessEmployee(User user, int employeeId) {
        if (user == null) {
            return false;
        }
        
        // Admin and HR Manager can access all employees
        if (isHRManager(user)) {
            return true;
        }
        
        // Employees can only access their own data
        return user.getEmployeeId() != null && user.getEmployeeId() == employeeId;
    }
}

