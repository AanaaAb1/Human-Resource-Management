package com.hrm.servlet.auth;

import com.hrm.model.User;
import com.hrm.service.AuthService;
import com.hrm.util.JSONUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet for user logout
 */
public class LogoutServlet extends HttpServlet {
    
    private final AuthService authService;
    
    public LogoutServlet() {
        this.authService = new AuthService();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            HttpSession session = request.getSession(false);
            
            if (session != null && session.getAttribute("user") != null) {
                // Get user before invalidating session
                User user = (User) session.getAttribute("user");
                
                // Log the logout
                String ipAddress = getClientIP(request);
                authService.logout(user.getId(), ipAddress);
                
                System.out.println("User logged out: " + user.getUsername() + " from " + ipAddress);
                
                // Invalidate session
                session.invalidate();
                
                JSONUtil.sendSuccess(response, "Logout successful", null);
            } else {
                JSONUtil.sendSuccess(response, "No active session", null);
            }
            
        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred during logout");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Just return success for GET requests
        JSONUtil.sendSuccess(response, "Logout endpoint ready", null);
    }
    
    /**
     * Get client IP address
     * @param request HttpServletRequest
     * @return IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}

