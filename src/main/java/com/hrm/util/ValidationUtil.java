package com.hrm.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtil {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[0-9\\-\\s()]{7,20}$"
    );
    
    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile(
        "^[A-Za-z0-9\\-_]{2,20}$"
    );
    
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_ADDRESS_LENGTH = 500;
    private static final int MAX_REASON_LENGTH = 1000;
    
    /**
     * Validate email format
     * @param email Email to validate
     * @return true if valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validate phone number format
     * @param phone Phone number to validate
     * @return true if valid
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }
    
    /**
     * Validate employee ID format
     * @param employeeId Employee ID to validate
     * @return true if valid
     */
    public static boolean isValidEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return false;
        }
        return EMPLOYEE_ID_PATTERN.matcher(employeeId.trim()).matches();
    }
    
    /**
     * Validate date string
     * @param dateStr Date string to validate
     * @param formatter Date formatter to use
     * @return LocalDate if valid, null otherwise
     */
    public static LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Validate salary
     * @param salary Salary value
     * @return true if valid
     */
    public static boolean isValidSalary(BigDecimal salary) {
        return salary != null && salary.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Validate name length
     * @param name Name to validate
     * @return true if valid
     */
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.trim().length() <= MAX_NAME_LENGTH;
    }
    
    /**
     * Validate address length
     * @param address Address to validate
     * @return true if valid
     */
    public static boolean isValidAddress(String address) {
        return address == null || address.length() <= MAX_ADDRESS_LENGTH;
    }
    
    /**
     * Validate reason length
     * @param reason Reason to validate
     * @return true if valid
     */
    public static boolean isValidReason(String reason) {
        return reason == null || reason.length() <= MAX_REASON_LENGTH;
    }
    
    /**
     * Validate not null or empty
     * @param value Value to check
     * @return true if not null and not empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validate positive integer
     * @param value Value to check
     * @return true if positive
     */
    public static boolean isPositiveInteger(Integer value) {
        return value != null && value > 0;
    }
    
    /**
     * Validate month (1-12)
     * @param month Month to validate
     * @return true if valid
     */
    public static boolean isValidMonth(Integer month) {
        return month != null && month >= 1 && month <= 12;
    }
    
    /**
     * Validate year (reasonable range)
     * @param year Year to validate
     * @return true if valid
     */
    public static boolean isValidYear(Integer year) {
        return year != null && year >= 2000 && year <= 2100;
    }
    
    /**
     * Sanitize string input
     * @param input Input string
     * @return Sanitized string
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.trim()
            .replaceAll("[\r\n]", "")
            .replaceAll("\\s+", " ");
    }
    
    /**
     * Validate date range
     * @param startDate Start date
     * @param endDate End date
     * @return true if valid range
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }
    
    /**
     * Validate future date
     * @param date Date to validate
     * @return true if date is today or in the future
     */
    public static boolean isFutureOrToday(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(LocalDate.now());
    }
}

