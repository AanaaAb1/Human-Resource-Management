package com.hrm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payroll model class representing employee payroll records
 */
public class Payroll {
    private int id;
    private int employeeId;
    private String employeeName; // For display purposes
    private int month;
    private int year;
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private BigDecimal netSalary;
    private LocalDateTime generatedAt;
    
    // Constructors
    public Payroll() {
        this.allowances = BigDecimal.ZERO;
        this.deductions = BigDecimal.ZERO;
    }
    
    public Payroll(int id, int employeeId, int month, int year, BigDecimal basicSalary,
                   BigDecimal allowances, BigDecimal deductions, BigDecimal netSalary) {
        this.id = id;
        this.employeeId = employeeId;
        this.month = month;
        this.year = year;
        this.basicSalary = basicSalary;
        this.allowances = allowances != null ? allowances : BigDecimal.ZERO;
        this.deductions = deductions != null ? deductions : BigDecimal.ZERO;
        this.netSalary = netSalary;
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
    
    public int getMonth() {
        return month;
    }
    
    public void setMonth(int month) {
        this.month = month;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public BigDecimal getBasicSalary() {
        return basicSalary;
    }
    
    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
    }
    
    public BigDecimal getAllowances() {
        return allowances;
    }
    
    public void setAllowances(BigDecimal allowances) {
        this.allowances = allowances;
    }
    
    public BigDecimal getDeductions() {
        return deductions;
    }
    
    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }
    
    public BigDecimal getNetSalary() {
        return netSalary;
    }
    
    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public BigDecimal getGrossSalary() {
        if (basicSalary != null && allowances != null) {
            return basicSalary.add(allowances);
        }
        return basicSalary != null ? basicSalary : BigDecimal.ZERO;
    }
    
    public String getMonthYear() {
        return String.format("%02d/%d", month, year);
    }
    
    @Override
    public String toString() {
        return "Payroll{" +
                "id=" + id +
                ", employeeId=" + employeeId +
                ", month=" + month +
                ", year=" + year +
                ", netSalary=" + netSalary +
                '}';
    }
}

