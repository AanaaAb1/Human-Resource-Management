package com.hrm.servlet.attendance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hrm.model.Attendance;
import com.hrm.model.User;
import com.hrm.service.AttendanceService;
import com.hrm.util.JSONUtil;
import com.hrm.util.ValidationUtil;

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
 * Servlet for attendance operations
 */
public class AttendanceServlet extends HttpServlet {
    
    private final AttendanceService attendanceService;
    private final DateTimeFormatter dateFormatter;
    
    public AttendanceServlet() {
        this.attendanceService = new AttendanceService();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            
            // GET /api/attendance - Get all attendance (filtered by user role)
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Attendance> attendanceList;
                
                // Employees can only see their own attendance
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (currentUser.getEmployeeId() == null) {
                        JSONUtil.sendSuccess(response, "No employee profile", List.of());
                        return;
                    }
                    attendanceList = attendanceService.getAttendanceByEmployee(currentUser.getEmployeeId());
                } else {
                    // Check for date filter
                    String dateParam = request.getParameter("date");
                    if (dateParam != null && !dateParam.isEmpty()) {
                        LocalDate date = ValidationUtil.parseDate(dateParam, dateFormatter);
                        if (date == null) {
                            JSONUtil.sendBadRequest(response, "Invalid date format. Use yyyy-MM-dd");
                            return;
                        }
                        attendanceList = attendanceService.getAttendanceByDate(date);
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
                        JSONUtil.sendForbidden(response, "You can only view your own attendance");
                        return;
                    }
                }
                
                JSONUtil.sendSuccess(response, "Attendance retrieved", attendance);
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid request");
            
        } catch (Exception e) {
            System.err.println("Attendance GET error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
            // Parse request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            
            String requestBody = sb.toString();
            
            if (requestBody == null || requestBody.isEmpty()) {
                JSONUtil.sendBadRequest(response, "Request body is empty");
                return;
            }
            
            JsonObject jsonRequest;
            try {
                jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();
            } catch (Exception e) {
                JSONUtil.sendBadRequest(response, "Invalid JSON format");
                return;
            }
            
            // Required fields
            Integer employeeId = null;
            if (jsonRequest.has("employeeId") && !jsonRequest.get("employeeId").isJsonNull()) {
                employeeId = jsonRequest.get("employeeId").getAsInt();
            } else if (currentUser.getEmployeeId() != null) {
                employeeId = currentUser.getEmployeeId();
            }
            
            if (employeeId == null) {
                JSONUtil.sendBadRequest(response, "Employee ID is required");
                return;
            }
            
            String dateStr = jsonRequest.has("date") ? 
                jsonRequest.get("date").getAsString().trim() : null;
            LocalDate attendanceDate;
            
            if (dateStr != null && !dateStr.isEmpty()) {
                attendanceDate = ValidationUtil.parseDate(dateStr, dateFormatter);
            } else {
                attendanceDate = LocalDate.now(); // Default to today
            }
            
            if (attendanceDate == null) {
                JSONUtil.sendBadRequest(response, "Invalid date format. Use yyyy-MM-dd");
                return;
            }
            
            String statusStr = jsonRequest.has("status") ? 
                jsonRequest.get("status").getAsString().trim().toUpperCase() : "PRESENT";
            
            Attendance.AttendanceStatus status;
            try {
                status = Attendance.AttendanceStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                JSONUtil.sendBadRequest(response, "Invalid status. Must be PRESENT, ABSENT, or LEAVE");
                return;
            }
            
            // Create attendance
            Attendance attendance = new Attendance();
            attendance.setEmployeeId(employeeId);
            attendance.setAttendanceDate(attendanceDate);
            attendance.setStatus(status);
            
            int attendanceId = attendanceService.recordAttendance(attendance, currentUser.getId(), ipAddress);
            
            if (attendanceId > 0) {
                Optional<Attendance> createdAttendance = attendanceService.getAttendanceById(attendanceId);
                JSONUtil.sendCreated(response, "Attendance recorded successfully", createdAttendance.orElse(null));
            } else {
                JSONUtil.sendBadRequest(response, "Failed to record attendance");
            }
            
        } catch (Exception e) {
            System.err.println("Attendance POST error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while recording attendance");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
            if (pathInfo == null || pathInfo.equals("/")) {
                JSONUtil.sendBadRequest(response, "Attendance ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int attendanceId;
            try {
                attendanceId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid attendance ID");
                return;
            }
            
            // Parse request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            
            String requestBody = sb.toString();
            
            if (requestBody == null || requestBody.isEmpty()) {
                JSONUtil.sendBadRequest(response, "Request body is empty");
                return;
            }
            
            JsonObject jsonRequest;
            try {
                jsonRequest = JsonParser.parseString(requestBody).getAsJsonObject();
            } catch (Exception e) {
                JSONUtil.sendBadRequest(response, "Invalid JSON format");
                return;
            }
            
            // Get existing attendance
            Optional<Attendance> existingOpt = attendanceService.getAttendanceById(attendanceId);
            if (existingOpt.isEmpty()) {
                JSONUtil.sendNotFound(response, "Attendance record not found");
                return;
            }
            
            Attendance attendance = existingOpt.get();
            
            // Update status
            if (jsonRequest.has("status") && !jsonRequest.get("status").isJsonNull()) {
                String statusStr = jsonRequest.get("status").getAsString().trim().toUpperCase();
                try {
                    attendance.setStatus(Attendance.AttendanceStatus.valueOf(statusStr));
                } catch (IllegalArgumentException e) {
                    JSONUtil.sendBadRequest(response, "Invalid status");
                    return;
                }
            }
            
            // Update attendance
            if (attendanceService.updateAttendance(attendance, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Attendance updated successfully", attendance);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to update attendance");
            }
            
        } catch (Exception e) {
            System.err.println("Attendance PUT error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while updating attendance");
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
            if (pathInfo == null || pathInfo.equals("/")) {
                JSONUtil.sendBadRequest(response, "Attendance ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int attendanceId;
            try {
                attendanceId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid attendance ID");
                return;
            }
            
            // Delete attendance
            if (attendanceService.deleteAttendance(attendanceId, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Attendance deleted successfully", null);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to delete attendance");
            }
            
        } catch (Exception e) {
            System.err.println("Attendance DELETE error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while deleting attendance");
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

