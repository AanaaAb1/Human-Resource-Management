package com.hrm.servlet.attendance;

import com.hrm.model.Attendance;
import com.hrm.model.User;
import com.hrm.service.AttendanceService;
import com.hrm.util.JSONUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Servlet for viewing attendance records
 */
public class AttendanceViewServlet extends HttpServlet {
    
    private final AttendanceService attendanceService;
    private final DateTimeFormatter dateFormatter;
    
    public AttendanceViewServlet() {
        this.attendanceService = new AttendanceService();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            
            // GET /api/attendance - Get attendance records
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Attendance> attendanceList;
                
                // Employees can only see their own attendance
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (currentUser.getEmployeeId() == null) {
                        JSONUtil.sendSuccess(response, "No employee profile", List.of());
                        return;
                    }
                    
                    // Check for date range parameters
                    String startDateParam = request.getParameter("startDate");
                    String endDateParam = request.getParameter("endDate");
                    
                    if (startDateParam != null && endDateParam != null) {
                        LocalDate startDate = LocalDate.parse(startDateParam, dateFormatter);
                        LocalDate endDate = LocalDate.parse(endDateParam, dateFormatter);
                        attendanceList = attendanceService.getAttendanceByEmployeeAndDateRange(
                            currentUser.getEmployeeId(), startDate, endDate);
                    } else {
                        attendanceList = attendanceService.getAttendanceByEmployee(currentUser.getEmployeeId());
                    }
                } else {
                    // Check for filters
                    String employeeIdParam = request.getParameter("employeeId");
                    String dateParam = request.getParameter("date");
                    
                    if (dateParam != null) {
                        LocalDate date = LocalDate.parse(dateParam, dateFormatter);
                        attendanceList = attendanceService.getAttendanceByDate(date);
                    } else if (employeeIdParam != null) {
                        try {
                            int employeeId = Integer.parseInt(employeeIdParam);
                            attendanceList = attendanceService.getAttendanceByEmployee(employeeId);
                        } catch (NumberFormatException e) {
                            JSONUtil.sendBadRequest(response, "Invalid employee ID");
                            return;
                        }
                    } else {
                        attendanceList = attendanceService.getAllAttendance();
                    }
                }
                
                JSONUtil.sendSuccess(response, "Attendance retrieved", attendanceList);
                return;
            }
            
            // GET /api/attendance/{id} - Get specific attendance record
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length >= 2) {
                int attendanceId;
                try {
                    attendanceId = Integer.parseInt(pathParts[1]);
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid attendance ID");
                    return;
                }
                
                Optional<Attendance> attendanceOpt = attendanceService.getAttendanceById(attendanceId);
                
                if (attendanceOpt.isEmpty()) {
                    JSONUtil.sendNotFound(response, "Attendance record not found");
                    return;
                }
                
                Attendance attendance = attendanceOpt.get();
                
                // Check access
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (attendance.getEmployeeId() != currentUser.getEmployeeId()) {
                        JSONUtil.sendForbidden(response, "You can only view your own attendance records");
                        return;
                    }
                }
                
                JSONUtil.sendSuccess(response, "Attendance retrieved", attendance);
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid request");
            
        } catch (Exception e) {
            System.err.println("Attendance view GET error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred");
        }
    }
}

