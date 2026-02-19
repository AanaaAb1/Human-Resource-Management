package com.hrm.servlet.payroll;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hrm.model.Payroll;
import com.hrm.model.User;
import com.hrm.service.PayrollService;
import com.hrm.util.JSONUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servlet for payroll operations
 */
public class PayrollServlet extends HttpServlet {
    
    private final PayrollService payrollService;
    
    public PayrollServlet() {
        this.payrollService = new PayrollService();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            
            // GET /api/payroll - Get all payroll (for HR/Admin) or own payroll (for employees)
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Payroll> payrollList;
                
                // Employees can only see their own payroll
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (currentUser.getEmployeeId() == null) {
                        JSONUtil.sendSuccess(response, "No employee profile", List.of());
                        return;
                    }
                    payrollList = payrollService.getPayrollByEmployee(currentUser.getEmployeeId());
                } else {
                    // Check for filters
                    String monthParam = request.getParameter("month");
                    String yearParam = request.getParameter("year");
                    String employeeIdParam = request.getParameter("employeeId");
                    
                    if (monthParam != null && yearParam != null) {
                        try {
                            int month = Integer.parseInt(monthParam);
                            int year = Integer.parseInt(yearParam);
                            
                            if (employeeIdParam != null) {
                                int employeeId = Integer.parseInt(employeeIdParam);
                                payrollList = payrollService.getPayrollByEmployee(employeeId);
                            } else {
                                payrollList = payrollService.getPayrollByMonthYear(month, year);
                            }
                        } catch (NumberFormatException e) {
                            JSONUtil.sendBadRequest(response, "Invalid month or year");
                            return;
                        }
                    } else if (employeeIdParam != null) {
                        try {
                            int employeeId = Integer.parseInt(employeeIdParam);
                            payrollList = payrollService.getPayrollByEmployee(employeeId);
                        } catch (NumberFormatException e) {
                            JSONUtil.sendBadRequest(response, "Invalid employee ID");
                            return;
                        }
                    } else if (yearParam != null) {
                        try {
                            int year = Integer.parseInt(yearParam);
                            payrollList = payrollService.getPayrollByYear(year);
                        } catch (NumberFormatException e) {
                            JSONUtil.sendBadRequest(response, "Invalid year");
                            return;
                        }
                    } else {
                        payrollList = payrollService.getAllPayroll();
                    }
                }
                
                JSONUtil.sendSuccess(response, "Payroll retrieved", payrollList);
                return;
            }
            
            // GET /api/payroll/{id} - Get specific payroll
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length >= 2) {
                int payrollId;
                try {
                    payrollId = Integer.parseInt(pathParts[1]);
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid payroll ID");
                    return;
                }
                
                Optional<Payroll> payrollOpt = payrollService.getPayrollById(payrollId);
                
                if (payrollOpt.isEmpty()) {
                    JSONUtil.sendNotFound(response, "Payroll record not found");
                    return;
                }
                
                Payroll payroll = payrollOpt.get();
                
                // Check access
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (payroll.getEmployeeId() != currentUser.getEmployeeId()) {
                        JSONUtil.sendForbidden(response, "You can only view your own payroll");
                        return;
                    }
                }
                
                JSONUtil.sendSuccess(response, "Payroll retrieved", payroll);
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid request");
            
        } catch (Exception e) {
            System.err.println("Payroll GET error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
            // POST /api/payroll/generate - Generate payroll
            if (pathInfo != null && pathInfo.equals("/generate")) {
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
                
                // Required fields
                Integer employeeId = null;
                if (jsonRequest.has("employeeId") && !jsonRequest.get("employeeId").isJsonNull()) {
                    employeeId = jsonRequest.get("employeeId").getAsInt();
                }
                
                Integer month = null;
                if (jsonRequest.has("month") && !jsonRequest.get("month").isJsonNull()) {
                    month = jsonRequest.get("month").getAsInt();
                }
                
                Integer year = null;
                if (jsonRequest.has("year") && !jsonRequest.get("year").isJsonNull()) {
                    year = jsonRequest.get("year").getAsInt();
                }
                
                if (employeeId == null || month == null || year == null) {
                    JSONUtil.sendBadRequest(response, "Employee ID, month, and year are required");
                    return;
                }
                
                if (month < 1 || month > 12) {
                    JSONUtil.sendBadRequest(response, "Month must be between 1 and 12");
                    return;
                }
                
                if (year < 2000 || year > 2100) {
                    JSONUtil.sendBadRequest(response, "Invalid year");
                    return;
                }
                
                // Optional custom allowances and deductions
                BigDecimal allowances = null;
                BigDecimal deductions = null;
                
                if (jsonRequest.has("allowances") && !jsonRequest.get("allowances").isJsonNull()) {
                    try {
                        allowances = new BigDecimal(jsonRequest.get("allowances").getAsString());
                    } catch (NumberFormatException e) {
                        JSONUtil.sendBadRequest(response, "Invalid allowances format");
                        return;
                    }
                }
                
                if (jsonRequest.has("deductions") && !jsonRequest.get("deductions").isJsonNull()) {
                    try {
                        deductions = new BigDecimal(jsonRequest.get("deductions").getAsString());
                    } catch (NumberFormatException e) {
                        JSONUtil.sendBadRequest(response, "Invalid deductions format");
                        return;
                    }
                }
                
                // Generate payroll
                int payrollId = payrollService.generatePayroll(
                    employeeId, month, year, allowances, deductions, currentUser.getId(), ipAddress);
                
                if (payrollId > 0) {
                    Optional<Payroll> generatedPayroll = payrollService.getPayrollById(payrollId);
                    JSONUtil.sendCreated(response, "Payroll generated successfully", 
                        generatedPayroll.orElse(null));
                } else {
                    JSONUtil.sendBadRequest(response, "Failed to generate payroll. Employee may not exist.");
                }
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid endpoint. Use /api/payroll/generate");
            
        } catch (Exception e) {
            System.err.println("Payroll POST error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while generating payroll");
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
                JSONUtil.sendBadRequest(response, "Payroll ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int payrollId;
            try {
                payrollId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid payroll ID");
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
            
            // Get existing payroll
            Optional<Payroll> existingOpt = payrollService.getPayrollById(payrollId);
            if (existingOpt.isEmpty()) {
                JSONUtil.sendNotFound(response, "Payroll record not found");
                return;
            }
            
            Payroll payroll = existingOpt.get();
            
            // Update fields
            if (jsonRequest.has("allowances") && !jsonRequest.get("allowances").isJsonNull()) {
                try {
                    payroll.setAllowances(new BigDecimal(
                        jsonRequest.get("allowances").getAsString()));
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid allowances format");
                    return;
                }
            }
            
            if (jsonRequest.has("deductions") && !jsonRequest.get("deductions").isJsonNull()) {
                try {
                    payroll.setDeductions(new BigDecimal(
                        jsonRequest.get("deductions").getAsString()));
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid deductions format");
                    return;
                }
            }
            
            // Update payroll
            if (payrollService.updatePayroll(payroll, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Payroll updated successfully", payroll);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to update payroll");
            }
            
        } catch (Exception e) {
            System.err.println("Payroll PUT error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while updating payroll");
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
                JSONUtil.sendBadRequest(response, "Payroll ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int payrollId;
            try {
                payrollId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid payroll ID");
                return;
            }
            
            // Delete payroll
            if (payrollService.deletePayroll(payrollId, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Payroll deleted successfully", null);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to delete payroll");
            }
            
        } catch (Exception e) {
            System.err.println("Payroll DELETE error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while deleting payroll");
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

