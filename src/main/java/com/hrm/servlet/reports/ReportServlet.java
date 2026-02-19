package com.hrm.servlet.reports;

import com.hrm.model.Attendance;
import com.hrm.model.Employee;
import com.hrm.model.LeaveRequest;
import com.hrm.model.Payroll;
import com.hrm.model.User;
import com.hrm.service.ReportService;
import com.hrm.util.JSONUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Servlet for generating reports
 */
public class ReportServlet extends HttpServlet {
    
    private final ReportService reportService;
    private final DateTimeFormatter dateFormatter;
    
    public ReportServlet() {
        this.reportService = new ReportService();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            
            // GET /api/reports - Get summary
            if (pathInfo == null || pathInfo.equals("/")) {
                Map<String, Object> summary = reportService.getSummaryStatistics();
                JSONUtil.sendSuccess(response, "Summary statistics retrieved", summary);
                return;
            }
            
            // GET /api/reports/employees - Employee report
            if (pathInfo.equals("/employees")) {
                List<Map<String, Object>> employeeReport = reportService.generateEmployeeReport();
                JSONUtil.sendSuccess(response, "Employee report generated", employeeReport);
                return;
            }
            
            // GET /api/reports/attendance - Attendance report
            if (pathInfo.equals("/attendance")) {
                String dateParam = request.getParameter("date");
                String startDateParam = request.getParameter("startDate");
                String endDateParam = request.getParameter("endDate");
                String employeeIdParam = request.getParameter("employeeId");
                
                if (dateParam != null && !dateParam.isEmpty()) {
                    // Report by specific date
                    LocalDate date = LocalDate.parse(dateParam, dateFormatter);
                    Map<String, Object> attendanceReport = reportService.generateAttendanceReportByDate(date);
                    JSONUtil.sendSuccess(response, "Attendance report generated", attendanceReport);
                } else if (employeeIdParam != null && startDateParam != null && endDateParam != null) {
                    // Report by employee and date range
                    try {
                        int employeeId = Integer.parseInt(employeeIdParam);
                        LocalDate startDate = LocalDate.parse(startDateParam, dateFormatter);
                        LocalDate endDate = LocalDate.parse(endDateParam, dateFormatter);
                        
                        Map<String, Object> attendanceReport = reportService.generateAttendanceReport(
                            employeeId, startDate, endDate);
                        JSONUtil.sendSuccess(response, "Attendance report generated", attendanceReport);
                    } catch (Exception e) {
                        JSONUtil.sendBadRequest(response, "Invalid parameters");
                    }
                } else {
                    JSONUtil.sendBadRequest(response, "Please provide date or employeeId with date range");
                }
                return;
            }
            
            // GET /api/reports/leave - Leave report
            if (pathInfo.equals("/leave")) {
                String statusParam = request.getParameter("status");
                
                LeaveRequest.LeaveStatus status = null;
                if (statusParam != null && !statusParam.isEmpty()) {
                    try {
                        status = LeaveRequest.LeaveStatus.valueOf(statusParam.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        JSONUtil.sendBadRequest(response, "Invalid status");
                        return;
                    }
                }
                
                Map<String, Object> leaveReport = reportService.generateLeaveReport(status);
                JSONUtil.sendSuccess(response, "Leave report generated", leaveReport);
                return;
            }
            
            // GET /api/reports/payroll - Payroll report
            if (pathInfo.equals("/payroll")) {
                String monthParam = request.getParameter("month");
                String yearParam = request.getParameter("year");
                
                if (monthParam != null && yearParam != null) {
                    try {
                        int month = Integer.parseInt(monthParam);
                        int year = Integer.parseInt(yearParam);
                        
                        if (month < 1 || month > 12) {
                            JSONUtil.sendBadRequest(response, "Month must be between 1 and 12");
                            return;
                        }
                        
                        Map<String, Object> payrollReport = reportService.generatePayrollReport(month, year);
                        JSONUtil.sendSuccess(response, "Payroll report generated", payrollReport);
                    } catch (NumberFormatException e) {
                        JSONUtil.sendBadRequest(response, "Invalid month or year");
                    }
                } else {
                    JSONUtil.sendBadRequest(response, "Month and year are required");
                }
                return;
            }
            
            // GET /api/reports/summary - Summary statistics
            if (pathInfo.equals("/summary")) {
                Map<String, Object> summary = reportService.getSummaryStatistics();
                JSONUtil.sendSuccess(response, "Summary statistics retrieved", summary);
                return;
            }
            
            // GET /api/reports/audit - Audit log report
            if (pathInfo.equals("/audit")) {
                // Only admins can view audit logs
                if (!currentUser.isAdmin()) {
                    JSONUtil.sendForbidden(response, "Only administrators can view audit logs");
                    return;
                }
                
                List<Map<String, Object>> employeeReport = reportService.generateEmployeeReport();
                JSONUtil.sendSuccess(response, "Audit report available", employeeReport);
                return;
            }
            
            // GET /api/reports/departments - Department report
            if (pathInfo.equals("/departments")) {
                List<Map<String, Object>> departmentReport = reportService.generateDepartmentReport();
                JSONUtil.sendSuccess(response, "Department report generated", departmentReport);
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid report type");
            
        } catch (Exception e) {
            System.err.println("Report GET error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while generating report");
        }
    }
    
    /**
     * Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

