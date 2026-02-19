package com.hrm.dao;

import com.hrm.config.DatabaseConfig;
import com.hrm.model.Attendance;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Attendance operations
 */
public class AttendanceDAO {
    
    private static final String TABLE_NAME = "attendance";
    
    /**
     * Find attendance by ID
     * @param id Attendance ID
     * @return Optional containing attendance if found
     */
    public Optional<Attendance> findById(int id) {
        String sql = "SELECT a.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " a JOIN employees e ON a.employee_id = e.id WHERE a.id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToAttendance(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding attendance by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find attendance by employee and date
     * @param employeeId Employee ID
     * @param date Attendance date
     * @return Optional containing attendance if found
     */
    public Optional<Attendance> findByEmployeeAndDate(int employeeId, LocalDate date) {
        String sql = "SELECT a.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " a JOIN employees e ON a.employee_id = e.id " +
                     " WHERE a.employee_id = ? AND a.attendance_date = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            statement.setDate(2, Date.valueOf(date));
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToAttendance(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding attendance by employee and date: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Get attendance for employee
     * @param employeeId Employee ID
     * @return List of attendance records
     */
    public List<Attendance> findByEmployee(int employeeId) {
        List<Attendance> attendanceList = new ArrayList<>();
        String sql = "SELECT a.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " a JOIN employees e ON a.employee_id = e.id " +
                     " WHERE a.employee_id = ? ORDER BY a.attendance_date DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    attendanceList.add(mapResultSetToAttendance(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding attendance by employee: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Get attendance for employee within date range
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records
     */
    public List<Attendance> findByEmployeeAndDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendanceList = new ArrayList<>();
        String sql = "SELECT a.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " a JOIN employees e ON a.employee_id = e.id " +
                     " WHERE a.employee_id = ? AND a.attendance_date BETWEEN ? AND ? " +
                     " ORDER BY a.attendance_date DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            statement.setDate(2, Date.valueOf(startDate));
            statement.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    attendanceList.add(mapResultSetToAttendance(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding attendance by date range: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Get all attendance for a specific date
     * @param date Attendance date
     * @return List of attendance records
     */
    public List<Attendance> findByDate(LocalDate date) {
        List<Attendance> attendanceList = new ArrayList<>();
        String sql = "SELECT a.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " a JOIN employees e ON a.employee_id = e.id " +
                     " WHERE a.attendance_date = ? ORDER BY e.full_name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setDate(1, Date.valueOf(date));
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    attendanceList.add(mapResultSetToAttendance(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding attendance by date: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Get all attendance
     * @return List of all attendance records
     */
    public List<Attendance> findAll() {
        List<Attendance> attendanceList = new ArrayList<>();
        String sql = "SELECT a.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " a JOIN employees e ON a.employee_id = e.id " +
                     " ORDER BY a.attendance_date DESC, e.full_name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                attendanceList.add(mapResultSetToAttendance(resultSet));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all attendance: " + e.getMessage());
        }
        
        return attendanceList;
    }
    
    /**
     * Create new attendance record
     * @param attendance Attendance to create
     * @return Created attendance ID, -1 if failed
     */
    public int create(Attendance attendance) {
        String sql = "INSERT INTO " + TABLE_NAME + " (employee_id, attendance_date, status) VALUES (?, ?, ?)";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setInt(1, attendance.getEmployeeId());
            statement.setDate(2, Date.valueOf(attendance.getAttendanceDate()));
            statement.setString(3, attendance.getStatus().name());
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating attendance: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Update attendance
     * @param attendance Attendance to update
     * @return true if successful
     */
    public boolean update(Attendance attendance) {
        String sql = "UPDATE " + TABLE_NAME + " SET status = ? WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, attendance.getStatus().name());
            statement.setInt(2, attendance.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating attendance: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Delete attendance by ID
     * @param id Attendance ID
     * @return true if successful
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting attendance: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Count attendance by status for employee
     * @param employeeId Employee ID
     * @param status Attendance status
     * @return Count
     */
    public int countByEmployeeAndStatus(int employeeId, Attendance.AttendanceStatus status) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE employee_id = ? AND status = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            statement.setString(2, status.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting attendance by status: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Map ResultSet to Attendance object
     * @param resultSet ResultSet
     * @return Attendance object
     * @throws SQLException if mapping fails
     */
    private Attendance mapResultSetToAttendance(ResultSet resultSet) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setId(resultSet.getInt("id"));
        attendance.setEmployeeId(resultSet.getInt("employee_id"));
        attendance.setEmployeeName(resultSet.getString("employee_name"));
        
        Date attendanceDate = resultSet.getDate("attendance_date");
        if (attendanceDate != null) {
            attendance.setAttendanceDate(attendanceDate.toLocalDate());
        }
        
        attendance.setStatus(Attendance.AttendanceStatus.valueOf(resultSet.getString("status")));
        return attendance;
    }
}

