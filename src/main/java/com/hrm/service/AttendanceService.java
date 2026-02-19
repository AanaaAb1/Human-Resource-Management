package com.hrm.service;

import com.hrm.dao.AuditLogDAO;
import com.hrm.dao.AttendanceDAO;
import com.hrm.model.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service class for attendance operations
 */
public class AttendanceService {
    
    private final AttendanceDAO attendanceDAO;
    private final AuditLogDAO auditLogDAO;
    
    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAO();
        this.auditLogDAO = new AuditLogDAO();
    }
    
    /**
     * Get attendance by ID
     * @param id Attendance ID
     * @return Optional containing attendance if found
     */
    public Optional<Attendance> getAttendanceById(int id) {
        return attendanceDAO.findById(id);
    }
    
    /**
     * Get attendance for employee
     * @param employeeId Employee ID
     * @return List of attendance records
     */
    public List<Attendance> getAttendanceByEmployee(int employeeId) {
        return attendanceDAO.findByEmployee(employeeId);
    }
    
    /**
     * Get attendance for employee within date range
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records
     */
    public List<Attendance> getAttendanceByEmployeeAndDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceDAO.findByEmployeeAndDateRange(employeeId, startDate, endDate);
    }
    
    /**
     * Get attendance for specific date
     * @param date Attendance date
     * @return List of attendance records
     */
    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceDAO.findByDate(date);
    }
    
    /**
     * Get all attendance records
     * @return List of all attendance records
     */
    public List<Attendance> getAllAttendance() {
        return attendanceDAO.findAll();
    }
    
    /**
     * Record attendance
     * @param attendance Attendance to record
     * @param recordedByUserId User ID recording the attendance
     * @param ipAddress Client IP address
     * @return Created attendance ID, -1 if failed
     */
    public int recordAttendance(Attendance attendance, Integer recordedByUserId, String ipAddress) {
        // Check if attendance already exists for this employee on this date
        Optional<Attendance> existing = attendanceDAO.findByEmployeeAndDate(
            attendance.getEmployeeId(), attendance.getAttendanceDate()
        );
        
        if (existing.isPresent()) {
            // Update existing attendance
            attendance.setId(existing.get().getId());
            boolean updated = attendanceDAO.update(attendance);
            return updated ? attendance.getId() : -1;
        }
        
        int attendanceId = attendanceDAO.create(attendance);
        
        if (attendanceId > 0) {
            // Log the creation
            auditLogDAO.logDataModification(
                recordedByUserId, "CREATE", "Attendance", attendanceId,
                "Recorded attendance for employee: " + attendance.getEmployeeId() + 
                ", Date: " + attendance.getAttendanceDate() + 
                ", Status: " + attendance.getStatus(), ipAddress
            );
        }
        
        return attendanceId;
    }
    
    /**
     * Update attendance
     * @param attendance Attendance to update
     * @param updatedByUserId User ID updating the attendance
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean updateAttendance(Attendance attendance, Integer updatedByUserId, String ipAddress) {
        boolean updated = attendanceDAO.update(attendance);
        
        if (updated) {
            // Log the update
            auditLogDAO.logDataModification(
                updatedByUserId, "UPDATE", "Attendance", attendance.getId(),
                "Updated attendance for employee: " + attendance.getEmployeeId() + 
                ", Status: " + attendance.getStatus(), ipAddress
            );
        }
        
        return updated;
    }
    
    /**
     * Delete attendance
     * @param id Attendance ID to delete
     * @param deletedByUserId User ID deleting the attendance
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean deleteAttendance(int id, Integer deletedByUserId, String ipAddress) {
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(id);
        
        if (attendanceOpt.isEmpty()) {
            return false;
        }
        
        Attendance attendance = attendanceOpt.get();
        boolean deleted = attendanceDAO.delete(id);
        
        if (deleted) {
            // Log the deletion
            auditLogDAO.logDataModification(
                deletedByUserId, "DELETE", "Attendance", id,
                "Deleted attendance for employee: " + attendance.getEmployeeId() + 
                ", Date: " + attendance.getAttendanceDate(), ipAddress
            );
        }
        
        return deleted;
    }
    
    /**
     * Get attendance count by status for employee
     * @param employeeId Employee ID
     * @param status Attendance status
     * @return Count
     */
    public int getAttendanceCountByStatus(int employeeId, Attendance.AttendanceStatus status) {
        return attendanceDAO.countByEmployeeAndStatus(employeeId, status);
    }
    
    /**
     * Get today's attendance for employee
     * @param employeeId Employee ID
     * @return Optional containing today's attendance
     */
    public Optional<Attendance> getTodayAttendance(int employeeId) {
        return attendanceDAO.findByEmployeeAndDate(employeeId, LocalDate.now());
    }
}

