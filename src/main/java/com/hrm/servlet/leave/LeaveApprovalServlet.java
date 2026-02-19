package com.hrm.servlet.leave;

import com.hrm.model.LeaveRequest;
import com.hrm.model.User;
import com.hrm.service.LeaveService;
import com.hrm.util.JSONUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Servlet for leave approval/rejection operations
 */
public class LeaveApprovalServlet extends HttpServlet {
    
    private final LeaveService leaveService;
    
    public LeaveApprovalServlet() {
        this.leaveService = new LeaveService();
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
            
            // Parse path: /{id}/approve or /{id}/reject
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 3) {
                JSONUtil.sendBadRequest(response, "Invalid request format");
                return;
            }
            
            int leaveRequestId;
            try {
                leaveRequestId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid leave request ID");
                return;
            }
            
            String action = pathParts[2].toLowerCase();
            
            // Get existing leave request
            Optional<LeaveRequest> leaveRequestOpt = leaveService.getLeaveRequestById(leaveRequestId);
            if (leaveRequestOpt.isEmpty()) {
                JSONUtil.sendNotFound(response, "Leave request not found");
                return;
            }
            
            LeaveRequest leaveRequest = leaveRequestOpt.get();
            
            // Check if leave request is already processed
            if (leaveRequest.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
                JSONUtil.sendBadRequest(response, "Leave request has already been processed");
                return;
            }
            
            boolean success;
            String message;
            
            if ("approve".equals(action)) {
                success = leaveService.approveLeaveRequest(leaveRequestId, currentUser.getId(), ipAddress);
                message = success ? "Leave request approved successfully" : "Failed to approve leave request";
            } else if ("reject".equals(action)) {
                success = leaveService.rejectLeaveRequest(leaveRequestId, currentUser.getId(), ipAddress);
                message = success ? "Leave request rejected successfully" : "Failed to reject leave request";
            } else {
                JSONUtil.sendBadRequest(response, "Invalid action. Use 'approve' or 'reject'");
                return;
            }
            
            if (success) {
                // Get updated leave request
                Optional<LeaveRequest> updatedRequest = leaveService.getLeaveRequestById(leaveRequestId);
                JSONUtil.sendSuccess(response, message, updatedRequest.orElse(null));
            } else {
                JSONUtil.sendBadRequest(response, message);
            }
            
        } catch (Exception e) {
            System.err.println("Leave approval error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while processing leave request");
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

