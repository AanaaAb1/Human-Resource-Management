#!/bin/bash

# HRM System - Database Setup Script for PostgreSQL
# This script creates the database, user, and runs the schema

# Configuration - Update these values for your environment
DB_NAME="hrm_system"
DB_USER="postgres"
DB_PASSWORD="password"
DB_HOST="localhost"
DB_PORT="5432"

echo "======================================"
echo "HRM System - PostgreSQL Database Setup"
echo "======================================"

# Check if psql is installed
if ! command -v psql &> /dev/null; then
    echo "Error: psql command not found. Please install PostgreSQL client."
    exit 1
fi

# Function to execute SQL
execute_sql() {
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" "$@"
}

# Check database connection
echo "Checking database connection..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "SELECT 1;" > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "Error: Cannot connect to PostgreSQL. Please check your credentials."
    echo "Current settings:"
    echo "  Host: $DB_HOST"
    echo "  Port: $DB_PORT"
    echo "  User: $DB_USER"
    echo ""
    echo "You can update the settings in this script or set environment variables:"
    echo "  export DB_PASSWORD='your_password'"
    exit 1
fi

echo "✓ Connected to PostgreSQL"

# Check if database exists
DB_EXISTS=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME';")

if [ "$DB_EXISTS" = "1" ]; then
    echo "Database '$DB_NAME' already exists."
    read -p "Do you want to drop and recreate it? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Dropping existing database..."
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;"
        echo "Creating database..."
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;"
        echo "✓ Database recreated"
    fi
else
    echo "Creating database '$DB_NAME'..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;"
    echo "✓ Database created"
fi

# Run the schema
echo ""
echo "Running database schema..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_FILE="$SCRIPT_DIR/database/schema-postgres.sql"

if [ ! -f "$SCHEMA_FILE" ]; then
    echo "Error: Schema file not found at $SCHEMA_FILE"
    exit 1
fi

PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCHEMA_FILE"

if [ $? -eq 0 ]; then
    echo "✓ Schema executed successfully"
else
    echo "Error: Failed to execute schema"
    exit 1
fi

# Show summary
echo ""
echo "======================================"
echo "Database Setup Complete!"
echo "======================================"
echo ""
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo ""
echo "Connection URL:"
echo "  jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
echo ""
echo "To connect via command line:"
echo "  psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
echo ""
echo "Default login credentials:"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo "Next steps:"
echo "  1. Update database.properties if needed"
echo "  2. Build the project: mvn clean package"
echo "  3. Deploy to Tomcat"
echo ""

