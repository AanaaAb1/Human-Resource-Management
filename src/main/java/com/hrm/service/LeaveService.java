package com.hrm.service;

import com.hrm.dao.AuditLogDAO;
import com.hrm.dao.LeaveDAO;
import com.hrm.model.LeaveRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service class for leave request operations
 */
public class LeaveService {
    
    private final LeaveDAO leaveDAO;
    private final AuditLogDAO auditLogDAO;
    
    public LeaveService() {
        this.leaveDAO = new LeaveDAO();
        this.auditLogDAO = new AuditLogDAO();
    }
    
    /**
     * Get leave request by ID
     * @param id Leave request ID
     * @return Optional containing leave request if found
     */
    public Optional<LeaveRequest> getLeaveRequestById(int id) {
        return leaveDAO.findById(id);
    }
    
    /**
     * Get leave requests for employee
     * @param employeeId Employee ID
     * @return List of leave requests
     */
    public List<LeaveRequest> getLeaveRequestsByEmployee(int employeeId) {
        return leaveDAO.findByEmployee(employeeId);
    }
    
    /**
     * Get leave requests by status
     * @param status Leave status
     * @return List of leave requests
     */
    public List<LeaveRequest> getLeaveRequestsByStatus(LeaveRequest.LeaveStatus status) {
        return leaveDAO.findByStatus(status);
    }
    
    /**
     * Get pending leave requests
     * @return List of pending leave requests
     */
    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveDAO.findPending();
    }
    
    /**
     * Get all leave requests
     * @return List of all leave requests
     */
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveDAO.findAll();
    }
    
    /**
     * Apply for leave
     * @param leaveRequest Leave request to create
     * @param appliedByUserId User ID applying for leave
     * @param ipAddress Client IP address
     * @return Created leave request ID, -1 if failed
     */
    public int applyForLeave(LeaveRequest leaveRequest, Integer appliedByUserId, String ipAddress) {
        // Validate date range
        if (!com.hrm.util.ValidationUtil.isValidDateRange(leaveRequest.getStartDate(), leaveRequest.getEndDate())) {
            return -1;
        }
        
        // Check for overlapping leave requests
        if (leaveDAO.hasOverlappingLeave(
            leaveRequest.getEmployeeId(), 
            leaveRequest.getStartDate(), 
            leaveRequest.getEndDate(), 
            null
        )) {
            return -2; // Overlapping leave exists
        }
        
        // Set status to pending
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        
        int leaveRequestId = leaveDAO.create(leaveRequest);
        
        if (leaveRequestId > 0) {
            // Log the application
            auditLogDAO.logDataModification(
                appliedByUserId, "CREATE", "LeaveRequest", leaveRequestId,
                "Applied for leave: " + leaveRequest.getLeaveType() + 
                ", From: " + leaveRequest.getStartDate() + 
                " To: " + leaveRequest.getEndDate(), ipAddress
            );
        }
        
        return leaveRequestId;
    }
    
    /**
     * Approve leave request
     * @param leaveRequestId Leave request ID
     * @param approvedByUserId User ID approving the leave
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean approveLeaveRequest(int leaveRequestId, Integer approvedByUserId, String ipAddress) {
        Optional<LeaveRequest> leaveRequestOpt = leaveDAO.findById(leaveRequestId);
        
        if (leaveRequestOpt.isEmpty()) {
            return false;
        }
        
        LeaveRequest leaveRequest = leaveRequestOpt.get();
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approvedByUserId);
        
        boolean updated = leaveDAO.updateStatus(leaveRequest);
        
        if (updated) {
            // Log the approval
            auditLogDAO.logDataModification(
                approvedByUserId, "APPROVE", "LeaveRequest", leaveRequestId,
                "Approved leave for employee: " + leaveRequest.getEmployeeId() + 
                ", From: " + leaveRequest.getStartDate() + 
                " To: " + leaveRequest.getEndDate(), ipAddress
            );
        }
        
        return updated;
    }
    
    /**
     * Reject leave request
     * @param leaveRequestId Leave request ID
     * @param rejectedByUserId User ID rejecting the leave
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean rejectLeaveRequest(int leaveRequestId, Integer rejectedByUserId, String ipAddress) {
        Optional<LeaveRequest> leaveRequestOpt = leaveDAO.findById(leaveRequestId);
        
        if (leaveRequestOpt.isEmpty()) {
            return false;
        }
        
        LeaveRequest leaveRequest = leaveRequestOpt.get();
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(rejectedByUserId);
        
        boolean updated = leaveDAO.updateStatus(leaveRequest);
        
        if (updated) {
            // Log the rejection
            auditLogDAO.logDataModification(
                rejectedByUserId, "REJECT", "LeaveRequest", leaveRequestId,
                "Rejected leave for employee: " + leaveRequest.getEmployeeId() + 
                ", From: " + leaveRequest.getStartDate() + 
                " To: " + leaveRequest.getEndDate(), ipAddress
            );
        }
        
        return updated;
    }
    
    /**
     * Update leave request (only if pending)
     * @param leaveRequest Leave request to update
     * @param updatedByUserId User ID updating the request
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean updateLeaveRequest(LeaveRequest leaveRequest, Integer updatedByUserId, String ipAddress) {
        Optional<LeaveRequest> existingOpt = leaveDAO.findById(leaveRequest.getId());
        
        if (existingOpt.isEmpty()) {
            return false;
        }
        
        LeaveRequest existing = existingOpt.get();
        
        // Only pending requests can be updated
        if (existing.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
            return false;
        }
        
        // Check for overlapping leave requests
        if (leaveDAO.hasOverlappingLeave(
            leaveRequest.getEmployeeId(), 
            leaveRequest.getStartDate(), 
            leaveRequest.getEndDate(), 
            leaveRequest.getId()
        )) {
            return false;
        }
        
        boolean updated = leaveDAO.update(leaveRequest);
        
        if (updated) {
            // Log the update
            auditLogDAO.logDataModification(
                updatedByUserId, "UPDATE", "LeaveRequest", leaveRequest.getId(),
                "Updated leave request", ipAddress
            );
        }
        
        return updated;
    }
    
    /**
     * Delete leave request (only if pending)
     * @param id Leave request ID to delete
     * @param deletedByUserId User ID deleting the request
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean deleteLeaveRequest(int id, Integer deletedByUserId, String ipAddress) {
        Optional<LeaveRequest> leaveRequestOpt = leaveDAO.findById(id);
        
        if (leaveRequestOpt.isEmpty()) {
            return false;
        }
        
        LeaveRequest leaveRequest = leaveRequestOpt.get();
        
        // Only pending requests can be deleted
        if (leaveRequest.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
            return false;
        }
        
        boolean deleted = leaveDAO.delete(id);
        
        if (deleted) {
            // Log the deletion
            auditLogDAO.logDataModification(
                deletedByUserId, "DELETE", "LeaveRequest", id,
                "Deleted leave request", ipAddress
            );
        }
        
        return deleted;
    }
    
    /**
     * Check if employee is on leave on a specific date
     * @param employeeId Employee ID
     * @param date Date to check
     * @return true if on approved leave
     */
    public boolean isOnLeave(int employeeId, LocalDate date) {
        List<LeaveRequest> approvedLeaves = leaveDAO.findByEmployee(employeeId);
        
        for (LeaveRequest leave : approvedLeaves) {
            if (leave.getStatus() == LeaveRequest.LeaveStatus.APPROVED) {
                if (!date.isBefore(leave.getStartDate()) && !date.isAfter(leave.getEndDate())) {
                    return true;
                }
            }
        }
        
        return false;
    }
}

