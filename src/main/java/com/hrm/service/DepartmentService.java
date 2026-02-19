package com.hrm.service;

import com.hrm.dao.AuditLogDAO;
import com.hrm.dao.DepartmentDAO;
import com.hrm.dao.EmployeeDAO;
import com.hrm.model.Department;

import java.util.List;
import java.util.Optional;

/**
 * Service class for department operations
 */
public class DepartmentService {
    
    private final DepartmentDAO departmentDAO;
    private final EmployeeDAO employeeDAO;
    private final AuditLogDAO auditLogDAO;
    
    public DepartmentService() {
        this.departmentDAO = new DepartmentDAO();
        this.employeeDAO = new EmployeeDAO();
        this.auditLogDAO = new AuditLogDAO();
    }
    
    /**
     * Get department by ID
     * @param id Department ID
     * @return Optional containing department if found
     */
    public Optional<Department> getDepartmentById(int id) {
        return departmentDAO.findById(id);
    }
    
    /**
     * Get department by name
     * @param name Department name
     * @return Optional containing department if found
     */
    public Optional<Department> getDepartmentByName(String name) {
        return departmentDAO.findByName(name);
    }
    
    /**
     * Get all departments
     * @return List of all departments
     */
    public List<Department> getAllDepartments() {
        return departmentDAO.findAll();
    }
    
    /**
     * Create new department
     * @param department Department to create
     * @param createdByUserId User ID creating the department
     * @param ipAddress Client IP address
     * @return Created department ID, -1 if failed
     */
    public int createDepartment(Department department, Integer createdByUserId, String ipAddress) {
        // Check if department name already exists
        if (departmentDAO.nameExists(department.getName(), null)) {
            return -1;
        }
        
        int departmentId = departmentDAO.create(department);
        
        if (departmentId > 0) {
            // Log the creation
            auditLogDAO.logDataModification(
                createdByUserId, "CREATE", "Department", departmentId,
                "Created department: " + department.getName(), ipAddress
            );
        }
        
        return departmentId;
    }
    
    /**
     * Update department
     * @param department Department to update
     * @param updatedByUserId User ID updating the department
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean updateDepartment(Department department, Integer updatedByUserId, String ipAddress) {
        // Check if name already exists for another department
        if (departmentDAO.nameExists(department.getName(), department.getId())) {
            return false;
        }
        
        boolean updated = departmentDAO.update(department);
        
        if (updated) {
            // Log the update
            auditLogDAO.logDataModification(
                updatedByUserId, "UPDATE", "Department", department.getId(),
                "Updated department: " + department.getName(), ipAddress
            );
        }
        
        return updated;
    }
    
    /**
     * Delete department
     * @param id Department ID to delete
     * @param deletedByUserId User ID deleting the department
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean deleteDepartment(int id, Integer deletedByUserId, String ipAddress) {
        // Check if department has employees
        List<Employee> employees = employeeDAO.findByDepartment(id);
        if (!employees.isEmpty()) {
            return false; // Cannot delete department with employees
        }
        
        Optional<Department> departmentOpt = departmentDAO.findById(id);
        
        if (departmentOpt.isEmpty()) {
            return false;
        }
        
        Department department = departmentOpt.get();
        boolean deleted = departmentDAO.delete(id);
        
        if (deleted) {
            // Log the deletion
            auditLogDAO.logDataModification(
                deletedByUserId, "DELETE", "Department", id,
                "Deleted department: " + department.getName(), ipAddress
            );
        }
        
        return deleted;
    }
    
    /**
     * Get department count
     * @return Total number of departments
     */
    public int getDepartmentCount() {
        return departmentDAO.count();
    }
    
    /**
     * Check if department exists
     * @param id Department ID
     * @return true if exists
     */
    public boolean departmentExists(int id) {
        return departmentDAO.findById(id).isPresent();
    }
    
    /**
     * Check if department has employees
     * @param departmentId Department ID
     * @return true if department has employees
     */
    public boolean hasEmployees(int departmentId) {
        return !employeeDAO.findByDepartment(departmentId).isEmpty();
    }
}

