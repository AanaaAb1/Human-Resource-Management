package com.hrm.filter;

import com.hrm.model.User;
import com.hrm.util.JSONUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Authentication filter to check if user is logged in
 */
public class AuthenticationFilter implements Filter {
    
    // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register",
        "/api/health",
        "/css/",
        "/js/",
        "/images/",
        "/favicon.ico"
    );
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestURI.substring(contextPath.length());
        
        // Check if path is public
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check for session-based authentication
        HttpSession session = httpRequest.getSession(false);
        
        if (session == null || session.getAttribute("user") == null) {
            sendUnauthorized(httpResponse, "Authentication required. Please login first.");
            return;
        }
        
        // Validate session
        User user = (User) session.getAttribute("user");
        if (user == null || user.getId() == 0) {
            sendUnauthorized(httpResponse, "Invalid session. Please login again.");
            return;
        }
        
        // Add user to request for downstream use
        httpRequest.setAttribute("currentUser", user);
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // No cleanup needed
    }
    
    /**
     * Check if the path is public (doesn't require authentication)
     * @param path Request path
     * @return true if public
     */
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Send unauthorized response
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException if response fails
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + 
            "\",\"statusCode\":401}");
    }
}

