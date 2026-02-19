package com.hrm.dao;

import com.hrm.config.DatabaseConfig;
import com.hrm.model.Employee;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Employee operations
 */
public class EmployeeDAO {
    
    private static final String TABLE_NAME = "employees";
    
    /**
     * Find employee by ID
     * @param id Employee ID
     * @return Optional containing employee if found
     */
    public Optional<Employee> findById(int id) {
        String sql = "SELECT e.*, d.name as department_name FROM " + TABLE_NAME + 
                     " e LEFT JOIN departments d ON e.department_id = d.id WHERE e.id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToEmployee(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding employee by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find employee by employee ID
     * @param employeeId Employee ID string
     * @return Optional containing employee if found
     */
    public Optional<Employee> findByEmployeeId(String employeeId) {
        String sql = "SELECT e.*, d.name as department_name FROM " + TABLE_NAME + 
                     " e LEFT JOIN departments d ON e.department_id = d.id WHERE e.employee_id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, employeeId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToEmployee(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding employee by employee ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find employee by email
     * @param email Email address
     * @return Optional containing employee if found
     */
    public Optional<Employee> findByEmail(String email) {
        String sql = "SELECT e.*, d.name as department_name FROM " + TABLE_NAME + 
                     " e LEFT JOIN departments d ON e.department_id = d.id WHERE e.email = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, email);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToEmployee(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding employee by email: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all employees
     * @return List of all employees
     */
    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT e.*, d.name as department_name FROM " + TABLE_NAME + 
                     " e LEFT JOIN departments d ON e.department_id = d.id ORDER BY e.full_name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                employees.add(mapResultSetToEmployee(resultSet));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all employees: " + e.getMessage());
        }
        
        return employees;
    }
    
    /**
     * Get employees by department
     * @param departmentId Department ID
     * @return List of employees in the department
     */
    public List<Employee> findByDepartment(int departmentId) {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT e.*, d.name as department_name FROM " + TABLE_NAME + 
                     " e LEFT JOIN departments d ON e.department_id = d.id " +
                     " WHERE e.department_id = ? ORDER BY e.full_name";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, departmentId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    employees.add(mapResultSetToEmployee(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding employees by department: " + e.getMessage());
        }
        
        return employees;
    }
    
    /**
     * Create new employee
     * @param employee Employee to create
     * @return Created employee ID, -1 if failed
     */
    public int create(Employee employee) {
        String sql = "INSERT INTO " + TABLE_NAME + 
                     " (employee_id, full_name, gender, email, phone, address, department_id, job_title, hire_date, salary) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setString(1, employee.getEmployeeId());
            statement.setString(2, employee.getFullName());
            statement.setString(3, employee.getGender().name());
            statement.setString(4, employee.getEmail());
            statement.setString(5, employee.getPhone());
            statement.setString(6, employee.getAddress());
            
            if (employee.getDepartmentId() != null) {
                statement.setInt(7, employee.getDepartmentId());
            } else {
                statement.setNull(7, Types.INTEGER);
            }
            
            statement.setString(8, employee.getJobTitle());
            statement.setDate(9, Date.valueOf(employee.getHireDate()));
            statement.setBigDecimal(10, employee.getSalary());
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating employee: " + e.getMessage());
        }
        
        return -1;
    }
    
    /**
     * Update employee
     * @param employee Employee to update
     * @return true if successful
     */
    public boolean update(Employee employee) {
        String sql = "UPDATE " + TABLE_NAME + 
                     " SET full_name = ?, gender = ?, email = ?, phone = ?, address = ?, " +
                     " department_id = ?, job_title = ?, hire_date = ?, salary = ? " +
                     " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getGender().name());
            statement.setString(3, employee.getEmail());
            statement.setString(4, employee.getPhone());
            statement.setString(5, employee.getAddress());
            
            if (employee.getDepartmentId() != null) {
                statement.setInt(6, employee.getDepartmentId());
            } else {
                statement.setNull(6, Types.INTEGER);
            }
            
            statement.setString(7, employee.getJobTitle());
            statement.setDate(8, Date.valueOf(employee.getHireDate()));
            statement.setBigDecimal(9, employee.getSalary());
            statement.setInt(10, employee.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating employee: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Delete employee by ID
     * @param id Employee ID
     * @return true if successful
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting employee: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Count all employees
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
            System.err.println("Error counting employees: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Check if employee ID exists
     * @param employeeId Employee ID to check
     * @return true if exists
     */
    public boolean employeeIdExists(String employeeId) {
        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE employee_id = ?";
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, employeeId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking employee ID existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if email exists
     * @param email Email to check
     * @param excludeId Employee ID to exclude from check
     * @return true if exists
     */
    public boolean emailExists(String email, Integer excludeId) {
        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE email = ?";
        
        if (excludeId != null) {
            sql += " AND id != ?";
        }
        
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, email);
            
            if (excludeId != null) {
                statement.setInt(2, excludeId);
            }
            
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Map ResultSet to Employee object
     * @param resultSet ResultSet
     * @return Employee object
     * @throws SQLException if mapping fails
     */
    private Employee mapResultSetToEmployee(ResultSet resultSet) throws SQLException {
        Employee employee = new Employee();
        employee.setId(resultSet.getInt("id"));
        employee.setEmployeeId(resultSet.getString("employee_id"));
        employee.setFullName(resultSet.getString("full_name"));
        employee.setGender(Employee.Gender.valueOf(resultSet.getString("gender")));
        employee.setEmail(resultSet.getString("email"));
        employee.setPhone(resultSet.getString("phone"));
        employee.setAddress(resultSet.getString("address"));
        
        int departmentId = resultSet.getInt("department_id");
        if (!resultSet.wasNull()) {
            employee.setDepartmentId(departmentId);
        }
        
        employee.setDepartmentName(resultSet.getString("department_name"));
        employee.setJobTitle(resultSet.getString("job_title"));
        
        Date hireDate = resultSet.getDate("hire_date");
        if (hireDate != null) {
            employee.setHireDate(hireDate.toLocalDate());
        }
        
        employee.setSalary(resultSet.getBigDecimal("salary"));
        
        return employee;
    }
}

