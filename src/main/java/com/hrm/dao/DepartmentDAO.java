package com.hrm.dao;

import com.hrm.config.DatabaseConfig;
import com.hrm.model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Department operations
 */
public class DepartmentDAO {
    
    private static final String TABLE_NAME = "departments";
    
    /**
     * Find department by ID
     * @param id Department ID
     * @return Optional containing department if found
     */
    public Optional<Department> findById(int id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToDepartment(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding department by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find department by name
     * @param name Department name
     * @return Optional containing department if found
     */
    public Optional<Department> findByName(String name) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE name = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, name);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToDepartment(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding department by name: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all departments
     * @return List of all departments
     */
    public List<Department> findAll() {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                departments.add(mapResultSetToDepartment(resultSet));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all departments: " + e.getMessage());
        }
        
        return departments;
    }
    
    /**
     * Create new department
     * @param department Department to create
     * @return Created department ID, -1 if failed
     */
    public int create(Department department) {
        String sql = "INSERT INTO " + TABLE_NAME + " (name, description) VALUES (?, ?)";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setString(1, department.getName());
            statement.setString(2, department.getDescription());
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating department: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Update department
     * @param department Department to update
     * @return true if successful
     */
    public boolean update(Department department) {
        String sql = "UPDATE " + TABLE_NAME + " SET name = ?, description = ? WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, department.getName());
            statement.setString(2, department.getDescription());
            statement.setInt(3, department.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating department: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Delete department by ID
     * @param id Department ID
     * @return true if successful
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting department: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if department name exists
     * @param name Department name to check
     * @param excludeId Department ID to exclude from check
     * @return true if exists
     */
    public boolean nameExists(String name, Integer excludeId) {
        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE name = ?";
        
        if (excludeId != null) {
            sql += " AND id != ?";
        }
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, name);
            
            if (excludeId != null) {
                statement.setInt(2, excludeId);
            }
            
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking department name existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Count departments
     * @return Total count
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting departments: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Map ResultSet to Department object
     * @param resultSet ResultSet
     * @return Department object
     * @throws SQLException if mapping fails
     */
    private Department mapResultSetToDepartment(ResultSet resultSet) throws SQLException {
        Department department = new Department();
        department.setId(resultSet.getInt("id"));
        department.setName(resultSet.getString("name"));
        department.setDescription(resultSet.getString("description"));
        return department;
    }
}

