package com.hrm.dao;

import com.hrm.config.DatabaseConfig;
import com.hrm.model.Payroll;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Payroll operations
 */
public class PayrollDAO {
    
    private static final String TABLE_NAME = "payroll";
    
    /**
     * Find payroll by ID
     * @param id Payroll ID
     * @return Optional containing payroll if found
     */
    public Optional<Payroll> findById(int id) {
        String sql = "SELECT p.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " p JOIN employees e ON p.employee_id = e.id WHERE p.id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToPayroll(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find payroll by employee, month, and year
     * @param employeeId Employee ID
     * @param month Month
     * @param year Year
     * @return Optional containing payroll if found
     */
    public Optional<Payroll> findByEmployeeAndMonthYear(int employeeId, int month, int year) {
        String sql = "SELECT p.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " p JOIN employees e ON p.employee_id = e.id " +
                     " WHERE p.employee_id = ? AND p.month = ? AND p.year = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            statement.setInt(2, month);
            statement.setInt(3, year);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToPayroll(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll by employee and month/year: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find payroll by employee
     * @param employeeId Employee ID
     * @return List of payroll records
     */
    public List<Payroll> findByEmployee(int employeeId) {
        List<Payroll> payrollList = new ArrayList<>();
        String sql = "SELECT p.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " p JOIN employees e ON p.employee_id = e.id " +
                     " WHERE p.employee_id = ? ORDER BY p.year DESC, p.month DESC";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payrollList.add(mapResultSetToPayroll(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll by employee: " + e.getMessage());
        }
        
        return payrollList;
    }
    
    /**
     * Find payroll by year
     * @param year Year
     * @return List of payroll records
     */
    public List<Payroll> findByYear(int year) {
        List<Payroll> payrollList = new ArrayList<>();
        String sql = "SELECT p.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " p JOIN employees e ON p.employee_id = e.id " +
                     " WHERE p.year = ? ORDER BY p.month DESC, e.full_name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, year);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payrollList.add(mapResultSetToPayroll(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll by year: " + e.getMessage());
        }
        
        return payrollList;
    }
    
    /**
     * Find payroll by month and year
     * @param month Month
     * @param year Year
     * @return List of payroll records
     */
    public List<Payroll> findByMonthYear(int month, int year) {
        List<Payroll> payrollList = new ArrayList<>();
        String sql = "SELECT p.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " p JOIN employees e ON p.employee_id = e.id " +
                     " WHERE p.month = ? AND p.year = ? ORDER BY e.full_name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, month);
            statement.setInt(2, year);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payrollList.add(mapResultSetToPayroll(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding payroll by month/year: " + e.getMessage());
        }
        
        return payrollList;
    }
    
    /**
     * Get all payroll records
     * @return List of all payroll records
     */
    public List<Payroll> findAll() {
        List<Payroll> payrollList = new ArrayList<>();
        String sql = "SELECT p.*, e.full_name as employee_name FROM " + TABLE_NAME + 
                     " p JOIN employees e ON p.employee_id = e.id " +
                     " ORDER BY p.year DESC, p.month DESC, e.full_name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                payrollList.add(mapResultSetToPayroll(resultSet));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all payroll: " + e.getMessage());
        }
        
        return payrollList;
    }
    
    /**
     * Generate payroll for employee
     * @param payroll Payroll to create
     * @return Created payroll ID, -1 if failed
     */
    public int create(Payroll payroll) {
        // Check if payroll already exists
        Optional<Payroll> existing = findByEmployeeAndMonthYear(
            payroll.getEmployeeId(), payroll.getMonth(), payroll.getYear()
        );
        
        if (existing.isPresent()) {
            // Update existing payroll
            payroll.setId(existing.get().getId());
            return update(payroll) ? payroll.getId() : -1;
        }
        
        String sql = "INSERT INTO " + TABLE_NAME + 
                     " (employee_id, month, year, basic_salary, allowances, deductions, net_salary) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setInt(1, payroll.getEmployeeId());
            statement.setInt(2, payroll.getMonth());
            statement.setInt(3, payroll.getYear());
            statement.setBigDecimal(4, payroll.getBasicSalary());
            statement.setBigDecimal(5, payroll.getAllowances());
            statement.setBigDecimal(6, payroll.getDeductions());
            statement.setBigDecimal(7, payroll.getNetSalary());
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating payroll: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Update payroll
     * @param payroll Payroll to update
     * @return true if successful
     */
    public boolean update(Payroll payroll) {
        String sql = "UPDATE " + TABLE_NAME + 
                     " SET basic_salary = ?, allowances = ?, deductions = ?, net_salary = ? " +
                     " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setBigDecimal(1, payroll.getBasicSalary());
            statement.setBigDecimal(2, payroll.getAllowances());
            statement.setBigDecimal(3, payroll.getDeductions());
            statement.setBigDecimal(4, payroll.getNetSalary());
            statement.setInt(5, payroll.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating payroll: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Delete payroll by ID
     * @param id Payroll ID
     * @return true if successful
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting payroll: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Calculate total payroll for month/year
     * @param month Month
     * @param year Year
     * @return Total net salary
     */
    public BigDecimal calculateTotalPayroll(int month, int year) {
        String sql = "SELECT COALESCE(SUM(net_salary), 0) as total FROM " + TABLE_NAME + 
                     " WHERE month = ? AND year = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, month);
            statement.setInt(2, year);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating total payroll: " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Map ResultSet to Payroll object
     * @param resultSet ResultSet
     * @return Payroll object
     * @throws SQLException if mapping fails
     */
    private Payroll mapResultSetToPayroll(ResultSet resultSet) throws SQLException {
        Payroll payroll = new Payroll();
        payroll.setId(resultSet.getInt("id"));
        payroll.setEmployeeId(resultSet.getInt("employee_id"));
        payroll.setEmployeeName(resultSet.getString("employee_name"));
        payroll.setMonth(resultSet.getInt("month"));
        payroll.setYear(resultSet.getInt("year"));
        payroll.setBasicSalary(resultSet.getBigDecimal("basic_salary"));
        payroll.setAllowances(resultSet.getBigDecimal("allowances"));
        payroll.setDeductions(resultSet.getBigDecimal("deductions"));
        payroll.setNetSalary(resultSet.getBigDecimal("net_salary"));
        return payroll;
    }
}

