-- =====================================================================
-- Employee Attendance and Payroll Tracker - database script
-- Run this whole file in MySQL Workbench (lightning-bolt "Execute" button).
-- It is safe to run more than once: nothing is dropped or duplicated.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS payroll_db;
USE payroll_db;

-- ---------------------------------------------------------------------
-- Table 1: employees
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employees (
    employee_id    INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    department     VARCHAR(100)  NOT NULL,
    designation    VARCHAR(100)  NOT NULL,
    monthly_salary DECIMAL(10,2) NOT NULL
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Table 2: attendance
-- UNIQUE (employee_id, attendance_date) = one record per employee per day.
-- ON DELETE RESTRICT = an employee with attendance cannot be deleted.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance (
    attendance_id   INT AUTO_INCREMENT PRIMARY KEY,
    employee_id     INT NOT NULL,
    attendance_date DATE NOT NULL,
    status          ENUM('Present','Absent') NOT NULL,
    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id) REFERENCES employees (employee_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_attendance_employee_date
        UNIQUE (employee_id, attendance_date)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Table 3: payroll
-- payroll_month is text in YYYY-MM format, for example '2026-09'.
-- UNIQUE (employee_id, payroll_month) = one payroll per employee per month.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payroll (
    payroll_id         INT AUTO_INCREMENT PRIMARY KEY,
    employee_id        INT NOT NULL,
    payroll_month      CHAR(7) NOT NULL,
    total_working_days INT NOT NULL,
    present_days       INT NOT NULL,
    absent_days        INT NOT NULL,
    deduction          DECIMAL(10,2) NOT NULL,
    net_salary         DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_payroll_employee
        FOREIGN KEY (employee_id) REFERENCES employees (employee_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_payroll_employee_month
        UNIQUE (employee_id, payroll_month)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Sample employees (each one is inserted only if that name is not there yet)
-- ---------------------------------------------------------------------
INSERT INTO employees (name, department, designation, monthly_salary)
SELECT 'Aarav Sharma', 'Engineering', 'Software Engineer', 60000.00 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE name = 'Aarav Sharma');

INSERT INTO employees (name, department, designation, monthly_salary)
SELECT 'Priya Verma', 'Human Resources', 'HR Executive', 45000.00 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE name = 'Priya Verma');

INSERT INTO employees (name, department, designation, monthly_salary)
SELECT 'Rohan Mehta', 'Finance', 'Accountant', 52500.50 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE name = 'Rohan Mehta');

-- Quick check
SELECT * FROM employees;
