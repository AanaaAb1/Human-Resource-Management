-- HRM System Database Schema
-- MySQL Database

-- Create Database
CREATE DATABASE IF NOT EXISTS hrm_system;
USE hrm_system;

-- Users Table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'HR_MANAGER', 'EMPLOYEE') NOT NULL,
    employee_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL
);

-- Departments Table
CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Employees Table
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    department_id INT,
    job_title VARCHAR(100),
    hire_date DATE NOT NULL,
    salary DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- Update users foreign key after employees table is created
ALTER TABLE users
ADD CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL;

-- Attendance Table
CREATE TABLE attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT', 'LEAVE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_attendance (employee_id, attendance_date)
);

-- Leave Requests Table
CREATE TABLE leave_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    leave_type ENUM('ANNUAL', 'SICK', 'PERSONAL', 'MATERNITY', 'PATERNITY', 'UNPAID') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    approved_by INT,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES employees(id) ON DELETE SET NULL
);

-- Payroll Table
CREATE TABLE payroll (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    basic_salary DECIMAL(10, 2) NOT NULL,
    allowances DECIMAL(10, 2) DEFAULT 0,
    deductions DECIMAL(10, 2) DEFAULT 0,
    net_salary DECIMAL(10, 2) NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_payroll (employee_id, month, year)
);

-- Audit Logs Table
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Insert Default Admin User (Password: admin123)
INSERT INTO users (username, password, role) VALUES 
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/nMskyB.M0aUB/LQCGm/.', 'ADMIN');

-- Insert Sample Departments
INSERT INTO departments (name, description) VALUES 
('Human Resources', 'Human Resource Management Department'),
('IT', 'Information Technology Department'),
('Finance', 'Financial Management Department'),
('Marketing', 'Marketing and Sales Department'),
('Operations', 'Operations Management Department');

-- Insert Sample Employees
INSERT INTO employees (employee_id, full_name, gender, email, phone, address, department_id, job_title, hire_date, salary) VALUES 
('EMP001', 'John Smith', 'MALE', 'john.smith@company.com', '555-0101', '123 Main St, City', 2, 'Software Engineer', '2020-01-15', 75000.00),
('EMP002', 'Jane Doe', 'FEMALE', 'jane.doe@company.com', '555-0102', '456 Oak Ave, Town', 1, 'HR Manager', '2019-03-20', 65000.00),
('EMP003', 'Bob Johnson', 'MALE', 'bob.johnson@company.com', '555-0103', '789 Pine Rd, Village', 3, 'Financial Analyst', '2021-06-01', 55000.00),
('EMP004', 'Alice Williams', 'FEMALE', 'alice.williams@company.com', '555-0104', '321 Elm St, County', 4, 'Marketing Specialist', '2022-02-10', 50000.00),
('EMP005', 'Charlie Brown', 'MALE', 'charlie.brown@company.com', '555-0105', '654 Maple Dr, State', 5, 'Operations Manager', '2018-11-05', 70000.00);

-- Sample Attendance Records
INSERT INTO attendance (employee_id, attendance_date, status) VALUES 
(1, CURDATE(), 'PRESENT'),
(2, CURDATE(), 'PRESENT'),
(3, CURDATE(), 'LEAVE'),
(4, CURDATE(), 'PRESENT'),
(5, CURDATE(), 'ABSENT');

-- Sample Leave Requests
INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, reason, status) VALUES 
(3, 'SICK', DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'Medical appointment', 'PENDING'),
(5, 'ANNUAL', DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'Family vacation', 'PENDING');

-- Create Indexes for better performance
CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_leave_status ON leave_requests(status);
CREATE INDEX idx_payroll_employee ON payroll(employee_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);

