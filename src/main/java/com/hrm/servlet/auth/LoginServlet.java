package com.hrm.servlet.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hrm.model.User;
import com.hrm.service.AuthService;
import com.hrm.util.JSONUtil;
import com.hrm.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

/**
 * Servlet for user authentication (login)
 */
public class LoginServlet extends HttpServlet {
    
    private final AuthService authService;
    
    public LoginServlet() {
        this.authService = new AuthService();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            
            String requestBody = sb.toString();
            
            if (requestBody == null || requestBody.isEmpty()) {
                JSONUtil.sendBadRequest(response, "Request body is empty");
                return;
            }
            
            // Parse JSON
            JsonObject jsonRequest;
            try {
                jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();
            } catch (Exception e) {
                JSONUtil.sendBadRequest(response, "Invalid JSON format");
                return;
            }
            
            // Extract and validate parameters
            String username = jsonRequest.has("username") ? 
                jsonRequest.get("username").getAsString().trim() : null;
            String password = jsonRequest.has("password") ? 
                jsonRequest.get("password").getAsString() : null;
            
            // Validate input
            if (!ValidationUtil.isNotEmpty(username)) {
                JSONUtil.sendBadRequest(response, "Username is required");
                return;
            }
            
            if (!ValidationUtil.isNotEmpty(password)) {
                JSONUtil.sendBadRequest(response, "Password is required");
                return;
            }
            
            // Get client IP address
            String ipAddress = getClientIP(request);
            
            // Authenticate user
            Optional<User> userOpt = authService.authenticate(username, password, ipAddress);
            
            if (userOpt.isEmpty()) {
                JSONUtil.sendUnauthorized(response, "Invalid username or password");
                return;
            }
            
            User user = userOpt.get();
            
            // Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes
            
            // Log successful login
            System.out.println("User logged in: " + username + " from " + ipAddress);
            
            // Prepare response (don't send password)
            JsonObject userJson = new JsonObject();
            userJson.addProperty("id", user.getId());
            userJson.addProperty("username", user.getUsername());
            userJson.addProperty("role", user.getRole().name());
            userJson.addProperty("employeeId", user.getEmployeeId());
            
            // Get employee details if available
            if (user.getEmployeeId() != null) {
                userJson.addProperty("employeeDetailId", user.getEmployeeId());
            }
            
            JSONUtil.sendSuccess(response, "Login successful", userJson);
            
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred during login");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Return current session info if logged in
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            
            JsonObject userJson = new JsonObject();
            userJson.addProperty("id", user.getId());
            userJson.addProperty("username", user.getUsername());
            userJson.addProperty("role", user.getRole().name());
            userJson.addProperty("employeeId", user.getEmployeeId());
            userJson.addProperty("loggedIn", true);
            
            JSONUtil.sendSuccess(response, "User is logged in", userJson);
        } else {
            JsonObject result = new JsonObject();
            result.addProperty("loggedIn", false);
            JSONUtil.sendSuccess(response, "User is not logged in", result);
        }
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

