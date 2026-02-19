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
import java.util.Optional;

/**
 * Servlet for generating payroll
 */
public class PayrollGenerateServlet extends HttpServlet {
    
    private final PayrollService payrollService;
    
    public PayrollGenerateServlet() {
        this.payrollService = new PayrollService();
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
            } else if (payrollId == -2) {
                JSONUtil.sendBadRequest(response, "Payroll for this employee and period already exists");
            } else {
                JSONUtil.sendBadRequest(response, "Failed to generate payroll. Employee may not exist.");
            }
            
        } catch (Exception e) {
            System.err.println("Payroll generation error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while generating payroll");
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

