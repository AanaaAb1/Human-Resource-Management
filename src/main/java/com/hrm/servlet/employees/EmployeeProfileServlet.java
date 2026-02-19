package com.hrm.servlet.employees;

import com.hrm.model.Employee;
import com.hrm.model.User;
import com.hrm.service.EmployeeService;
import com.hrm.util.JSONUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Servlet for employee profile operations
 */
public class EmployeeProfileServlet extends HttpServlet {
    
    private final EmployeeService employeeService;
    
    public EmployeeProfileServlet() {
        this.employeeService = new EmployeeService();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            User currentUser = (User) request.getAttribute("currentUser");
            
            // Get the employee ID from the request
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get current user's profile
                if (currentUser.getEmployeeId() == null) {
                    JSONUtil.sendNotFound(response, "No employee profile associated with this user");
                    return;
                }
                
                Optional<Employee> employeeOpt = employeeService.getEmployeeById(currentUser.getEmployeeId());
                
                if (employeeOpt.isEmpty()) {
                    JSONUtil.sendNotFound(response, "Employee profile not found");
                    return;
                }
                
                JSONUtil.sendSuccess(response, "Profile retrieved", employeeOpt.get());
                return;
            }
            
            // Get specific employee profile
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length >= 2) {
                int employeeId;
                try {
                    employeeId = Integer.parseInt(pathParts[1]);
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid employee ID");
                    return;
                }
                
                Optional<Employee> employeeOpt = employeeService.getEmployeeById(employeeId);
                
                if (employeeOpt.isEmpty()) {
                    JSONUtil.sendNotFound(response, "Employee not found");
                    return;
                }
                
                // Check access - employees can only view their own profile
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (employeeId != currentUser.getEmployeeId()) {
                        JSONUtil.sendForbidden(response, "You can only view your own profile");
                        return;
                    }
                }
                
                JSONUtil.sendSuccess(response, "Profile retrieved", employeeOpt.get());
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid request");
            
        } catch (Exception e) {
            System.err.println("Employee profile GET error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            User currentUser = (User) request.getAttribute("currentUser");
            
            // Employees can only update their own profile
            if (currentUser.getEmployeeId() == null) {
                JSONUtil.sendNotFound(response, "No employee profile associated with this user");
                return;
            }
            
            // Parse request body
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
            
            // For now, redirect to EmployeeServlet for full updates
            // This servlet is primarily for self-service profile updates
            JSONUtil.sendBadRequest(response, "Please use /api/employees/{id} for updates");
            
        } catch (Exception e) {
            System.err.println("Employee profile PUT error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred");
        }
    }
}

