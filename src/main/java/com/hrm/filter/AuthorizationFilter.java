package com.hrm.filter;

import com.hrm.model.User;
import com.hrm.util.JSONUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Authorization filter for role-based access control
 */
public class AuthorizationFilter implements Filter {
    
    // Role-based endpoint permissions
    // Format: HTTP_METHOD:ENDPOINT_PATTERN -> [ALLOWED_ROLES]
    private static final Map<String, List<String>> ENDPOINT_PERMISSIONS = new HashMap<>();
    
    static {
        // Authentication endpoints - all roles
        ENDPOINT_PERMISSIONS.put("POST:/api/auth/logout", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        
        // Employee endpoints
        ENDPOINT_PERMISSIONS.put("POST:/api/employees", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/employees", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("GET:/api/employees/\\d+", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("PUT:/api/employees/\\d+", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("DELETE:/api/employees/\\d+", Arrays.asList("ADMIN"));
        
        // Department endpoints
        ENDPOINT_PERMISSIONS.put("POST:/api/departments", Arrays.asList("ADMIN"));
        ENDPOINT_PERMISSIONS.put("GET:/api/departments", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("GET:/api/departments/\\d+", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("PUT:/api/departments/\\d+", Arrays.asList("ADMIN"));
        ENDPOINT_PERMISSIONS.put("DELETE:/api/departments/\\d+", Arrays.asList("ADMIN"));
        
        // Attendance endpoints
        ENDPOINT_PERMISSIONS.put("POST:/api/attendance", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/attendance", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("GET:/api/attendance/\\d+", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("PUT:/api/attendance/\\d+", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("DELETE:/api/attendance/\\d+", Arrays.asList("ADMIN", "HR_MANAGER"));
        
        // Leave endpoints
        ENDPOINT_PERMISSIONS.put("POST:/api/leave", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("GET:/api/leave", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("GET:/api/leave/\\d+", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("PUT:/api/leave/\\d+/approve", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("PUT:/api/leave/\\d+/reject", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("PUT:/api/leave/\\d+", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("DELETE:/api/leave/\\d+", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        
        // Payroll endpoints
        ENDPOINT_PERMISSIONS.put("POST:/api/payroll/generate", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/payroll", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/payroll/\\d+", Arrays.asList("ADMIN", "HR_MANAGER", "EMPLOYEE"));
        ENDPOINT_PERMISSIONS.put("PUT:/api/payroll/\\d+", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("DELETE:/api/payroll/\\d+", Arrays.asList("ADMIN"));
        
        // Report endpoints
        ENDPOINT_PERMISSIONS.put("GET:/api/reports", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/reports/employees", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/reports/attendance", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/reports/leave", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/reports/payroll", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/reports/summary", Arrays.asList("ADMIN", "HR_MANAGER"));
        ENDPOINT_PERMISSIONS.put("GET:/api/reports/audit", Arrays.asList("ADMIN"));
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Get user from request (set by AuthenticationFilter)
        User user = (User) httpRequest.getAttribute("currentUser");
        
        if (user == null) {
            JSONUtil.sendForbidden(httpResponse, "User not authenticated");
            return;
        }
        
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        
        // Check authorization
        if (!hasPermission(user, method, path)) {
            JSONUtil.sendForbidden(httpResponse, 
                "Access denied. You don't have permission to access this resource.");
            return;
        }
        
        // Additional checks for employee data access
        if (path.matches("/api/employees/\\d+") && !isAdminOrHR(user)) {
            // Employees can only view their own profile
            String[] pathParts = path.split("/");
            int requestedEmployeeId = Integer.parseInt(pathParts[pathParts.length - 1]);
            
            if (user.getEmployeeId() == null || user.getEmployeeId() != requestedEmployeeId) {
                JSONUtil.sendForbidden(httpResponse, 
                    "You can only access your own employee profile.");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // No cleanup needed
    }
    
    /**
     * Check if user has permission to access the endpoint
     * @param user User
     * @param method HTTP method
     * @param path Request path
     * @return true if authorized
     */
    private boolean hasPermission(User user, String method, String path) {
        String key = method + ":" + path;
        
        // Check exact match first
        if (ENDPOINT_PERMISSIONS.containsKey(key)) {
            List<String> allowedRoles = ENDPOINT_PERMISSIONS.get(key);
            return allowedRoles.contains(user.getRole().name());
        }
        
        // Check pattern matches
        for (Map.Entry<String, List<String>> entry : ENDPOINT_PERMISSIONS.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.contains("\\d+") || pattern.contains(".*")) {
                String permissionKey = method + ":" + pattern;
                if (path.matches("^" + permissionKey.substring(permissionKey.indexOf(':') + 1) + "$")) {
                    List<String> allowedRoles = entry.getValue();
                    return allowedRoles.contains(user.getRole().name());
                }
            }
        }
        
        // Default: deny access
        return false;
    }
    
    /**
     * Check if user is admin or HR manager
     * @param user User
     * @return true if admin or HR
     */
    private boolean isAdminOrHR(User user) {
        return user.isAdmin() || user.isHRManager();
    }
}

