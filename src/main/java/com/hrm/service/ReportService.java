package com.hrm.service;

import com.hrm.dao.AttendanceDAO;
import com.hrm.dao.EmployeeDAO;
import com.hrm.dao.LeaveDAO;
import com.hrm.dao.PayrollDAO;
import com.hrm.model.Attendance;
import com.hrm.model.Employee;
import com.hrm.model.LeaveRequest;
import com.hrm.model.Payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for generating reports
 */
public class ReportService {
    
    private final EmployeeDAO employeeDAO;
    private final AttendanceDAO attendanceDAO;
    private final LeaveDAO leaveDAO;
    private final PayrollDAO payrollDAO;
    
    public ReportService() {
        this.employeeDAO = new EmployeeDAO();
        this.attendanceDAO = new AttendanceDAO();
        this.leaveDAO = new LeaveDAO();
        this.payrollDAO = new PayrollDAO();
    }
    
    /**
     * Generate employee report
     * @return List of employee summaries
     */
    public List<Map<String, Object>> generateEmployeeReport() {
        List<Employee> employees = employeeDAO.findAll();
        List<Map<String, Object>> report = new ArrayList<>();
        
        for (Employee employee : employees) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", employee.getId());
            summary.put("employeeId", employee.getEmployeeId());
            summary.put("fullName", employee.getFullName());
            summary.put("email", employee.getEmail());
            summary.put("department", employee.getDepartmentName());
            summary.put("jobTitle", employee.getJobTitle());
            summary.put("hireDate", employee.getHireDate());
            summary.put("salary", employee.getSalary());
            report.add(summary);
        }
        
        return report;
    }
    
    /**
     * Generate attendance report for employee
     * @param employeeId Employee ID
     * @param startDate Start date
     * @param endDate End date
     * @return Attendance summary
     */
    public Map<String, Object> generateAttendanceReport(int employeeId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendanceList = attendanceDAO.findByEmployeeAndDateRange(employeeId, startDate, endDate);
        
        Map<String, Object> report = new HashMap<>();
        
        // Count by status
        long present = attendanceList.stream()
            .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
            .count();
        long absent = attendanceList.stream()
            .filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT)
            .count();
        long leave = attendanceList.stream()
            .filter(a -> a.getStatus() == Attendance.AttendanceStatus.LEAVE)
            .count();
        
        report.put("employeeId", employeeId);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalDays", attendanceList.size());
        report.put("present", present);
        report.put("absent", absent);
        report.put("onLeave", leave);
        report.put("attendanceRate", attendanceList.isEmpty() ? 0 : 
            (double) present / attendanceList.size() * 100);
        report.put("details", attendanceList);
        
        return report;
    }
    
    /**
     * Generate attendance report for all employees on a specific date
     * @param date Attendance date
     * @return Attendance summary
     */
    public Map<String, Object> generateAttendanceReportByDate(LocalDate date) {
        List<Attendance> attendanceList = attendanceDAO.findByDate(date);
        
        Map<String, Object> report = new HashMap<>();
        
        long present = attendanceList.stream()
            .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
            .count();
        long absent = attendanceList.stream()
            .filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT)
            .count();
        long leave = attendanceList.stream()
            .filter(a -> a.getStatus() == Attendance.AttendanceStatus.LEAVE)
            .count();
        
        report.put("date", date);
        report.put("totalEmployees", attendanceList.size());
        report.put("present", present);
        report.put("absent", absent);
        report.put("onLeave", leave);
        report.put("attendanceRate", attendanceList.isEmpty() ? 0 : 
            (double) present / attendanceList.size() * 100);
        report.put("details", attendanceList);
        
        return report;
    }
    
    /**
     * Generate leave report
     * @param status Leave status filter (null for all)
     * @return Leave summary
     */
    public Map<String, Object> generateLeaveReport(LeaveRequest.LeaveStatus status) {
        List<LeaveRequest> leaveList;
        
        if (status != null) {
            leaveList = leaveDAO.findByStatus(status);
        } else {
            leaveList = leaveDAO.findAll();
        }
        
        Map<String, Object> report = new HashMap<>();
        
        // Group by leave type
        Map<String, Long> byType = leaveList.stream()
            .collect(Collectors.groupingBy(
                lr -> lr.getLeaveType().name(),
                Collectors.counting()
            ));
        
        // Group by status
        Map<String, Long> byStatus = leaveList.stream()
            .collect(Collectors.groupingBy(
                lr -> lr.getStatus().name(),
                Collectors.counting()
            ));
        
        // Calculate total days
        long totalDays = leaveList.stream()
            .mapToLong(LeaveRequest::getNumberOfDays)
            .sum();
        
        report.put("totalRequests", leaveList.size());
        report.put("byType", byType);
        report.put("byStatus", byStatus);
        report.put("totalLeaveDays", totalDays);
        report.put("details", leaveList);
        
        return report;
    }
    
    /**
     * Generate payroll report for a month
     * @param month Month
     * @param year Year
     * @return Payroll summary
     */
    public Map<String, Object> generatePayrollReport(int month, int year) {
        List<Payroll> payrollList = payrollDAO.findByMonthYear(month, year);
        
        Map<String, Object> report = new HashMap<>();
        
        BigDecimal totalBasicSalary = payrollList.stream()
            .map(Payroll::getBasicSalary)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAllowances = payrollList.stream()
            .map(Payroll::getAllowances)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDeductions = payrollList.stream()
            .map(Payroll::getDeductions)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalNetSalary = payrollList.stream()
            .map(Payroll::getNetSalary)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.put("month", month);
        report.put("year", year);
        report.put("totalEmployees", payrollList.size());
        report.put("totalBasicSalary", totalBasicSalary);
        report.put("totalAllowances", totalAllowances);
        report.put("totalDeductions", totalDeductions);
        report.put("totalNetSalary", totalNetSalary);
        report.put("averageSalary", payrollList.isEmpty() ? BigDecimal.ZERO : 
            totalNetSalary.divide(BigDecimal.valueOf(payrollList.size()), 2, java.math.RoundingMode.HALF_UP));
        report.put("details", payrollList);
        
        return report;
    }
    
    /**
     * Generate employee payroll history
     * @param employeeId Employee ID
     * @return List of payroll records
     */
    public List<Payroll> getEmployeePayrollHistory(int employeeId) {
        return payrollDAO.findByEmployee(employeeId);
    }
    
    /**
     * Generate department-wise employee count
     * @return Department summary
     */
    public List<Map<String, Object>> generateDepartmentReport() {
        List<Employee> employees = employeeDAO.findAll();
        
        Map<Integer, List<Employee>> byDepartment = employees.stream()
            .filter(e -> e.getDepartmentId() != null)
            .collect(Collectors.groupingBy(Employee::getDepartmentId));
        
        List<Map<String, Object>> report = new ArrayList<>();
        
        for (Map.Entry<Integer, List<Employee>> entry : byDepartment.entrySet()) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("departmentId", entry.getKey());
            summary.put("departmentName", entry.getValue().get(0).getDepartmentName());
            summary.put("employeeCount", entry.getValue().size());
            summary.put("employees", entry.getValue());
            report.add(summary);
        }
        
        return report;
    }
    
    /**
     * Convert report to CSV format
     * @param report Report data
     * @param headers Column headers
     * @return CSV string
     */
    public String toCSV(List<Map<String, Object>> report, String[] headers) {
        StringBuilder csv = new StringBuilder();
        
        // Add headers
        csv.append(String.join(",", headers)).append("\n");
        
        // Add data rows
        for (Map<String, Object> row : report) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object value = row.get(header);
                if (value == null) {
                    values.add("");
                } else if (value instanceof String && ((String) value).contains(",")) {
                    values.add("\"" + value + "\"");
                } else {
                    values.add(value.toString());
                }
            }
            csv.append(String.join(",", values)).append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Generate summary statistics
     * @return Summary statistics
     */
    public Map<String, Object> getSummaryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEmployees", employeeDAO.count());
        stats.put("totalDepartments", new DepartmentService().getDepartmentCount());
        
        // Current month payroll
        LocalDate now = LocalDate.now();
        BigDecimal currentMonthPayroll = payrollDAO.calculateTotalPayroll(now.getMonthValue(), now.getYear());
        stats.put("currentMonthPayroll", currentMonthPayroll);
        
        // Pending leave requests
        stats.put("pendingLeaveRequests", leaveDAO.findPending().size());
        
        // Report generated timestamp
        stats.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return stats;
    }
}

