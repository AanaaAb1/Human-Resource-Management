package com.hrm.service;

import com.hrm.dao.AuditLogDAO;
import com.hrm.dao.EmployeeDAO;
import com.hrm.dao.PayrollDAO;
import com.hrm.model.Employee;
import com.hrm.model.Payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service class for payroll operations
 */
public class PayrollService {
    
    private final PayrollDAO payrollDAO;
    private final EmployeeDAO employeeDAO;
    private final AuditLogDAO auditLogDAO;
    
    // Default salary components (can be customized)
    private static final BigDecimal DEFAULT_ALLOWANCE_RATE = new BigDecimal("0.10"); // 10% of basic salary
    private static final BigDecimal DEFAULT_DEDUCTION_RATE = new BigDecimal("0.05"); // 5% of basic salary
    
    public PayrollService() {
        this.payrollDAO = new PayrollDAO();
        this.employeeDAO = new EmployeeDAO();
        this.auditLogDAO = new AuditLogDAO();
    }
    
    /**
     * Get payroll by ID
     * @param id Payroll ID
     * @return Optional containing payroll if found
     */
    public Optional<Payroll> getPayrollById(int id) {
        return payrollDAO.findById(id);
    }
    
    /**
     * Get payroll by employee, month, and year
     * @param employeeId Employee ID
     * @param month Month
     * @param year Year
     * @return Optional containing payroll if found
     */
    public Optional<Payroll> getPayrollByEmployeeAndMonthYear(int employeeId, int month, int year) {
        return payrollDAO.findByEmployeeAndMonthYear(employeeId, month, year);
    }
    
    /**
     * Get payroll for employee
     * @param employeeId Employee ID
     * @return List of payroll records
     */
    public List<Payroll> getPayrollByEmployee(int employeeId) {
        return payrollDAO.findByEmployee(employeeId);
    }
    
    /**
     * Get payroll by year
     * @param year Year
     * @return List of payroll records
     */
    public List<Payroll> getPayrollByYear(int year) {
        return payrollDAO.findByYear(year);
    }
    
    /**
     * Get payroll by month and year
     * @param month Month
     * @param year Year
     * @return List of payroll records
     */
    public List<Payroll> getPayrollByMonthYear(int month, int year) {
        return payrollDAO.findByMonthYear(month, year);
    }
    
    /**
     * Get all payroll records
     * @return List of all payroll records
     */
    public List<Payroll> getAllPayroll() {
        return payrollDAO.findAll();
    }
    
    /**
     * Generate payroll for a single employee
     * @param employeeId Employee ID
     * @param month Month
     * @param year Year
     * @param allowances Custom allowances (optional)
     * @param deductions Custom deductions (optional)
     * @param generatedByUserId User ID generating the payroll
     * @param ipAddress Client IP address
     * @return Generated payroll ID, -1 if failed
     */
    public int generatePayroll(int employeeId, int month, int year, 
                               BigDecimal allowances, BigDecimal deductions,
                               Integer generatedByUserId, String ipAddress) {
        // Get employee details
        Optional<Employee> employeeOpt = employeeDAO.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            return -1;
        }
        
        Employee employee = employeeOpt.get();
        
        // Calculate salary components
        BigDecimal basicSalary = employee.getSalary();
        BigDecimal allowancesAmount = allowances != null ? allowances : 
            basicSalary.multiply(DEFAULT_ALLOWANCE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deductionsAmount = deductions != null ? deductions : 
            basicSalary.multiply(DEFAULT_DEDUCTION_RATE).setScale(2, RoundingMode.HALF_UP);
        
        // Calculate net salary
        BigDecimal netSalary = basicSalary.add(allowancesAmount).subtract(deductionsAmount);
        
        // Create payroll record
        Payroll payroll = new Payroll();
        payroll.setEmployeeId(employeeId);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setBasicSalary(basicSalary);
        payroll.setAllowances(allowancesAmount);
        payroll.setDeductions(deductionsAmount);
        payroll.setNetSalary(netSalary);
        
        int payrollId = payrollDAO.create(payroll);
        
        if (payrollId > 0) {
            // Log the generation
            auditLogDAO.logDataModification(
                generatedByUserId, "GENERATE", "Payroll", payrollId,
                "Generated payroll for employee: " + employee.getFullName() + 
                ", Month: " + month + "/" + year + 
                ", Net Salary: " + netSalary, ipAddress
            );
        }
        
        return payrollId;
    }
    
    /**
     * Generate payroll for all employees
     * @param month Month
     * @param year Year
     * @param generatedByUserId User ID generating the payroll
     * @param ipAddress Client IP address
     * @return Number of payroll records generated
     */
    public int generatePayrollForAll(int month, int year, Integer generatedByUserId, String ipAddress) {
        List<Employee> employees = employeeDAO.findAll();
        int generatedCount = 0;
        
        for (Employee employee : employees) {
            int payrollId = generatePayroll(
                employee.getId(), month, year, 
                null, null, generatedByUserId, ipAddress
            );
            if (payrollId > 0) {
                generatedCount++;
            }
        }
        
        return generatedCount;
    }
    
    /**
     * Update payroll
     * @param payroll Payroll to update
     * @param updatedByUserId User ID updating the payroll
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean updatePayroll(Payroll payroll, Integer updatedByUserId, String ipAddress) {
        // Recalculate net salary
        BigDecimal netSalary = payroll.getBasicSalary()
            .add(payroll.getAllowances())
            .subtract(payroll.getDeductions());
        payroll.setNetSalary(netSalary);
        
        boolean updated = payrollDAO.update(payroll);
        
        if (updated) {
            // Log the update
            auditLogDAO.logDataModification(
                updatedByUserId, "UPDATE", "Payroll", payroll.getId(),
                "Updated payroll for employee: " + payroll.getEmployeeId() + 
                ", Net Salary: " + netSalary, ipAddress
            );
        }
        
        return updated;
    }
    
    /**
     * Delete payroll
     * @param id Payroll ID to delete
     * @param deletedByUserId User ID deleting the payroll
     * @param ipAddress Client IP address
     * @return true if successful
     */
    public boolean deletePayroll(int id, Integer deletedByUserId, String ipAddress) {
        Optional<Payroll> payrollOpt = payrollDAO.findById(id);
        
        if (payrollOpt.isEmpty()) {
            return false;
        }
        
        Payroll payroll = payrollOpt.get();
        boolean deleted = payrollDAO.delete(id);
        
        if (deleted) {
            // Log the deletion
            auditLogDAO.logDataModification(
                deletedByUserId, "DELETE", "Payroll", id,
                "Deleted payroll for employee: " + payroll.getEmployeeId() + 
                ", Month: " + payroll.getMonth() + "/" + payroll.getYear(), ipAddress
            );
        }
        
        return deleted;
    }
    
    /**
     * Calculate total payroll for a month
     * @param month Month
     * @param year Year
     * @return Total net salary
     */
    public BigDecimal calculateTotalPayroll(int month, int year) {
        return payrollDAO.calculateTotalPayroll(month, year);
    }
    
    /**
     * Check if payroll exists for employee and month/year
     * @param employeeId Employee ID
     * @param month Month
     * @param year Year
     * @return true if payroll exists
     */
    public boolean payrollExists(int employeeId, int month, int year) {
        return payrollDAO.findByEmployeeAndMonthYear(employeeId, month, year).isPresent();
    }
}

