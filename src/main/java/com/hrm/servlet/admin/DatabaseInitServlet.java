package com.hrm.servlet.admin;

import com.hrm.config.DatabaseConfig;
import com.hrm.util.JSONUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Servlet to initialize the database schema programmatically
 * Accessible only to administrators
 */
public class DatabaseInitServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            // Create tables using raw SQL
            createTables();
            
            // Insert sample data
            insertSampleData();
            
            JSONUtil.sendSuccess(response, "Database initialized successfully", null);
            
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            JSONUtil.sendInternalError(response, "Database initialization failed: " + e.getMessage());
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        try {
            DatabaseConfig config = DatabaseConfig.getInstance();
            
            if (config.testConnection()) {
                JSONUtil.sendSuccess(response, "Database connection successful", 
                    java.util.Map.of(
                        "dbType", config.getDbType(),
                        "dbUrl", config.getDbUrl()
                    ));
            } else {
                JSONUtil.sendInternalError(response, "Database connection failed");
            }
        } catch (Exception e) {
            JSONUtil.sendInternalError(response, "Error: " + e.getMessage());
        }
    }
    
    private void createTables() throws SQLException {
        DatabaseConfig config = DatabaseConfig.getInstance();
        String dbType = config.getDbType();
        
        try (Connection conn = config.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Drop existing tables in correct order (due to foreign keys)
            stmt.execute("DROP TABLE IF EXISTS audit_logs CASCADE");
            stmt.execute("DROP TABLE IF EXISTS payroll CASCADE");
            stmt.execute("DROP TABLE IF EXISTS leave_requests CASCADE");
            stmt.execute("DROP TABLE IF EXISTS attendance CASCADE");
            stmt.execute("DROP TABLE IF EXISTS users CASCADE");
            stmt.execute("DROP TABLE IF EXISTS employees CASCADE");
            stmt.execute("DROP TABLE IF EXISTS departments CASCADE");
            
            // Create departments table
            stmt.execute("""
                CREATE TABLE departments (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create employees table
            stmt.execute("""
                CREATE TABLE employees (
                    id SERIAL PRIMARY KEY,
                    employee_id VARCHAR(20) NOT NULL UNIQUE,
                    full_name VARCHAR(100) NOT NULL,
                    gender VARCHAR(10) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
                    email VARCHAR(100) NOT NULL UNIQUE,
                    phone VARCHAR(20),
                    address TEXT,
                    department_id INTEGER REFERENCES departments(id) ON DELETE SET NULL,
                    job_title VARCHAR(100),
                    hire_date DATE NOT NULL,
                    salary DECIMAL(10, 2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create users table
            stmt.execute("""
                CREATE TABLE users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'HR_MANAGER', 'EMPLOYEE')),
                    employee_id INTEGER REFERENCES employees(id) ON DELETE SET NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create attendance table
            stmt.execute("""
                CREATE TABLE attendance (
                    id SERIAL PRIMARY KEY,
                    employee_id INTEGER NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                    attendance_date DATE NOT NULL,
                    status VARCHAR(10) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LEAVE')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(employee_id, attendance_date)
                )
            """);
            
            // Create leave_requests table
            stmt.execute("""
                CREATE TABLE leave_requests (
                    id SERIAL PRIMARY KEY,
                    employee_id INTEGER NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                    leave_type VARCHAR(20) NOT NULL CHECK (leave_type IN ('ANNUAL', 'SICK', 'PERSONAL', 'MATERNITY', 'PATERNITY', 'UNPAID')),
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    reason TEXT,
                    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
                    approved_by INTEGER REFERENCES employees(id) ON DELETE SET NULL,
                    approved_at TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create payroll table
            stmt.execute("""
                CREATE TABLE payroll (
                    id SERIAL PRIMARY KEY,
                    employee_id INTEGER NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                    month INTEGER NOT NULL CHECK (month >= 1 AND month <= 12),
                    year INTEGER NOT NULL CHECK (year >= 2000 AND year <= 2100),
                    basic_salary DECIMAL(10, 2) NOT NULL,
                    allowances DECIMAL(10, 2) DEFAULT 0,
                    deductions DECIMAL(10, 2) DEFAULT 0,
                    net_salary DECIMAL(10, 2) NOT NULL,
                    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(employee_id, month, year)
                )
            """);
            
            // Create audit_logs table
            stmt.execute("""
                CREATE TABLE audit_logs (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
                    action VARCHAR(100) NOT NULL,
                    details TEXT,
                    ip_address VARCHAR(45),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create indexes
            stmt.execute("CREATE INDEX idx_attendance_date ON attendance(attendance_date)");
            stmt.execute("CREATE INDEX idx_leave_status ON leave_requests(status)");
            stmt.execute("CREATE INDEX idx_payroll_employee ON payroll(employee_id)");
            stmt.execute("CREATE INDEX idx_audit_user ON audit_logs(user_id)");
            
            // Create trigger function for updated_at
            stmt.execute("""
                CREATE OR REPLACE FUNCTION update_updated_at_column()
                RETURNS TRIGGER AS $$
                BEGIN
                    NEW.updated_at = CURRENT_TIMESTAMP;
                    RETURN NEW;
                END;
                $$ language 'plpgsql'
            """);
            
            // Apply triggers
            stmt.execute("DROP TRIGGER IF EXISTS update_employees_updated_at ON employees");
            stmt.execute("""
                CREATE TRIGGER update_employees_updated_at
                BEFORE UPDATE ON employees
                FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()
            """);
            
            System.out.println("All tables created successfully");
        }
    }
    
    private void insertSampleData() throws SQLException {
        DatabaseConfig config = DatabaseConfig.getInstance();
        
        try (Connection conn = config.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Insert departments
            stmt.execute("""
                INSERT INTO departments (name, description) VALUES
                ('Human Resources', 'Human Resource Management Department'),
                ('IT', 'Information Technology Department'),
                ('Finance', 'Financial Management Department'),
                ('Marketing', 'Marketing and Sales Department'),
                ('Operations', 'Operations Management Department')
            """);
            
            // Insert employees
            stmt.execute("""
                INSERT INTO employees (employee_id, full_name, gender, email, phone, address, department_id, job_title, hire_date, salary) VALUES
                ('EMP001', 'John Smith', 'MALE', 'john.smith@company.com', '555-0101', '123 Main St, City', 2, 'Software Engineer', '2020-01-15', 75000.00),
                ('EMP002', 'Jane Doe', 'FEMALE', 'jane.doe@company.com', '555-0102', '456 Oak Ave, Town', 1, 'HR Manager', '2019-03-20', 65000.00),
                ('EMP003', 'Bob Johnson', 'MALE', 'bob.johnson@company.com', '555-0103', '789 Pine Rd, Village', 3, 'Financial Analyst', '2021-06-01', 55000.00),
                ('EMP004', 'Alice Williams', 'FEMALE', 'alice.williams@company.com', '555-0104', '321 Elm St, County', 4, 'Marketing Specialist', '2022-02-10', 50000.00),
                ('EMP005', 'Charlie Brown', 'MALE', 'charlie.brown@company.com', '555-0105', '654 Maple Dr, State', 5, 'Operations Manager', '2018-11-05', 70000.00)
            """);
            
            // Insert admin user (password: admin123 - BCrypt hash)
            stmt.execute("""
                INSERT INTO users (username, password, role) VALUES
                ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/nMskyB.M0aUB/LQCGm/.', 'ADMIN')
            """);
            
            // Insert sample attendance
            stmt.execute("""
                INSERT INTO attendance (employee_id, attendance_date, status) VALUES
                (1, CURRENT_DATE, 'PRESENT'),
                (2, CURRENT_DATE, 'PRESENT'),
                (3, CURRENT_DATE, 'LEAVE'),
                (4, CURRENT_DATE, 'PRESENT'),
                (5, CURRENT_DATE, 'ABSENT')
            """);
            
            // Insert sample leave requests
            stmt.execute("""
                INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, reason, status) VALUES
                (3, 'SICK', CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE + INTERVAL '3 day', 'Medical appointment', 'PENDING'),
                (5, 'ANNUAL', CURRENT_DATE + INTERVAL '7 day', CURRENT_DATE + INTERVAL '14 day', 'Family vacation', 'PENDING')
            """);
            
            System.out.println("Sample data inserted successfully");
        }
    }
}

