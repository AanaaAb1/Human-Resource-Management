package com.hrm.dao;

import com.hrm.config.DatabaseConfig;
import com.hrm.model.LeaveRequest;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Leave Request operations
 */
public class LeaveDAO {
    
    private static final String TABLE_NAME = "leave_requests";
    
    /**
     * Find leave request by ID
     * @param id Leave request ID
     * @return Optional containing leave request if found
     */
    public Optional<LeaveRequest> findById(int id) {
        String sql = "SELECT lr.*, e.full_name as employee_name, " +
                     " (SELECT full_name FROM employees WHERE id = lr.approved_by) as approved_by_name " +
                     " FROM " + TABLE_NAME + " lr " +
                     " JOIN employees e ON lr.employee_id = e.id " +
                     " WHERE lr.id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToLeaveRequest(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding leave request by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find leave requests by employee
     * @param employeeId Employee ID
     * @return List of leave requests
     */
    public List<LeaveRequest> findByEmployee(int employeeId) {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String sql = "SELECT lr.*, e.full_name as employee_name, " +
                     " (SELECT full_name FROM employees WHERE id = lr.approved_by) as approved_by_name " +
                     " FROM " + TABLE_NAME + " lr " +
                     " JOIN employees e ON lr.employee_id = e.id " +
                     " WHERE lr.employee_id = ? " +
                     " ORDER BY lr.created_at DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    leaveRequests.add(mapResultSetToLeaveRequest(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding leave requests by employee: " + e.getMessage());
        }
        
        return leaveRequests;
    }
    
    /**
     * Find leave requests by status
     * @param status Leave status
     * @return List of leave requests
     */
    public List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status) {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String sql = "SELECT lr.*, e.full_name as employee_name, " +
                     " (SELECT full_name FROM employees WHERE id = lr.approved_by) as approved_by_name " +
                     " FROM " + TABLE_NAME + " lr " +
                     " JOIN employees e ON lr.employee_id = e.id " +
                     " WHERE lr.status = ? " +
                     " ORDER BY lr.created_at DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, status.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    leaveRequests.add(mapResultSetToLeaveRequest(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding leave requests by status: " + e.getMessage());
        }
        
        return leaveRequests;
    }
    
    /**
     * Find pending leave requests (for HR/Admin)
     * @return List of pending leave requests
     */
    public List<LeaveRequest> findPending() {
        return findByStatus(LeaveRequest.LeaveStatus.PENDING);
    }
    
    /**
     * Get all leave requests
     * @return List of all leave requests
     */
    public List<LeaveRequest> findAll() {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String sql = "SELECT lr.*, e.full_name as employee_name, " +
                     " (SELECT full_name FROM employees WHERE id = lr.approved_by) as approved_by_name " +
                     " FROM " + TABLE_NAME + " lr " +
                     " JOIN employees e ON lr.employee_id = e.id " +
                     " ORDER BY lr.created_at DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                leaveRequests.add(mapResultSetToLeaveRequest(resultSet));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all leave requests: " + e.getMessage());
        }
        
        return leaveRequests;
    }
    
    /**
     * Create new leave request
     * @param leaveRequest Leave request to create
     * @return Created leave request ID, -1 if failed
     */
    public int create(LeaveRequest leaveRequest) {
        String sql = "INSERT INTO " + TABLE_NAME + 
                     " (employee_id, leave_type, start_date, end_date, reason, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setInt(1, leaveRequest.getEmployeeId());
            statement.setString(2, leaveRequest.getLeaveType().name());
            statement.setDate(3, Date.valueOf(leaveRequest.getStartDate()));
            statement.setDate(4, Date.valueOf(leaveRequest.getEndDate()));
            statement.setString(5, leaveRequest.getReason());
            statement.setString(6, leaveRequest.getStatus().name());
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating leave request: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Update leave request status
     * @param leaveRequest Leave request to update
     * @return true if successful
     */
    public boolean updateStatus(LeaveRequest leaveRequest) {
        String sql = "UPDATE " + TABLE_NAME + 
                     " SET status = ?, approved_by = ?, approved_at = NOW(), updated_at = NOW() " +
                     " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, leaveRequest.getStatus().name());
            
            if (leaveRequest.getApprovedBy() != null) {
                statement.setInt(2, leaveRequest.getApprovedBy());
            } else {
                statement.setNull(2, Types.INTEGER);
            }
            
            statement.setInt(3, leaveRequest.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating leave request status: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Update leave request
     * @param leaveRequest Leave request to update
     * @return true if successful
     */
    public boolean update(LeaveRequest leaveRequest) {
        String sql = "UPDATE " + TABLE_NAME + 
                     " SET leave_type = ?, start_date = ?, end_date = ?, reason = ?, updated_at = NOW() " +
                     " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, leaveRequest.getLeaveType().name());
            statement.setDate(2, Date.valueOf(leaveRequest.getStartDate()));
            statement.setDate(3, Date.valueOf(leaveRequest.getEndDate()));
            statement.setString(4, leaveRequest.getReason());
            statement.setInt(5, leaveRequest.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating leave request: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Delete leave request by ID
     * @param id Leave request ID
     * @return true if successful
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting leave request: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if employee has overlapping leave request
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @param excludeId Leave request ID to exclude
     * @return true if overlapping
     */
    public boolean hasOverlappingLeave(int employeeId, LocalDate startDate, LocalDate endDate, Integer excludeId) {
        String sql = "SELECT 1 FROM " + TABLE_NAME + 
                     " WHERE employee_id = ? AND status IN ('PENDING', 'APPROVED') " +
                     " AND ((start_date <= ? AND end_date >= ?) " +
                     " OR (start_date <= ? AND end_date >= ?) " +
                     " OR (start_date >= ? AND end_date <= ?))";
        
        if (excludeId != null) {
            sql += " AND id != ?";
        }
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            statement.setDate(2, Date.valueOf(endDate));
            statement.setDate(3, Date.valueOf(endDate));
            statement.setDate(4, Date.valueOf(startDate));
            statement.setDate(5, Date.valueOf(startDate));
            statement.setDate(6, Date.valueOf(startDate));
            statement.setDate(7, Date.valueOf(endDate));
            
            if (excludeId != null) {
                statement.setInt(8, excludeId);
            }
            
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking overlapping leave: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to LeaveRequest object
     * @param resultSet ResultSet
     * @return LeaveRequest object
     * @throws SQLException if mapping fails
     */
    private LeaveRequest mapResultSetToLeaveRequest(ResultSet resultSet) throws SQLException {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(resultSet.getInt("id"));
        leaveRequest.setEmployeeId(resultSet.getInt("employee_id"));
        leaveRequest.setEmployeeName(resultSet.getString("employee_name"));
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.valueOf(resultSet.getString("leave_type")));
        
        Date startDate = resultSet.getDate("start_date");
        if (startDate != null) {
            leaveRequest.setStartDate(startDate.toLocalDate());
        }
        
        Date endDate = resultSet.getDate("end_date");
        if (endDate != null) {
            leaveRequest.setEndDate(endDate.toLocalDate());
        }
        
        leaveRequest.setReason(resultSet.getString("reason"));
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.valueOf(resultSet.getString("status")));
        
        int approvedBy = resultSet.getInt("approved_by");
        if (!resultSet.wasNull()) {
            leaveRequest.setApprovedBy(approvedBy);
        }
        
        leaveRequest.setApprovedByName(resultSet.getString("approved_by_name"));
        
        Timestamp approvedAt = resultSet.getTimestamp("approved_at");
        if (approvedAt != null) {
            leaveRequest.setApprovedAt(approvedAt.toLocalDateTime());
        }
        
        return leaveRequest;
    }
}

