package com.hrm.servlet.employees;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hrm.model.Employee;
import com.hrm.model.User;
import com.hrm.service.EmployeeService;
import com.hrm.util.JSONUtil;
import com.hrm.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Servlet for employee CRUD operations
 */
public class EmployeeServlet extends HttpServlet {
    
    private final EmployeeService employeeService;
    private final DateTimeFormatter dateFormatter;
    
    public EmployeeServlet() {
        this.employeeService = new EmployeeService();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            String pathInfo = request.getPathInfo();
            User currentUser = (User) request.getAttribute("currentUser");
            String ipAddress = getClientIP(request);
            
            // GET /api/employees - Get all employees
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Employee> employees = employeeService.getAllEmployees();
                
                // Filter for employees - they can only see their own profile
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    Optional<Employee> selfEmployee = employees.stream()
                        .filter(e -> e.getId() == currentUser.getEmployeeId())
                        .findFirst();
                    
                    if (selfEmployee.isPresent()) {
                        JSONUtil.sendSuccess(response, "Employee retrieved", List.of(selfEmployee.get()));
                    } else {
                        JSONUtil.sendSuccess(response, "No employee profile found", List.of());
                    }
                } else {
                    JSONUtil.sendSuccess(response, "Employees retrieved", employees);
                }
                return;
            }
            
            // GET /api/employees/{id} - Get specific employee
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length >= 2) {
                int employeeId;
                try {
                    employeeId = Integer.parseInt(pathParts[1]);
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid employee ID");
                    return;
                }
                
                Optional<Employee> employeeOpt = employeeService.getEmployeeById(employeeId);
                
                if (employeeOpt.isEmpty()) {
                    JSONUtil.sendNotFound(response, "Employee not found");
                    return;
                }
                
                Employee employee = employeeOpt.get();
                
                // Check if user can access this employee
                if (currentUser.isEmployee() && !currentUser.isHRManager() && !currentUser.isAdmin()) {
                    if (employee.getId() != currentUser.getEmployeeId()) {
                        JSONUtil.sendForbidden(response, "You can only access your own profile");
                        return;
                    }
                }
                
                JSONUtil.sendSuccess(response, "Employee retrieved", employee);
                return;
            }
            
            JSONUtil.sendBadRequest(response, "Invalid request");
            
        } catch (Exception e) {
            System.err.println("Employee GET error: " + e.getMessage());
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
            
            // Create employee object
            Employee employee = new Employee();
            
            // Required fields
            String employeeId = jsonRequest.has("employeeId") ? 
                jsonRequest.get("employeeId").getAsString().trim() : null;
            String fullName = jsonRequest.has("fullName") ? 
                jsonRequest.get("fullName").getAsString().trim() : null;
            String email = jsonRequest.has("email") ? 
                jsonRequest.get("email").getAsString().trim().toLowerCase() : null;
            String hireDateStr = jsonRequest.has("hireDate") ? 
                jsonRequest.get("hireDate").getAsString().trim() : null;
            String salaryStr = jsonRequest.has("salary") ? 
                jsonRequest.get("salary").getAsString() : null;
            String genderStr = jsonRequest.has("gender") ? 
                jsonRequest.get("gender").getAsString().trim().toUpperCase() : null;
            
            // Validate required fields
            if (!ValidationUtil.isValidEmployeeId(employeeId)) {
                JSONUtil.sendBadRequest(response, "Invalid employee ID format");
                return;
            }
            
            if (!ValidationUtil.isValidName(fullName)) {
                JSONUtil.sendBadRequest(response, "Full name is required and must be valid");
                return;
            }
            
            if (!ValidationUtil.isValidEmail(email)) {
                JSONUtil.sendBadRequest(response, "Valid email is required");
                return;
            }
            
            LocalDate hireDate = ValidationUtil.parseDate(hireDateStr, dateFormatter);
            if (hireDate == null) {
                JSONUtil.sendBadRequest(response, "Valid hire date (yyyy-MM-dd) is required");
                return;
            }
            
            BigDecimal salary;
            try {
                salary = new BigDecimal(salaryStr);
                if (!ValidationUtil.isValidSalary(salary)) {
                    JSONUtil.sendBadRequest(response, "Valid positive salary is required");
                    return;
                }
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid salary format");
                return;
            }
            
            Employee.Gender gender;
            try {
                gender = Employee.Gender.valueOf(genderStr);
            } catch (IllegalArgumentException e) {
                JSONUtil.sendBadRequest(response, "Invalid gender. Must be MALE, FEMALE, or OTHER");
                return;
            }
            
            // Optional fields
            String phone = jsonRequest.has("phone") ? 
                jsonRequest.get("phone").getAsString().trim() : null;
            if (phone != null && !ValidationUtil.isValidPhone(phone)) {
                JSONUtil.sendBadRequest(response, "Invalid phone number format");
                return;
            }
            
            String address = jsonRequest.has("address") ? 
                jsonRequest.get("address").getAsString() : null;
            if (address != null && !ValidationUtil.isValidAddress(address)) {
                JSONUtil.sendBadRequest(response, "Address is too long");
                return;
            }
            
            Integer departmentId = null;
            if (jsonRequest.has("departmentId") && !jsonRequest.get("departmentId").isJsonNull()) {
                departmentId = jsonRequest.get("departmentId").getAsInt();
            }
            
            String jobTitle = jsonRequest.has("jobTitle") ? 
                jsonRequest.get("jobTitle").getAsString().trim() : null;
            
            // Set employee properties
            employee.setEmployeeId(employeeId);
            employee.setFullName(fullName);
            employee.setEmail(email);
            employee.setHireDate(hireDate);
            employee.setSalary(salary);
            employee.setGender(gender);
            employee.setPhone(phone);
            employee.setAddress(address);
            employee.setDepartmentId(departmentId);
            employee.setJobTitle(jobTitle);
            
            // Create employee
            int newEmployeeId = employeeService.createEmployee(employee, currentUser.getId(), ipAddress);
            
            if (newEmployeeId > 0) {
                // Get the created employee
                Optional<Employee> createdEmployee = employeeService.getEmployeeById(newEmployeeId);
                JSONUtil.sendCreated(response, "Employee created successfully", createdEmployee.orElse(null));
            } else {
                JSONUtil.sendBadRequest(response, "Failed to create employee. Employee ID or email may already exist.");
            }
            
        } catch (Exception e) {
            System.err.println("Employee POST error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while creating employee");
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
                JSONUtil.sendBadRequest(response, "Employee ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int employeeId;
            try {
                employeeId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid employee ID");
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
            
            // Get existing employee
            Optional<Employee> existingOpt = employeeService.getEmployeeById(employeeId);
            if (existingOpt.isEmpty()) {
                JSONUtil.sendNotFound(response, "Employee not found");
                return;
            }
            
            Employee employee = existingOpt.get();
            
            // Update fields (all are optional for PUT, but validate if provided)
            if (jsonRequest.has("fullName") && !jsonRequest.get("fullName").isJsonNull()) {
                String fullName = jsonRequest.get("fullName").getAsString().trim();
                if (!ValidationUtil.isValidName(fullName)) {
                    JSONUtil.sendBadRequest(response, "Invalid full name");
                    return;
                }
                employee.setFullName(fullName);
            }
            
            if (jsonRequest.has("email") && !jsonRequest.get("email").isJsonNull()) {
                String email = jsonRequest.get("email").getAsString().trim().toLowerCase();
                if (!ValidationUtil.isValidEmail(email)) {
                    JSONUtil.sendBadRequest(response, "Invalid email format");
                    return;
                }
                employee.setEmail(email);
            }
            
            if (jsonRequest.has("gender") && !jsonRequest.get("gender").isJsonNull()) {
                try {
                    employee.setGender(Employee.Gender.valueOf(
                        jsonRequest.get("gender").getAsString().trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    JSONUtil.sendBadRequest(response, "Invalid gender");
                    return;
                }
            }
            
            if (jsonRequest.has("phone") && !jsonRequest.get("phone").isJsonNull()) {
                String phone = jsonRequest.get("phone").getAsString().trim();
                if (!ValidationUtil.isValidPhone(phone)) {
                    JSONUtil.sendBadRequest(response, "Invalid phone number");
                    return;
                }
                employee.setPhone(phone);
            }
            
            if (jsonRequest.has("address") && !jsonRequest.get("address").isJsonNull()) {
                String address = jsonRequest.get("address").getAsString();
                if (!ValidationUtil.isValidAddress(address)) {
                    JSONUtil.sendBadRequest(response, "Address is too long");
                    return;
                }
                employee.setAddress(address);
            }
            
            if (jsonRequest.has("departmentId") && !jsonRequest.get("departmentId").isJsonNull()) {
                employee.setDepartmentId(jsonRequest.get("departmentId").getAsInt());
            }
            
            if (jsonRequest.has("jobTitle") && !jsonRequest.get("jobTitle").isJsonNull()) {
                employee.setJobTitle(jsonRequest.get("jobTitle").getAsString().trim());
            }
            
            if (jsonRequest.has("hireDate") && !jsonRequest.get("hireDate").isJsonNull()) {
                LocalDate hireDate = ValidationUtil.parseDate(
                    jsonRequest.get("hireDate").getAsString().trim(), dateFormatter);
                if (hireDate == null) {
                    JSONUtil.sendBadRequest(response, "Invalid hire date format");
                    return;
                }
                employee.setHireDate(hireDate);
            }
            
            if (jsonRequest.has("salary") && !jsonRequest.get("salary").isJsonNull()) {
                try {
                    BigDecimal salary = new BigDecimal(jsonRequest.get("salary").getAsString());
                    if (!ValidationUtil.isValidSalary(salary)) {
                        JSONUtil.sendBadRequest(response, "Invalid salary");
                        return;
                    }
                    employee.setSalary(salary);
                } catch (NumberFormatException e) {
                    JSONUtil.sendBadRequest(response, "Invalid salary format");
                    return;
                }
            }
            
            // Update employee
            if (employeeService.updateEmployee(employee, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Employee updated successfully", employee);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to update employee");
            }
            
        } catch (Exception e) {
            System.err.println("Employee PUT error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while updating employee");
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
                JSONUtil.sendBadRequest(response, "Employee ID is required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                JSONUtil.sendBadRequest(response, "Invalid request");
                return;
            }
            
            int employeeId;
            try {
                employeeId = Integer.parseInt(pathParts[1]);
            } catch (NumberFormatException e) {
                JSONUtil.sendBadRequest(response, "Invalid employee ID");
                return;
            }
            
            // Delete employee
            if (employeeService.deleteEmployee(employeeId, currentUser.getId(), ipAddress)) {
                JSONUtil.sendSuccess(response, "Employee deleted successfully", null);
            } else {
                JSONUtil.sendBadRequest(response, "Failed to delete employee. Employee may not exist.");
            }
            
        } catch (Exception e) {
            System.err.println("Employee DELETE error: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "An error occurred while deleting employee");
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

