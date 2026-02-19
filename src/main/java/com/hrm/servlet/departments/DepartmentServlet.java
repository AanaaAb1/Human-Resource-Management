package com.hrm.servlet.departments;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hrm.model.Department;
import com.hrm.model.User;
import com.hrm.service.DepartmentService;
import com.hrm.util.JSONUtil;
import com.hrm.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Servlet for department CRUD operations
 */
public class DepartmentServlet extends HttpServlet {
    
    private final DepartmentService departmentService;
    
    public DepartmentServlet() {
        this.departmentService = new DepartmentService();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            
            // GET /api/departments - Get all departments
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Department> departments = departmentService.getAllDepartments();
                JSONUtil.sendSuccess(response, "Departments retrieved", departments);
                return;
            }
            
            // GET /api/departments/{id} - Get specific department
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length >= 2) {
                int departmentId;
                try {
                    departmentId = Integer.parseInt(pathParts[1]);
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid department ID");
                    return;
                }
                
                Optional<Department> departmentOpt = departmentService.getDepartmentById(departmentId);
                
                if (departmentOpt.isEmpty()) {
                    JSONUtil.sendNotFound(response, "Department not found");
                    return;
                }
                
                JSONUtil.sendSuccess(response, "Department retrieved", departmentOpt.get());
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid request");
            
        } catch (Exception e) {
            System.err.println("Department GET error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
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
            
            JsonObject jsonRequest;
            try {
                jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();
            } catch (Exception e) {
                JSONUtil.sendBadRequest(response, "Invalid JSON format");
                return;
            }
            
            // Validate required fields
            String name = jsonRequest.has("name") ? 
                jsonRequest.get("name").getAsString().trim() : null;
            
            if (!ValidationUtil.isNotEmpty(name)) {
                JSONUtil.sendBadRequest(response, "Department name is required");
                return;
            }
            
            if (name.length() > 100) {
                JSONUtil.sendBadRequest(response, "Department name must be less than 100 characters");
                return;
            }
            
            String description = jsonRequest.has("description") ? 
                jsonRequest.get("description").getAsString() : null;
            
            // Create department
            Department department = new Department();
            department.setName(name);
            department.setDescription(description);
            
            int newDepartmentId = departmentService.createDepartment(department, currentUser.getId(), ipAddress);
            
            if (newDepartmentId > 0) {
                Optional<Department> createdDepartment = departmentService.getDepartmentById(newDepartmentId);
                JSONUtil.sendCreated(response, "Department created successfully", createdDepartment.orElse(null));
            } else {
                JSONUtil.sendBadRequest(response, "Failed to create department. Department name may already exist.");
            }
            
        } catch (Exception e) {
            System.err.println("Department POST error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while creating department");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
            if (pathInfo == null || pathInfo.equals("/")) {
                JSONUtil.sendBadRequest(response, "Department ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int departmentId;
            try {
                departmentId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid department ID");
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
            
            JsonObject jsonRequest;
            try {
                jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();
            } catch (Exception e) {
                JSONUtil.sendBadRequest(response, "Invalid JSON format");
                return;
            }
            
            // Get existing department
            Optional<Department> existingOpt = departmentService.getDepartmentById(departmentId);
            if (existingOpt.isEmpty()) {
                JSONUtil.sendNotFound(response, "Department not found");
                return;
            }
            
            Department department = existingOpt.get();
            
            // Update fields
            if (jsonRequest.has("name") && !jsonRequest.get("name").isJsonNull()) {
                String name = jsonRequest.get("name").getAsString().trim();
                if (!ValidationUtil.isNotEmpty(name)) {
                    JSONUtil.sendBadRequest(response, "Department name is required");
                    return;
                }
                if (name.length() > 100) {
                    JSONUtil.sendBadRequest(response, "Department name must be less than 100 characters");
                    return;
                }
                department.setName(name);
            }
            
            if (jsonRequest.has("description") && !jsonRequest.get("description").isJsonNull()) {
                department.setDescription(jsonRequest.get("description").getAsString());
            }
            
            // Update department
            if (departmentService.updateDepartment(department, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Department updated successfully", department);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to update department");
            }
            
        } catch (Exception e) {
            System.err.println("Department PUT error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while updating department");
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
            if (pathInfo == null || pathInfo.equals("/")) {
                JSONUtil.sendBadRequest(response, "Department ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int departmentId;
            try {
                departmentId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid department ID");
                return;
            }
            
            // Check if department has employees
            if (departmentService.hasEmployees(departmentId)) {
                JSONUtil.sendBadRequest(response, "Cannot delete department with employees. Move or delete employees first.");
                return;
            }
            
            // Delete department
            if (departmentService.deleteDepartment(departmentId, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Department deleted successfully", null);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to delete department. Department may not exist.");
            }
            
        } catch (Exception e) {
            System.err.println("Department DELETE error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while deleting department");
        }
    }
    
    /**
     * Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

