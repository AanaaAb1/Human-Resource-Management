package com.hrm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Attendance model class representing employee attendance records
 */
public class Attendance {
    private int id;
    private int employeeId;
    private String employeeName; // For display purposes
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private LocalDateTime createdAt;
    
    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LEAVE
    }
    
    // Constructors
    public Attendance() {}
    
    public Attendance(int id, int employeeId, LocalDate attendanceDate, AttendanceStatus status) {
        this.id = id;
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.status = status;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }
    
    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }
    
    public AttendanceStatus getStatus() {
        return status;
    }
    
    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", employeeId=" + employeeId +
                ", date=" + attendanceDate +
                ", status=" + status +
                '}';
    }
}

