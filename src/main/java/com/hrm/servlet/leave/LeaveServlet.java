package com.hrm.servlet.leave;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hrm.model.LeaveRequest;
import com.hrm.model.User;
import com.hrm.service.LeaveService;
import com.hrm.util.JSONUtil;
import com.hrm.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Servlet for leave request operations
 */
public class LeaveServlet extends HttpServlet {
    
    private final LeaveService leaveService;
    private final DateTimeFormatter dateFormatter;
    
    public LeaveServlet() {
        this.leaveService = new LeaveService();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            
            // GET /api/leave - Get leave requests
            if (pathInfo == null || pathInfo.equals("/")) {
                List<LeaveRequest> leaveRequests;
                
                // Employees can only see their own leave requests
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (currentUser.getEmployeeId() == null) {
                        JSONUtil.sendSuccess(response, "No employee profile", List.of());
                        return;
                    }
                    leaveRequests = leaveService.getLeaveRequestsByEmployee(currentUser.getEmployeeId());
                } else {
                    // Check for status filter
                    String statusParam = request.getParameter("status");
                    if (statusParam != null && !statusParam.isEmpty()) {
                        try {
                            LeaveRequest.LeaveStatus status = LeaveRequest.LeaveStatus.valueOf(
                                statusParam.toUpperCase());
                            leaveRequests = leaveService.getLeaveRequestsByStatus(status);
                        } catch (IllegalArgumentException e) {
                            JSONUtil.sendBadRequest(response, "Invalid status filter");
                            return;
                        }
                    } else {
                        leaveRequests = leaveService.getAllLeaveRequests();
                    }
                }
                
                JSONUtil.sendSuccess(response, "Leave requests retrieved", leaveRequests);
                return;
            }
            
            // GET /api/leave/{id} - Get specific leave request
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length >= 2) {
                int leaveRequestId;
                try {
                    leaveRequestId = Integer.parseInt(pathParts[1]);
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid leave request ID");
                    return;
                }
                
                Optional<LeaveRequest> leaveRequestOpt = leaveService.getLeaveRequestById(leaveRequestId);
                
                if (leaveRequestOpt.isEmpty()) {
                    JSONUtil.sendNotFound(response, "Leave request not found");
                    return;
                }
                
                LeaveRequest leaveRequest = leaveRequestOpt.get();
                
                // Check access
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (leaveRequest.getEmployeeId() != currentUser.getEmployeeId()) {
                        JSONUtil.sendForbidden(response, "You can only view your own leave requests");
                        return;
                    }
                }
                
                JSONUtil.sendSuccess(response, "Leave request retrieved", leaveRequest);
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid request");
            
        } catch (Exception e) {
            System.err.println("Leave GET error: " + e.getMessage());
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
            
            // Required fields
            Integer employeeId = null;
            if (jsonRequest.has("employeeId") && !jsonRequest.get("employeeId").isJsonNull()) {
                employeeId = jsonRequest.get("employeeId").getAsInt();
            } else if (currentUser.getEmployeeId() != null) {
                employeeId = currentUser.getEmployeeId();
            }
            
            if (employeeId == null) {
                JSONUtil.sendBadRequest(response, "Employee ID is required");
                return;
            }
            
            String leaveTypeStr = jsonRequest.has("leaveType") ? 
                jsonRequest.get("leaveType").getAsString().trim().toUpperCase() : null;
            
            LeaveRequest.LeaveType leaveType;
            try {
                leaveType = LeaveRequest.LeaveType.valueOf(leaveTypeStr);
            } catch (IllegalArgumentException e) {
                JSONUtil.sendBadRequest(response, "Invalid leave type");
                return;
            }
            
            String startDateStr = jsonRequest.has("startDate") ? 
                jsonRequest.get("startDate").getAsString().trim() : null;
            String endDateStr = jsonRequest.has("endDate") ? 
                jsonRequest.get("endDate").getAsString().trim() : null;
            
            LocalDate startDate = ValidationUtil.parseDate(startDateStr, dateFormatter);
            LocalDate endDate = ValidationUtil.parseDate(endDateStr, dateFormatter);
            
            if (startDate == null) {
                JSONUtil.sendBadRequest(response, "Valid start date (yyyy-MM-dd) is required");
                return;
            }
            
            if (endDate == null) {
                JSONUtil.sendBadRequest(response, "Valid end date (yyyy-MM-dd) is required");
                return;
            }
            
            if (!ValidationUtil.isValidDateRange(startDate, endDate)) {
                JSONUtil.sendBadRequest(response, "End date must be on or after start date");
                return;
            }
            
            String reason = jsonRequest.has("reason") ? 
                jsonRequest.get("reason").getAsString() : null;
            if (reason != null && !ValidationUtil.isValidReason(reason)) {
                JSONUtil.sendBadRequest(response, "Reason is too long");
                return;
            }
            
            // Create leave request
            LeaveRequest leaveRequest = new LeaveRequest();
            leaveRequest.setEmployeeId(employeeId);
            leaveRequest.setLeaveType(leaveType);
            leaveRequest.setStartDate(startDate);
            leaveRequest.setEndDate(endDate);
            leaveRequest.setReason(reason);
            
            int leaveRequestId = leaveService.applyForLeave(leaveRequest, currentUser.getId(), ipAddress);
            
            if (leaveRequestId > 0) {
                Optional<LeaveRequest> createdRequest = leaveService.getLeaveRequestById(leaveRequestId);
                JSONUtil.sendCreated(response, "Leave request submitted successfully", createdRequest.orElse(null));
            } else if (leaveRequestId == -2) {
                JSONUtil.sendBadRequest(response, "You already have a leave request for this period");
            } else {
                JSONUtil.sendBadRequest(response, "Failed to submit leave request");
            }
            
        } catch (Exception e) {
            System.err.println("Leave POST error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while submitting leave request");
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
                JSONUtil.sendBadRequest(response, "Leave request ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int leaveRequestId;
            try {
                leaveRequestId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid leave request ID");
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
            
            // Get existing leave request
            Optional<LeaveRequest> existingOpt = leaveService.getLeaveRequestById(leaveRequestId);
            if (existingOpt.isEmpty()) {
                JSONUtil.sendNotFound(response, "Leave request not found");
                return;
            }
            
            LeaveRequest leaveRequest = existingOpt.get();
            
            // Update fields (only allowed for pending requests)
            if (leaveRequest.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
                JSONUtil.sendBadRequest(response, "Cannot update a processed leave request");
                return;
            }
            
            if (jsonRequest.has("leaveType") && !jsonRequest.get("leaveType").isJsonNull()) {
                try {
                    leaveRequest.setLeaveType(LeaveRequest.LeaveType.valueOf(
                        jsonRequest.get("leaveType").getAsString().trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    JSONUtil.sendBadRequest(response, "Invalid leave type");
                    return;
                }
            }
            
            if (jsonRequest.has("startDate") && !jsonRequest.get("startDate").isJsonNull()) {
                LocalDate startDate = ValidationUtil.parseDate(
                    jsonRequest.get("startDate").getAsString().trim(), dateFormatter);
                if (startDate == null) {
                    JSONUtil.sendBadRequest(response, "Invalid start date format");
                    return;
                }
                leaveRequest.setStartDate(startDate);
            }
            
            if (jsonRequest.has("endDate") && !jsonRequest.get("endDate").isJsonNull()) {
                LocalDate endDate = ValidationUtil.parseDate(
                    jsonRequest.get("endDate").getAsString().trim(), dateFormatter);
                if (endDate == null) {
                    JSONUtil.sendBadRequest(response, "Invalid end date format");
                    return;
                }
                leaveRequest.setEndDate(endDate);
            }
            
            if (jsonRequest.has("reason") && !jsonRequest.get("reason").isJsonNull()) {
                String reason = jsonRequest.get("reason").getAsString();
                if (!ValidationUtil.isValidReason(reason)) {
                    JSONUtil.sendBadRequest(response, "Reason is too long");
                    return;
                }
                leaveRequest.setReason(reason);
            }
            
            // Update leave request
            if (leaveService.updateLeaveRequest(leaveRequest, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Leave request updated successfully", leaveRequest);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to update leave request");
            }
            
        } catch (Exception e) {
            System.err.println("Leave PUT error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while updating leave request");
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
                JSONUtil.sendBadRequest(response, "Leave request ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int leaveRequestId;
            try {
                leaveRequestId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid leave request ID");
                return;
            }
            
            // Delete leave request
            if (leaveService.deleteLeaveRequest(leaveRequestId, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Leave request deleted successfully", null);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to delete leave request. Only pending requests can be deleted.");
            }
            
        } catch (Exception e) {
            System.err.println("Leave DELETE error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while deleting leave request");
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

