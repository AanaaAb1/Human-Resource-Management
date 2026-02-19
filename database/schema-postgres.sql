-- HRM System Database Schema
-- PostgreSQL Database

-- Create Database
-- Run: createdb hrm_system
-- Or: psql -U postgres -c "CREATE DATABASE hrm_system;"

-- Connect to database
-- \c hrm_system

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'HR_MANAGER', 'EMPLOYEE')),
    employee_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Departments Table
CREATE TABLE IF NOT EXISTS departments (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Employees Table
CREATE TABLE IF NOT EXISTS employees (
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
);

-- Update users foreign key after employees table is created
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_user_employee;
ALTER TABLE users ADD CONSTRAINT fk_user_employee 
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL;

-- Attendance Table
CREATE TABLE IF NOT EXISTS attendance (
    id SERIAL PRIMARY KEY,
    employee_id INTEGER NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LEAVE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, attendance_date)
);

-- Leave Requests Table
CREATE TABLE IF NOT EXISTS leave_requests (
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
);

-- Payroll Table
CREATE TABLE IF NOT EXISTS payroll (
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
);

-- Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(attendance_date);
CREATE INDEX IF NOT EXISTS idx_leave_status ON leave_requests(status);
CREATE INDEX IF NOT EXISTS idx_payroll_employee ON payroll(employee_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);

-- Trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to tables
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_departments_updated_at ON departments;
CREATE TRIGGER update_departments_updated_at
    BEFORE UPDATE ON departments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_employees_updated_at ON employees;
CREATE TRIGGER update_employees_updated_at
    BEFORE UPDATE ON employees
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_leave_requests_updated_at ON leave_requests;
CREATE TRIGGER update_leave_requests_updated_at
    BEFORE UPDATE ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert Default Admin User (Password: admin123 - BCrypt hash)
INSERT INTO users (username, password, role) VALUES 
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/nMskyB.M0aUB/LQCGm/.', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

-- Insert Sample Departments
INSERT INTO departments (name, description) VALUES 
('Human Resources', 'Human Resource Management Department'),
('IT', 'Information Technology Department'),
('Finance', 'Financial Management Department'),
('Marketing', 'Marketing and Sales Department'),
('Operations', 'Operations Management Department')
ON CONFLICT DO NOTHING;

-- Insert Sample Employees
INSERT INTO employees (employee_id, full_name, gender, email, phone, address, department_id, job_title, hire_date, salary) VALUES 
('EMP001', 'John Smith', 'MALE', 'john.smith@company.com', '555-0101', '123 Main St, City', 2, 'Software Engineer', '2020-01-15', 75000.00),
('EMP002', 'Jane Doe', 'FEMALE', 'jane.doe@company.com', '555-0102', '456 Oak Ave, Town', 1, 'HR Manager', '2019-03-20', 65000.00),
('EMP003', 'Bob Johnson', 'MALE', 'bob.johnson@company.com', '555-0103', '789 Pine Rd, Village', 3, 'Financial Analyst', '2021-06-01', 55000.00),
('EMP004', 'Alice Williams', 'FEMALE', 'alice.williams@company.com', '555-0104', '321 Elm St, County', 4, 'Marketing Specialist', '2022-02-10', 50000.00),
('EMP005', 'Charlie Brown', 'MALE', 'charlie.brown@company.com', '555-0105', '654 Maple Dr, State', 5, 'Operations Manager', '2018-11-05', 70000.00)
ON CONFLICT (employee_id) DO NOTHING;

-- Sample Attendance Records
INSERT INTO attendance (employee_id, attendance_date, status) VALUES 
(1, CURRENT_DATE, 'PRESENT'),
(2, CURRENT_DATE, 'PRESENT'),
(3, CURRENT_DATE, 'LEAVE'),
(4, CURRENT_DATE, 'PRESENT'),
(5, CURRENT_DATE, 'ABSENT')
ON CONFLICT (employee_id, attendance_date) DO NOTHING;

-- Sample Leave Requests
INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, reason, status) VALUES 
(3, 'SICK', CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE + INTERVAL '3 day', 'Medical appointment', 'PENDING'),
(5, 'ANNUAL', CURRENT_DATE + INTERVAL '7 day', CURRENT_DATE + INTERVAL '14 day', 'Family vacation', 'PENDING')
ON CONFLICT DO NOTHING;

-- Grant permissions (adjust as needed for your setup)
-- GRANT ALL PRIVILEGES ON DATABASE hrm_system TO your_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO your_user;

