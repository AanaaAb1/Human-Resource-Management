// HRM System JavaScript Application

const API_BASE = '/api';
let currentUser = null;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    checkAuth();
    setupEventListeners();
});

// Check authentication status
async function checkAuth() {
    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });
        
        const data = await response.json();
        
        if (data.success && data.data && data.data.loggedIn) {
            currentUser = data.data;
            showMainContent();
            loadDashboard();
        } else {
            showLoginSection();
        }
    } catch (error) {
        console.error('Auth check failed:', error);
        showLoginSection();
    }
}

// Setup event listeners
function setupEventListeners() {
    // Login form
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    
    // Employee form
    document.getElementById('employee-form-element').addEventListener('submit', handleEmployeeSubmit);
    
    // Department form
    document.getElementById('department-form-element').addEventListener('submit', handleDepartmentSubmit);
    
    // Attendance form
    document.getElementById('attendance-form-element').addEventListener('submit', handleAttendanceSubmit);
    
    // Leave form
    document.getElementById('leave-form-element').addEventListener('submit', handleLeaveSubmit);
    
    // Payroll form
    document.getElementById('payroll-form-element').addEventListener('submit', handlePayrollSubmit);
}

// Authentication
async function handleLogin(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorEl = document.getElementById('login-error');
    
    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            currentUser = data.data;
            errorEl.classList.remove('show');
            showMainContent();
            loadDashboard();
            showToast('Login successful!', 'success');
        } else {
            errorEl.textContent = data.message;
            errorEl.classList.add('show');
        }
    } catch (error) {
        console.error('Login failed:', error);
        errorEl.textContent = 'An error occurred. Please try again.';
        errorEl.classList.add('show');
    }
}

async function logout() {
    try {
        await fetch(`${API_BASE}/auth/logout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (error) {
        console.error('Logout error:', error);
    }
    
    currentUser = null;
    showLoginSection();
    showToast('Logged out successfully', 'success');
}

// UI Functions
function showLoginSection() {
    document.getElementById('login-section').style.display = 'block';
    document.getElementById('main-content').style.display = 'none';
    document.getElementById('user-info').style.display = 'none';
}

function showMainContent() {
    document.getElementById('login-section').style.display = 'none';
    document.getElementById('main-content').style.display = 'block';
    document.getElementById('user-info').style.display = 'flex';
    document.getElementById('current-user').textContent = 
        `Welcome, ${currentUser.username} (${currentUser.role})`;
}

function showTab(tabName) {
    // Update tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    
    // Update tab content
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    document.getElementById(tabName).classList.add('active');
    
    // Load tab data
    switch(tabName) {
        case 'dashboard': loadDashboard(); break;
        case 'employees': loadEmployees(); break;
        case 'departments': loadDepartments(); break;
        case 'attendance': loadAttendance(); break;
        case 'leave': loadLeaveRequests(); break;
        case 'payroll': loadPayroll(); break;
    }
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// Dashboard
async function loadDashboard() {
    try {
        const response = await fetch(`${API_BASE}/reports/summary`);
        const data = await response.json();
        
        if (data.success) {
            const stats = data.data;
            document.getElementById('dashboard-stats').innerHTML = `
                <div class="stat-card">
                    <h3>${stats.totalEmployees || 0}</h3>
                    <p>Total Employees</p>
                </div>
                <div class="stat-card">
                    <h3>${stats.totalDepartments || 0}</h3>
                    <p>Departments</p>
                </div>
                <div class="stat-card">
                    <h3>${stats.pendingLeaveRequests || 0}</h3>
                    <p>Pending Leave Requests</p>
                </div>
                <div class="stat-card">
                    <h3>$${parseFloat(stats.currentMonthPayroll || 0).toLocaleString()}</h3>
                    <p>Current Month Payroll</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Failed to load dashboard:', error);
    }
}

// Employees
async function loadEmployees() {
    try {
        const response = await fetch(`${API_BASE}/employees`);
        const data = await response.json();
        
        if (data.success) {
            const tbody = document.getElementById('employees-table-body');
            tbody.innerHTML = data.data.map(emp => `
                <tr>
                    <td>${emp.employeeId}</td>
                    <td>${emp.fullName}</td>
                    <td>${emp.email}</td>
                    <td>${emp.departmentName || 'N/A'}</td>
                    <td>${emp.jobTitle || 'N/A'}</td>
                    <td class="actions">
                        <button class="btn btn-secondary" onclick="editEmployee(${emp.id})">Edit</button>
                        ${currentUser.role === 'ADMIN' ? 
                            `<button class="btn btn-danger" onclick="deleteEmployee(${emp.id})">Delete</button>` : ''}
                    </td>
                </tr>
            `).join('');
            
            // Load departments for dropdown
            loadDepartmentsForDropdown();
        }
    } catch (error) {
        console.error('Failed to load employees:', error);
        showToast('Failed to load employees', 'error');
    }
}

function showAddEmployeeForm() {
    document.getElementById('employee-form-title').textContent = 'Add Employee';
    document.getElementById('employee-form-element').reset();
    document.getElementById('employee-id').value = '';
    document.getElementById('employee-form').style.display = 'block';
}

function hideEmployeeForm() {
    document.getElementById('employee-form').style.display = 'none';
}

async function editEmployee(id) {
    try {
        const response = await fetch(`${API_BASE}/employees/${id}`);
        const data = await response.json();
        
        if (data.success) {
            const emp = data.data;
            document.getElementById('employee-form-title').textContent = 'Edit Employee';
            document.getElementById('employee-id').value = emp.id;
            document.getElementById('emp-employee-id').value = emp.employeeId;
            document.getElementById('emp-full-name').value = emp.fullName;
            document.getElementById('emp-email').value = emp.email;
            document.getElementById('emp-phone').value = emp.phone || '';
            document.getElementById('emp-gender').value = emp.gender;
            document.getElementById('emp-department').value = emp.departmentId || '';
            document.getElementById('emp-job-title').value = emp.jobTitle || '';
            document.getElementById('emp-hire-date').value = emp.hireDate;
            document.getElementById('emp-salary').value = emp.salary;
            document.getElementById('emp-address').value = emp.address || '';
            document.getElementById('employee-form').style.display = 'block';
        }
    } catch (error) {
        console.error('Failed to load employee:', error);
        showToast('Failed to load employee details', 'error');
    }
}

async function deleteEmployee(id) {
    if (!confirm('Are you sure you want to delete this employee?')) return;
    
    try {
        const response = await fetch(`${API_BASE}/employees/${id}`, { method: 'DELETE' });
        const data = await response.json();
        
        if (data.success) {
            showToast('Employee deleted successfully', 'success');
            loadEmployees();
        } else {
            showToast(data.message || 'Failed to delete employee', 'error');
        }
    } catch (error) {
        console.error('Failed to delete employee:', error);
        showToast('Failed to delete employee', 'error');
    }
}

async function handleEmployeeSubmit(e) {
    e.preventDefault();
    
    const id = document.getElementById('employee-id').value;
    const employeeData = {
        employeeId: document.getElementById('emp-employee-id').value,
        fullName: document.getElementById('emp-full-name').value,
        email: document.getElementById('emp-email').value,
        phone: document.getElementById('emp-phone').value,
        gender: document.getElementById('emp-gender').value,
        departmentId: document.getElementById('emp-department').value || null,
        jobTitle: document.getElementById('emp-job-title').value,
        hireDate: document.getElementById('emp-hire-date').value,
        salary: document.getElementById('emp-salary').value,
        address: document.getElementById('emp-address').value
    };
    
    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE}/employees/${id}` : `${API_BASE}/employees`;
    
    try {
        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(employeeData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast(id ? 'Employee updated successfully' : 'Employee created successfully', 'success');
            hideEmployeeForm();
            loadEmployees();
        } else {
            showToast(data.message || 'Operation failed', 'error');
        }
    } catch (error) {
        console.error('Failed to save employee:', error);
        showToast('Failed to save employee', 'error');
    }
}

// Departments
async function loadDepartments() {
    try {
        const response = await fetch(`${API_BASE}/departments`);
        const data = await response.json();
        
        if (data.success) {
            const tbody = document.getElementById('departments-table-body');
            tbody.innerHTML = data.data.map(dept => `
                <tr>
                    <td>${dept.id}</td>
                    <td>${dept.name}</td>
                    <td>${dept.description || 'N/A'}</td>
                    <td class="actions">
                        <button class="btn btn-secondary" onclick="editDepartment(${dept.id})">Edit</button>
                        ${currentUser.role === 'ADMIN' ? 
                            `<button class="btn btn-danger" onclick="deleteDepartment(${dept.id})">Delete</button>` : ''}
                    </td>
                </tr>
            `).join('');
        }
    } catch (error) {
        console.error('Failed to load departments:', error);
        showToast('Failed to load departments', 'error');
    }
}

function showAddDepartmentForm() {
    document.getElementById('department-form-title').textContent = 'Add Department';
    document.getElementById('department-form-element').reset();
    document.getElementById('department-id').value = '';
    document.getElementById('department-form').style.display = 'block';
}

function hideDepartmentForm() {
    document.getElementById('department-form').style.display = 'none';
}

async function editDepartment(id) {
    try {
        const response = await fetch(`${API_BASE}/departments/${id}`);
        const data = await response.json();
        
        if (data.success) {
            const dept = data.data;
            document.getElementById('department-form-title').textContent = 'Edit Department';
            document.getElementById('department-id').value = dept.id;
            document.getElementById('department-name').value = dept.name;
            document.getElementById('department-description').value = dept.description || '';
            document.getElementById('department-form').style.display = 'block';
        }
    } catch (error) {
        console.error('Failed to load department:', error);
        showToast('Failed to load department details', 'error');
    }
}

async function deleteDepartment(id) {
    if (!confirm('Are you sure you want to delete this department?')) return;
    
    try {
        const response = await fetch(`${API_BASE}/departments/${id}`, { method: 'DELETE' });
        const data = await response.json();
        
        if (data.success) {
            showToast('Department deleted successfully', 'success');
            loadDepartments();
        } else {
            showToast(data.message || 'Failed to delete department', 'error');
        }
    } catch (error) {
        console.error('Failed to delete department:', error);
        showToast('Failed to delete department', 'error');
    }
}

async function handleDepartmentSubmit(e) {
    e.preventDefault();
    
    const id = document.getElementById('department-id').value;
    const departmentData = {
        name: document.getElementById('department-name').value,
        description: document.getElementById('department-description').value
    };
    
    const method = id ? 'PUT' : 'POST';
    const url = id ? `${API_BASE}/departments/${id}` : `${API_BASE}/departments`;
    
    try {
        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(departmentData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast(id ? 'Department updated successfully' : 'Department created successfully', 'success');
            hideDepartmentForm();
            loadDepartments();
        } else {
            showToast(data.message || 'Operation failed', 'error');
        }
    } catch (error) {
        console.error('Failed to save department:', error);
        showToast('Failed to save department', 'error');
    }
}

// Attendance
async function loadAttendance() {
    try {
        const response = await fetch(`${API_BASE}/attendance`);
        const data = await response.json();
        
        if (data.success) {
            const tbody = document.getElementById('attendance-table-body');
            tbody.innerHTML = data.data.map(att => `
                <tr>
                    <td>${att.attendanceDate}</td>
                    <td>${att.employeeName || att.employeeId}</td>
                    <td><span class="status-badge status-${att.status.toLowerCase()}">${att.status}</span></td>
                    <td class="actions">
                        <button class="btn btn-secondary" onclick="editAttendance(${att.id})">Edit</button>
                        <button class="btn btn-danger" onclick="deleteAttendance(${att.id})">Delete</button>
                    </td>
                </tr>
            `).join('');
            
            loadEmployeesForAttendanceDropdown();
        }
    } catch (error) {
        console.error('Failed to load attendance:', error);
        showToast('Failed to load attendance', 'error');
    }
}

function showAddAttendanceForm() {
    document.getElementById('attendance-form-element').reset();
    document.getElementById('attendance-date').value = new Date().toISOString().split('T')[0];
    document.getElementById('attendance-form').style.display = 'block';
}

function hideAttendanceForm() {
    document.getElementById('attendance-form').style.display = 'none';
}

async function handleAttendanceSubmit(e) {
    e.preventDefault();
    
    const attendanceData = {
        employeeId: parseInt(document.getElementById('attendance-employee').value),
        date: document.getElementById('attendance-date').value,
        status: document.getElementById('attendance-status').value
    };
    
    try {
        const response = await fetch(`${API_BASE}/attendance`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(attendanceData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Attendance recorded successfully', 'success');
            hideAttendanceForm();
            loadAttendance();
        } else {
            showToast(data.message || 'Failed to record attendance', 'error');
        }
    } catch (error) {
        console.error('Failed to record attendance:', error);
        showToast('Failed to record attendance', 'error');
    }
}

async function editAttendance(id) {
    try {
        const response = await fetch(`${API_BASE}/attendance/${id}`);
        const data = await response.json();
        
        if (data.success) {
            const att = data.data;
            await loadEmployeesForAttendanceDropdown();
            document.getElementById('attendance-employee').value = att.employeeId;
            document.getElementById('attendance-date').value = att.attendanceDate;
            document.getElementById('attendance-status').value = att.status;
            document.getElementById('attendance-form').style.display = 'block';
        }
    } catch (error) {
        console.error('Failed to load attendance:', error);
        showToast('Failed to load attendance details', 'error');
    }
}

async function deleteAttendance(id) {
    if (!confirm('Are you sure you want to delete this attendance record?')) return;
    
    try {
        const response = await fetch(`${API_BASE}/attendance/${id}`, { method: 'DELETE' });
        const data = await response.json();
        
        if (data.success) {
            showToast('Attendance deleted successfully', 'success');
            loadAttendance();
        } else {
            showToast(data.message || 'Failed to delete attendance', 'error');
        }
    } catch (error) {
        console.error('Failed to delete attendance:', error);
        showToast('Failed to delete attendance', 'error');
    }
}

// Leave Requests
async function loadLeaveRequests() {
    try {
        const response = await fetch(`${API_BASE}/leave`);
        const data = await response.json();
        
        if (data.success) {
            const tbody = document.getElementById('leave-table-body');
            tbody.innerHTML = data.data.map(leave => `
                <tr>
                    <td>${leave.employeeName || leave.employeeId}</td>
                    <td>${leave.leaveType}</td>
                    <td>${leave.startDate}</td>
                    <td>${leave.endDate}</td>
                    <td><span class="status-badge status-${leave.status.toLowerCase()}">${leave.status}</span></td>
                    <td class="actions">
                        ${leave.status === 'PENDING' && (currentUser.role === 'ADMIN' || currentUser.role === 'HR_MANAGER') ? `
                            <button class="btn btn-success" onclick="approveLeave(${leave.id})">Approve</button>
                            <button class="btn btn-danger" onclick="rejectLeave(${leave.id})">Reject</button>
                        ` : ''}
                    </td>
                </tr>
            `).join('');
            
            loadEmployeesForLeaveDropdown();
        }
    } catch (error) {
        console.error('Failed to load leave requests:', error);
        showToast('Failed to load leave requests', 'error');
    }
}

function showAddLeaveForm() {
    document.getElementById('leave-form-element').reset();
    document.getElementById('leave-form').style.display = 'block';
}

function hideLeaveForm() {
    document.getElementById('leave-form').style.display = 'none';
}

async function handleLeaveSubmit(e) {
    e.preventDefault();
    
    const leaveData = {
        employeeId: parseInt(document.getElementById('leave-employee').value),
        leaveType: document.getElementById('leave-type').value,
        startDate: document.getElementById('leave-start-date').value,
        endDate: document.getElementById('leave-end-date').value,
        reason: document.getElementById('leave-reason').value
    };
    
    try {
        const response = await fetch(`${API_BASE}/leave`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(leaveData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Leave request submitted successfully', 'success');
            hideLeaveForm();
            loadLeaveRequests();
        } else {
            showToast(data.message || 'Failed to submit leave request', 'error');
        }
    } catch (error) {
        console.error('Failed to submit leave request:', error);
        showToast('Failed to submit leave request', 'error');
    }
}

async function approveLeave(id) {
    try {
        const response = await fetch(`${API_BASE}/leave/${id}/approve`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' }
        });
        const data = await response.json();
        
        if (data.success) {
            showToast('Leave request approved', 'success');
            loadLeaveRequests();
        } else {
            showToast(data.message || 'Failed to approve leave request', 'error');
        }
    } catch (error) {
        console.error('Failed to approve leave:', error);
        showToast('Failed to approve leave request', 'error');
    }
}

async function rejectLeave(id) {
    try {
        const response = await fetch(`${API_BASE}/leave/${id}/reject`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' }
        });
        const data = await response.json();
        
        if (data.success) {
            showToast('Leave request rejected', 'success');
            loadLeaveRequests();
        } else {
            showToast(data.message || 'Failed to reject leave request', 'error');
        }
    } catch (error) {
        console.error('Failed to reject leave:', error);
        showToast('Failed to reject leave request', 'error');
    }
}

// Payroll
async function loadPayroll() {
    try {
        const response = await fetch(`${API_BASE}/payroll`);
        const data = await response.json();
        
        if (data.success) {
            const tbody = document.getElementById('payroll-table-body');
            tbody.innerHTML = data.data.map(pay => `
                <tr>
                    <td>${pay.employeeName || pay.employeeId}</td>
                    <td>${pay.month}/${pay.year}</td>
                    <td>$${parseFloat(pay.basicSalary).toLocaleString()}</td>
                    <td>$${parseFloat(pay.allowances).toLocaleString()}</td>
                    <td>$${parseFloat(pay.deductions).toLocaleString()}</td>
                    <td><strong>$${parseFloat(pay.netSalary).toLocaleString()}</strong></td>
                </tr>
            `).join('');
            
            loadEmployeesForPayrollDropdown();
        }
    } catch (error) {
        console.error('Failed to load payroll:', error);
        showToast('Failed to load payroll', 'error');
    }
}

function showGeneratePayrollForm() {
    document.getElementById('payroll-form-element').reset();
    const now = new Date();
    document.getElementById('payroll-month').value = now.getMonth() + 1;
    document.getElementById('payroll-year').value = now.getFullYear();
    document.getElementById('payroll-form').style.display = 'block';
}

function hidePayrollForm() {
    document.getElementById('payroll-form').style.display = 'none';
}

async function handlePayrollSubmit(e) {
    e.preventDefault();
    
    const payrollData = {
        employeeId: parseInt(document.getElementById('payroll-employee').value),
        month: parseInt(document.getElementById('payroll-month').value),
        year: parseInt(document.getElementById('payroll-year').value),
        allowances: document.getElementById('payroll-allowances').value || null,
        deductions: document.getElementById('payroll-deductions').value || null
    };
    
    try {
        const response = await fetch(`${API_BASE}/payroll/generate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payrollData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Payroll generated successfully', 'success');
            hidePayrollForm();
            loadPayroll();
        } else {
            showToast(data.message || 'Failed to generate payroll', 'error');
        }
    } catch (error) {
        console.error('Failed to generate payroll:', error);
        showToast('Failed to generate payroll', 'error');
    }
}

// Reports
async function generateReport(reportType) {
    try {
        const response = await fetch(`${API_BASE}/reports/${reportType}`);
        const data = await response.json();
        
        if (data.success) {
            document.getElementById('report-output').style.display = 'block';
            document.getElementById('report-content').textContent = JSON.stringify(data.data, null, 2);
            showToast('Report generated successfully', 'success');
        } else {
            showToast(data.message || 'Failed to generate report', 'error');
        }
    } catch (error) {
        console.error('Failed to generate report:', error);
        showToast('Failed to generate report', 'error');
    }
}

// Helper Functions
async function loadEmployeesForDropdown() {
    try {
        const response = await fetch(`${API_BASE}/employees`);
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('emp-department');
            // Already handled by loadDepartmentsForDropdown
        }
    } catch (error) {
        console.error('Failed to load employees:', error);
    }
}

async function loadDepartmentsForDropdown() {
    try {
        const response = await fetch(`${API_BASE}/departments`);
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('emp-department');
            const currentValue = select.value;
            select.innerHTML = '<option value="">Select Department</option>' +
                data.data.map(dept => `<option value="${dept.id}">${dept.name}</option>`).join('');
            select.value = currentValue;
        }
    } catch (error) {
        console.error('Failed to load departments:', error);
    }
}

async function loadEmployeesForAttendanceDropdown() {
    try {
        const response = await fetch(`${API_BASE}/employees`);
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('attendance-employee');
            const currentValue = select.value;
            select.innerHTML = '<option value="">Select Employee</option>' +
                data.data.map(emp => `<option value="${emp.id}">${emp.fullName} (${emp.employeeId})</option>`).join('');
            select.value = currentValue;
        }
    } catch (error) {
        console.error('Failed to load employees:', error);
    }
}

async function loadEmployeesForLeaveDropdown() {
    try {
        const response = await fetch(`${API_BASE}/employees`);
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('leave-employee');
            const currentValue = select.value;
            select.innerHTML = '<option value="">Select Employee</option>' +
                data.data.map(emp => `<option value="${emp.id}">${emp.fullName} (${emp.employeeId})</option>`).join('');
            select.value = currentValue;
        }
    } catch (error) {
        console.error('Failed to load employees:', error);
    }
}

async function loadEmployeesForPayrollDropdown() {
    try {
        const response = await fetch(`${API_BASE}/employees`);
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('payroll-employee');
            const currentValue = select.value;
            select.innerHTML = '<option value="">Select Employee</option>' +
                data.data.map(emp => `<option value="${emp.id}">${emp.fullName} (${emp.employeeId})</option>`).join('');
            select.value = currentValue;
        }
    } catch (error) {
        console.error('Failed to load employees:', error);
    }
}

