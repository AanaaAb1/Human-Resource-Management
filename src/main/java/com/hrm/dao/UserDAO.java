package com.hrm.dao;

import com.hrm.config.DatabaseConfig;
import com.hrm.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User operations
 */
public class UserDAO {
    
    private static final String TABLE_NAME = "users";
    
    /**
     * Find user by username
     * @param username Username to search
     * @return Optional containing user if found
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE username = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, username);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find user by ID
     * @param id User ID
     * @return Optional containing user if found
     */
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find user by employee ID
     * @param employeeId Employee ID
     * @return Optional containing user if found
     */
    public Optional<User> findByEmployeeId(int employeeId) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE employee_id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, employeeId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by employee ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all users
     * @return List of all users
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY username";
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                users.add(mapResultSetToUser(resultSet));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all users: " + e.getMessage());
        }
        
        return users;
    }
    
    /**
     * Authenticate user with username and password
     * @param username Username
     * @param password Plain text password
     * @return Optional containing user if authentication successful
     */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (BCrypt.checkpw(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Create new user
     * @param user User to create
     * @return Created user ID, -1 if failed
     */
    public int create(User user) {
        String sql = "INSERT INTO " + TABLE_NAME + " (username, password, role, employee_id) VALUES (?, ?, ?, ?)";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setString(1, user.getUsername());
            statement.setString(2, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            statement.setString(3, user.getRole().name());
            
            if (user.getEmployeeId() != null) {
                statement.setInt(4, user.getEmployeeId());
            } else {
                statement.setNull(4, Types.INTEGER);
            }
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Update user
     * @param user User to update
     * @return true if successful
     */
    public boolean update(User user) {
        String sql = "UPDATE " + TABLE_NAME + " SET username = ?, role = ?, employee_id = ?, updated_at = NOW() WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getRole().name());
            
            if (user.getEmployeeId() != null) {
                statement.setInt(3, user.getEmployeeId());
            } else {
                statement.setNull(3, Types.INTEGER);
            }
            
            statement.setInt(4, user.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Update user password
     * @param userId User ID
     * @param newPassword New hashed password
     * @return true if successful
     */
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE " + TABLE_NAME + " SET password = ?, updated_at = NOW() WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            statement.setInt(2, userId);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Delete user by ID
     * @param id User ID
     * @return true if successful
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if username exists
     * @param username Username to check
     * @return true if exists
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE username = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, username);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to User object
     * @param resultSet ResultSet
     * @return User object
     * @throws SQLException if mapping fails
     */
    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPassword(resultSet.getString("password"));
        user.setRole(User.UserRole.valueOf(resultSet.getString("role")));
        
        int employeeId = resultSet.getInt("employee_id");
        if (!resultSet.wasNull()) {
            user.setEmployeeId(employeeId);
        }
        
        return user;
    }
}

