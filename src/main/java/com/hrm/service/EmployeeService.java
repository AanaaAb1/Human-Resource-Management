package com.hrm.service;

import com.hrm.dao.AuditLogDAO;
import com.hrm.dao.EmployeeDAO;
import com.hrm.model.Employee;

import java.util.List;
import java.util.Optional;

/**
 * Service class for employee operations
 */
public class EmployeeService {
    
    private final EmployeeDAO employeeDAO;
    private final AuditLogDAO auditLogDAO;
    
    public EmployeeService() {
        this.employeeDAO = new EmployeeDAO();
        this.auditLogDAO = new AuditLogDAO();
    }
    
    /**
     * Get employee by ID
     * @param id Employee ID
     * @return Optional containing employee if found
     */
    public Optional<Employee> getEmployeeById(int id) {
        return employeeDAO.findById(id);
    }
    
    /**
     * Get employee by employee ID
     * @param employeeId Employee ID string
     * @return Optional containing employee if found
     */
    public Optional<Employee> getEmployeeByEmployeeId(String employeeId) {
        return employeeDAO.findByEmployeeId(employeeId);
    }
    
    /**
     * Get all employees
     * @return List of all employees
     */
    public List<Employee> getAllEmployees() {
        return employeeDAO.findAll();
    }
    
    /**
     * Get employees by department
     * @param departmentId Department ID
     * @return List of employees in the department
     */
    public List<Employee> getEmployeesByDepartment(int departmentId) {
        return employeeDAO.findByDepartment(departmentId);
    }
    
    /**
     * Create new employee
     * @param employee Employee to create
     * @param createdByUserId User ID creating the employee
     * @param ipAddress Client IP address
     * @return Created employee ID, -1 if failed
     */
    public int createEmployee(Employee employee, Integer createdByUserId, String ipAddress) {
        // Validate employee ID format
        if (!com.hrm.util.ValidationUtil.isValidEmployeeId(employee.getEmployeeId())) {
            return -1;
        }
        
        // Check if employee ID already exists
        if (employeeDAO.employeeIdExists(employee.getEmployeeId())) {
            return -1;
        }
        
        // Check if email already exists
        if (employeeDAO.emailExists(employee.getEmail(), null)) {
            return -1;
        }
        
        int employeeId = employeeDAO.create(employee);
        
        if (employeeId > 0) {
            // Log the creation
            auditLogDAO.logDataModification(
                createdByUserId, "CREATE", "Employee", employeeId,
                "Created employee: " + employee.getFullName(), ipAddress
            );
        }
        
        return employeeId;
    }
    
    /**
     * Update employee
     * @param employee Employee to update
     * @param updatedByUserId User ID updating the employee
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean updateEmployee(Employee employee, Integer updatedByUserId, String ipAddress) {
        // Check if email already exists for another employee
        if (employeeDAO.emailExists(employee.getEmail(), employee.getId())) {
            return false;
        }
        
        boolean updated = employeeDAO.update(employee);
        
        if (updated) {
            // Log the update
            auditLogDAO.logDataModification(
                updatedByUserId, "UPDATE", "Employee", employee.getId(),
                "Updated employee: " + employee.getFullName(), ipAddress
            );
        }
        
        return updated;
    }
    
    /**
     * Delete employee
     * @param id Employee ID to delete
     * @param deletedByUserId User ID deleting the employee
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean deleteEmployee(int id, Integer deletedByUserId, String ipAddress) {
        Optional<Employee> employeeOpt = employeeDAO.findById(id);
        
        if (employeeOpt.isEmpty()) {
            return false;
        }
        
        Employee employee = employeeOpt.get();
        boolean deleted = employeeDAO.delete(id);
        
        if (deleted) {
            // Log the deletion
            auditLogDAO.logDataModification(
                deletedByUserId, "DELETE", "Employee", id,
                "Deleted employee: " + employee.getFullName(), ipAddress
            );
        }
        
        return deleted;
    }
    
    /**
     * Get employee count
     * @return Total number of employees
     */
    public int getEmployeeCount() {
        return employeeDAO.count();
    }
    
    /**
     * Check if employee exists
     * @param id Employee ID
     * @return true if exists
     */
    public boolean employeeExists(int id) {
        return employeeDAO.findById(id).isPresent();
    }
}

