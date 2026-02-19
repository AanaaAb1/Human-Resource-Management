<<<<<<< HEAD
# Human-Resource-Management
=======
# HRM System - Human Resource Management

A complete Servlet-based Human Resource Management System built with Java Servlets, JDBC, and **PostgreSQL**.

## Features

### Mandatory Modules (Implemented)
1. **Authentication & Authorization**
   - User login/logout with session management
   - Role-based access control (Admin, HR Manager, Employee)
   - Password hashing with BCrypt

2. **Employee Management**
   - Create, read, update, delete employee records
   - Employee profiles with personal details
   - Department assignment

3. **Input Validation & Error Handling**
   - Comprehensive input validation
   - Proper HTTP status codes (400, 401, 403, 500)

4. **API Endpoints**
   - RESTful API design
   - JSON responses

### Optional Modules (Implemented)
5. **Department Management** - Create, update, delete departments
6. **Attendance Management** - Record and view attendance
7. **Leave Management** - Apply for leave, approve/reject requests
8. **Payroll Management** - Generate payroll records
9. **Reports** - Employee, attendance, leave, payroll reports
12. **Logging & Auditing** - Track all system activities

## Technology Stack

- Java Servlets
- JDBC
- **PostgreSQL** (Primary Database)
- Apache Tomcat
- Maven
- HTML/CSS/JavaScript (Frontend)

## Project Structure

```
HRM/
├── src/main/java/com/hrm/
│   ├── config/          # Database configuration
│   ├── dao/             # Data Access Objects
│   ├── model/           # Entity classes
│   ├── service/         # Business logic
│   ├── servlet/         # Servlet controllers
│   ├── filter/          # Authentication/Authorization filters
│   └── util/            # Utility classes
├── src/main/webapp/
│   ├── WEB-INF/web.xml  # Deployment descriptor
│   ├── index.html       # Frontend UI
│   ├── css/style.css    # Styles
│   └── js/app.js        # Frontend logic
├── src/main/resources/
│   └── database.properties
├── database/
│   ├── schema.sql           # MySQL schema (legacy)
│   └── schema-postgres.sql  # PostgreSQL schema
├── pom.xml              # Maven configuration
└── README.md            # This file
```

## PostgreSQL Setup

### 1. Install PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

**Windows:**
Download from https://www.postgresql.org/download/windows/

**macOS:**
```bash
brew install postgresql
```

### 2. Create Database and User

```bash
# Switch to postgres user
sudo -i -u postgres

# Enter PostgreSQL command line
psql

# Create database and user
CREATE DATABASE hrm_system;
CREATE USER your_username WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE hrm_system TO your_username;
GRANT ALL ON SCHEMA public TO your_username;

# Exit
\q
```

### 3. Run Database Schema

```bash
# Run the PostgreSQL schema
psql -U your_username -d hrm_system -f database/schema-postgres.sql

# Or using connection string
psql "postgresql://your_username:your_password@localhost:5432/hrm_system" -f database/schema-postgres.sql
```

### 4. Configure Database Connection

Set environment variables or edit `src/main/resources/database.properties`:

```bash
# Environment variables (recommended for production)
export DB_URL="jdbc:postgresql://localhost:5432/hrm_system"
export DB_USER="your_username"
export DB_PASSWORD="your_password"
```

Or edit the properties file:
```properties
db.url=jdbc:postgresql://localhost:5432/hrm_system
db.username=postgres
db.password=password
```

### 5. Build the Project

```bash
cd /home/aneman/Desktop/HRM
mvn clean package
```

### 6. Deploy to Tomcat

Copy the WAR file to Tomcat's webapps directory:
```bash
cp target/hrm-system.war $CATALINA_HOME/webapps/
```

Or use Maven Tomcat plugin:
```bash
mvn tomcat7:deploy
```

### 7. Access the Application

Open browser and navigate to:
```
http://localhost:8080/hrm-system/
```

## Default Credentials

- **Username:** admin
- **Password:** admin123

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/login` - Check auth status

### Employees
- `GET /api/employees` - Get all employees
- `GET /api/employees/{id}` - Get employee by ID
- `POST /api/employees` - Create employee
- `PUT /api/employees/{id}` - Update employee
- `DELETE /api/employees/{id}` - Delete employee

### Departments
- `GET /api/departments` - Get all departments
- `POST /api/departments` - Create department
- `PUT /api/departments/{id}` - Update department
- `DELETE /api/departments/{id}` - Delete department

### Attendance
- `GET /api/attendance` - Get all attendance records
- `POST /api/attendance` - Record attendance
- `PUT /api/attendance/{id}` - Update attendance
- `DELETE /api/attendance/{id}` - Delete attendance

### Leave
- `GET /api/leave` - Get leave requests
- `POST /api/leave` - Apply for leave
- `PUT /api/leave/{id}/approve` - Approve leave
- `PUT /api/leave/{id}/reject` - Reject leave
- `DELETE /api/leave/{id}` - Delete leave request

### Payroll
- `GET /api/payroll` - Get payroll records
- `POST /api/payroll/generate` - Generate payroll
- `PUT /api/payroll/{id}` - Update payroll
- `DELETE /api/payroll/{id}` - Delete payroll

### Reports
- `GET /api/reports/summary` - System summary
- `GET /api/reports/employees` - Employee report
- `GET /api/reports/attendance` - Attendance report
- `GET /api/reports/leave` - Leave report
- `GET /api/reports/payroll` - Payroll report

## User Roles

| Role | Permissions |
|------|-------------|
| **Admin** | Full access to all features |
| **HR Manager** | Manage employees, departments, attendance, leave, payroll |
| **Employee** | View own profile, attendance, leave requests, payroll |

## Database Schema

### Tables
- `users` - System users
- `employees` - Employee records
- `departments` - Department records
- `attendance` - Daily attendance
- `leave_requests` - Leave applications
- `payroll` - Payroll records
- `audit_logs` - System audit trail

### Key PostgreSQL Features Used
- SERIAL for auto-increment
- CHECK constraints for data validation
- FOREIGN KEY constraints for referential integrity
- UNIQUE constraints
- TIMESTAMP with timezone support
- TRIGGER for updated_at auto-update
- INDEX for performance optimization

## Development

### Run with embedded Tomcat
```bash
mvn tomcat7:run
```

### Access development server
```
http://localhost:8080
```

### Run Database Schema Only (for testing)
```bash
psql -U postgres -d hrm_system -c "DROP DATABASE IF EXISTS hrm_system;"
psql -U postgres -c "CREATE DATABASE hrm_system;"
psql -U postgres -d hrm_system -f database/schema-postgres.sql
```

## Testing

Run unit tests:
```bash
mvn test
```

## Building for Production

```bash
mvn clean package -DskipTests
```

The WAR file will be created in `target/hrm-system.war`.

## Troubleshooting

### Connection Refused
- Check PostgreSQL is running: `sudo systemctl status postgresql`
- Verify port 5432 is open: `pg_isready -h localhost -p 5432`
- Check firewall settings

### Authentication Failed
- Verify username/password in database.properties
- Ensure user has proper permissions on the database

### SSL Connections
- For production, enable SSL in database.properties:
```properties
db.ssl.enabled=true
db.ssl.mode=require
```

## License

This project is for educational purposes.

>>>>>>> 294eeb2 (Initial commit)
