package payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Console menu for the Employee Attendance and Payroll Tracker.
 *
 * INPUT STRATEGY (prevents skipped prompts):
 *   - There is exactly ONE Scanner for the whole program.
 *   - We ALWAYS read a full line with nextLine() and convert it ourselves.
 *   - We never use nextInt()/nextDouble(), because they leave the Enter key
 *     in the buffer and make the next nextLine() return an empty string.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final EmployeeDAO employeeDAO = new EmployeeDAO();
    private static final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private static final PayrollDAO payrollDAO = new PayrollDAO();

    // The biggest value a DECIMAL(10,2) column can hold.
    private static final BigDecimal MAX_SALARY = new BigDecimal("99999999.99");

    public static void main(String[] args) {
        System.out.println("=== Employee Attendance and Payroll Tracker ===");

        if (!checkDatabaseConnection()) {
            return; // the reason has already been printed
        }

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readMenuChoice();

            try {
                switch (choice) {
                    case 1:
                        addEmployee();
                        break;
                    case 2:
                        viewAllEmployees();
                        break;
                    case 3:
                        searchEmployee();
                        break;
                    case 4:
                        updateEmployee();
                        break;
                    case 5:
                        deleteEmployee();
                        break;
                    case 6:
                        markAttendance();
                        break;
                    case 7:
                        viewAttendance();
                        break;
                    case 8:
                        calculatePayroll();
                        break;
                    case 9:
                        viewPayrollHistory();
                        break;
                    case 10:
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number from 1 to 10.");
                }
            } catch (SQLException e) {
                printDatabaseError(e);
            }
        }
        scanner.close();
    }

    // =====================================================================
    // Start-up check and menu
    // =====================================================================

    private static boolean checkDatabaseConnection() {
        try (Connection con = DBConnection.getConnection()) {
            if (con.isValid(5)) {
                System.out.println("Connected to MySQL database successfully.");
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Could not connect to the database.");
            printDatabaseError(e);
            return false;
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("========== MAIN MENU ==========");
        System.out.println(" 1. Add Employee");
        System.out.println(" 2. View All Employees");
        System.out.println(" 3. Search Employee");
        System.out.println(" 4. Update Employee");
        System.out.println(" 5. Delete Employee");
        System.out.println(" 6. Mark Attendance");
        System.out.println(" 7. View Attendance");
        System.out.println(" 8. Calculate Monthly Payroll");
        System.out.println(" 9. View Payroll History");
        System.out.println("10. Exit");
        System.out.println("===============================");
    }

    /** Reads the menu choice. Returns -1 if the text is not a whole number. */
    private static int readMenuChoice() {
        String line = readLine("Enter your choice (1-10): ");
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // =====================================================================
    // Employee features
    // =====================================================================

    private static void addEmployee() throws SQLException {
        System.out.println("\n--- Add Employee ---");
        String name = readText("Name: ", 100, false);
        String department = readText("Department: ", 100, false);
        String designation = readText("Designation: ", 100, false);
        BigDecimal salary = readSalary("Monthly salary: ", false);

        int newId = employeeDAO.addEmployee(new Employee(name, department, designation, salary));
        System.out.println("Employee added successfully. New employee ID: " + newId);
    }

    private static void viewAllEmployees() throws SQLException {
        System.out.println("\n--- All Employees ---");
        List<Employee> employees = employeeDAO.getAllEmployees();
        if (employees.isEmpty()) {
            System.out.println("No employees found.");
            return;
        }
        System.out.printf("%-5s %-22s %-18s %-22s %12s%n",
                "ID", "Name", "Department", "Designation", "Salary");
        System.out.println("-".repeat(83));
        for (Employee e : employees) {
            System.out.printf("%-5d %-22.22s %-18.18s %-22.22s %12s%n",
                    e.getEmployeeId(), e.getName(), e.getDepartment(), e.getDesignation(),
                    e.getMonthlySalary().toPlainString());
        }
        System.out.println("Total employees: " + employees.size());
    }

    private static void searchEmployee() throws SQLException {
        System.out.println("\n--- Search Employee ---");
        Employee e = askForExistingEmployee();
        if (e != null) {
            printEmployeeDetails(e);
        }
    }

    private static void updateEmployee() throws SQLException {
        System.out.println("\n--- Update Employee ---");
        Employee e = askForExistingEmployee();
        if (e == null) {
            return;
        }
        printEmployeeDetails(e);
        System.out.println("Press Enter on a question to keep the current value.");

        String name = readText("New name [" + e.getName() + "]: ", 100, true);
        String department = readText("New department [" + e.getDepartment() + "]: ", 100, true);
        String designation = readText("New designation [" + e.getDesignation() + "]: ", 100, true);
        BigDecimal salary = readSalary("New monthly salary [" + e.getMonthlySalary().toPlainString() + "]: ", true);

        if (name == null && department == null && designation == null && salary == null) {
            System.out.println("Nothing was changed.");
            return;
        }
        if (name != null) e.setName(name);
        if (department != null) e.setDepartment(department);
        if (designation != null) e.setDesignation(designation);
        if (salary != null) e.setMonthlySalary(salary);

        if (employeeDAO.updateEmployee(e)) {
            System.out.println("Employee updated successfully.");
            System.out.println("(Payroll that was already generated is NOT recalculated.)");
        } else {
            System.out.println("Update failed: the employee no longer exists.");
        }
    }

    private static void deleteEmployee() throws SQLException {
        System.out.println("\n--- Delete Employee ---");
        Employee e = askForExistingEmployee();
        if (e == null) {
            return;
        }
        printEmployeeDetails(e);

        // Deletion is BLOCKED while attendance or payroll records exist. Deleting the
        // employee would otherwise destroy salary history, and MySQL's foreign key refuses it anyway.
        int attendanceCount = attendanceDAO.countForEmployee(e.getEmployeeId());
        int payrollCount = payrollDAO.countForEmployee(e.getEmployeeId());
        if (attendanceCount > 0 || payrollCount > 0) {
            System.out.println("Cannot delete this employee. They still have "
                    + attendanceCount + " attendance record(s) and "
                    + payrollCount + " payroll record(s).");
            System.out.println("Only employees with no attendance and no payroll records can be deleted.");
            return;
        }

        if (!readYesNo("Are you sure you want to delete this employee? (y/n): ")) {
            System.out.println("Delete cancelled.");
            return;
        }
        if (employeeDAO.deleteEmployee(e.getEmployeeId())) {
            System.out.println("Employee deleted successfully.");
        } else {
            System.out.println("Nothing deleted: the employee no longer exists.");
        }
    }

    private static void printEmployeeDetails(Employee e) {
        System.out.println("Employee ID    : " + e.getEmployeeId());
        System.out.println("Name           : " + e.getName());
        System.out.println("Department     : " + e.getDepartment());
        System.out.println("Designation    : " + e.getDesignation());
        System.out.println("Monthly salary : " + e.getMonthlySalary().toPlainString());
    }

    // =====================================================================
    // Attendance features
    // =====================================================================

    private static void markAttendance() throws SQLException {
        System.out.println("\n--- Mark Attendance ---");
        Employee e = askForExistingEmployee();
        if (e == null) {
            return;
        }
        System.out.println("Employee: " + e.getName());

        LocalDate date = readDate("Date (YYYY-MM-DD, or type 'today'): ");
        if (date.isAfter(LocalDate.now())) {
            System.out.println("You cannot mark attendance for a future date.");
            return;
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            System.out.println("Sundays are not working days, so attendance cannot be marked on them.");
            return;
        }
        String status = readStatus("Status (P = Present, A = Absent): ");

        if (attendanceDAO.attendanceExists(e.getEmployeeId(), date)) {
            System.out.println("Attendance for this employee on " + date + " is already recorded.");
            return;
        }
        if (attendanceDAO.markAttendance(e.getEmployeeId(), date, status)) {
            System.out.println("Attendance saved: " + e.getName() + " - " + date + " - " + status);
        } else {
            // Rare: another program inserted the same row between our check and our insert.
            System.out.println("Attendance for this employee on " + date + " is already recorded.");
        }
    }

    private static void viewAttendance() throws SQLException {
        System.out.println("\n--- View Attendance ---");
        System.out.println("1. All attendance records");
        System.out.println("2. Monthly attendance for one employee");
        String option = readLine("Choose 1 or 2: ");

        if (option.equals("1")) {
            List<AttendanceRecord> records = attendanceDAO.getAllAttendance();
            printAttendanceTable(records);
        } else if (option.equals("2")) {
            Employee e = askForExistingEmployee();
            if (e == null) {
                return;
            }
            YearMonth month = readYearMonth("Month (YYYY-MM): ");
            List<AttendanceRecord> records = attendanceDAO.getMonthlyAttendance(e.getEmployeeId(), month);
            System.out.println("Attendance of " + e.getName() + " for " + month);
            printAttendanceTable(records);

            int present = attendanceDAO.countByStatus(e.getEmployeeId(), month, "Present");
            int absent = attendanceDAO.countByStatus(e.getEmployeeId(), month, "Absent");
            int working = PayrollDAO.calculateWorkingDays(month);
            System.out.println("Working days (excluding Sundays): " + working);
            System.out.println("Present: " + present + "   Absent: " + absent
                    + "   Not marked: " + (working - present - absent));
        } else {
            System.out.println("Invalid option. Please choose 1 or 2.");
        }
    }

    private static void printAttendanceTable(List<AttendanceRecord> records) {
        if (records.isEmpty()) {
            System.out.println("No attendance records found.");
            return;
        }
        System.out.printf("%-6s %-5s %-22s %-12s %-8s%n", "ID", "EmpID", "Name", "Date", "Status");
        System.out.println("-".repeat(58));
        for (AttendanceRecord r : records) {
            System.out.printf("%-6d %-5d %-22.22s %-12s %-8s%n",
                    r.getAttendanceId(), r.getEmployeeId(), r.getEmployeeName(),
                    r.getDate(), r.getStatus());
        }
        System.out.println("Records shown: " + records.size());
    }

    // =====================================================================
    // Payroll features
    // =====================================================================

    private static void calculatePayroll() throws SQLException {
        System.out.println("\n--- Calculate Monthly Payroll ---");
        Employee e = askForExistingEmployee();
        if (e == null) {
            return;
        }
        YearMonth month = readYearMonth("Payroll month (YYYY-MM): ");
        if (month.isAfter(YearMonth.now())) {
            System.out.println("You cannot generate payroll for a future month.");
            return;
        }
        String monthText = month.toString(); // always "YYYY-MM", e.g. 2026-09

        if (payrollDAO.payrollExists(e.getEmployeeId(), monthText)) {
            System.out.println("Payroll for " + e.getName() + " for " + monthText
                    + " has already been generated. Use option 9 to view it.");
            return;
        }

        int workingDays = PayrollDAO.calculateWorkingDays(month);
        int presentDays = attendanceDAO.countByStatus(e.getEmployeeId(), month, "Present");
        int absentDays = attendanceDAO.countByStatus(e.getEmployeeId(), month, "Absent");
        int notMarked = workingDays - presentDays - absentDays;

        if (notMarked < 0) {
            System.out.println("The attendance data for this month looks wrong (more records than working days).");
            System.out.println("Attendance may have been added on a Sunday outside this program. Please fix it first.");
            return;
        }
        if (month.equals(YearMonth.now())) {
            System.out.println("Note: this month is not over yet.");
        }
        if (notMarked > 0) {
            System.out.println("Warning: " + notMarked + " working day(s) have NO attendance record.");
            System.out.println("Only days marked Absent are deducted; unmarked days are NOT deducted.");
            if (!readYesNo("Continue and generate payroll anyway? (y/n): ")) {
                System.out.println("Payroll cancelled.");
                return;
            }
        }

        BigDecimal monthlySalary = e.getMonthlySalary();
        BigDecimal dailySalary = PayrollDAO.calculateDailySalary(monthlySalary, workingDays);
        BigDecimal deduction = PayrollDAO.calculateDeduction(monthlySalary, workingDays, absentDays);
        BigDecimal netSalary = monthlySalary.subtract(deduction);

        System.out.println();
        System.out.println("----- Payroll: " + e.getName() + " (ID " + e.getEmployeeId() + ") - " + monthText + " -----");
        System.out.println("Monthly salary     : " + monthlySalary.toPlainString());
        System.out.println("Total working days : " + workingDays);
        System.out.println("Present days       : " + presentDays);
        System.out.println("Absent days        : " + absentDays);
        System.out.println("Daily salary       : " + dailySalary.toPlainString());
        System.out.println("Deduction          : " + deduction.toPlainString());
        System.out.println("Net salary         : " + netSalary.toPlainString());

        PayrollRecord record = new PayrollRecord(0, e.getEmployeeId(), e.getName(), monthText,
                workingDays, presentDays, absentDays, deduction, netSalary);
        if (payrollDAO.savePayroll(record)) {
            System.out.println("Payroll record saved.");
        } else {
            System.out.println("Payroll for this employee and month already exists. Nothing was saved.");
        }
    }

    private static void viewPayrollHistory() throws SQLException {
        System.out.println("\n--- Payroll History ---");
        System.out.println("1. All payroll records");
        System.out.println("2. Payroll history of one employee");
        String option = readLine("Choose 1 or 2: ");

        List<PayrollRecord> records;
        if (option.equals("1")) {
            records = payrollDAO.getAllPayroll();
        } else if (option.equals("2")) {
            Employee e = askForExistingEmployee();
            if (e == null) {
                return;
            }
            records = payrollDAO.getPayrollByEmployee(e.getEmployeeId());
        } else {
            System.out.println("Invalid option. Please choose 1 or 2.");
            return;
        }

        if (records.isEmpty()) {
            System.out.println("No payroll records found.");
            return;
        }
        System.out.printf("%-4s %-5s %-16s %-8s %5s %5s %5s %11s %11s %11s%n",
                "ID", "EmpID", "Name", "Month", "Work", "Pres", "Abs", "Salary", "Deduction", "Net");
        System.out.println("-".repeat(93));
        for (PayrollRecord r : records) {
            System.out.printf("%-4d %-5d %-16.16s %-8s %5d %5d %5d %11s %11s %11s%n",
                    r.getPayrollId(), r.getEmployeeId(), r.getEmployeeName(), r.getPayrollMonth(),
                    r.getTotalWorkingDays(), r.getPresentDays(), r.getAbsentDays(),
                    r.getMonthlySalary().toPlainString(), r.getDeduction().toPlainString(),
                    r.getNetSalary().toPlainString());
        }
        System.out.println("Records shown: " + records.size());
    }

    // =====================================================================
    // Input helpers - each one keeps asking until the input is valid
    // =====================================================================

    /** Prints the prompt and returns one trimmed line. Exits cleanly if the input has ended. */
    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.out.println("No more input. Exiting.");
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    /**
     * Reads text of 1..maxLength characters.
     * If allowBlank is true, pressing Enter returns null (meaning "keep the old value").
     */
    private static String readText(String prompt, int maxLength, boolean allowBlank) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                if (allowBlank) {
                    return null;
                }
                System.out.println("This field cannot be empty.");
            } else if (text.length() > maxLength) {
                System.out.println("Too long. Please use at most " + maxLength + " characters.");
            } else {
                return text;
            }
        }
    }

    /** Reads a whole number that is 1 or more. */
    private static int readPositiveInt(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= 1) {
                    return value;
                }
                System.out.println("Please enter a number greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter digits only, for example 3.");
            }
        }
    }

    /**
     * Reads a salary: greater than 0, at most 2 decimal places, at most 99999999.99.
     * If allowBlank is true, pressing Enter returns null.
     */
    private static BigDecimal readSalary(String prompt, boolean allowBlank) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty() && allowBlank) {
                return null;
            }
            try {
                BigDecimal value = new BigDecimal(text);
                if (value.signum() <= 0) {
                    System.out.println("Salary must be greater than 0.");
                } else if (value.stripTrailingZeros().scale() > 2) {
                    System.out.println("Use at most 2 decimal places, for example 45000.50");
                } else if (value.compareTo(MAX_SALARY) > 0) {
                    System.out.println("Salary is too large. The maximum is " + MAX_SALARY.toPlainString());
                } else {
                    return value.setScale(2, RoundingMode.UNNECESSARY);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Example: 45000 or 45000.50");
            }
        }
    }

    /** Reads a real calendar date in YYYY-MM-DD form (or the word "today"). */
    private static LocalDate readDate(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.equalsIgnoreCase("today")) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(text); // strict: rejects 2026-02-30
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use YYYY-MM-DD, for example 2026-09-15.");
            }
        }
    }

    /** Reads a month in YYYY-MM form. */
    private static YearMonth readYearMonth(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return YearMonth.parse(text);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid month. Use YYYY-MM, for example 2026-09.");
            }
        }
    }

    /** Reads P/Present or A/Absent and returns exactly "Present" or "Absent". */
    private static String readStatus(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.equalsIgnoreCase("P") || text.equalsIgnoreCase("Present")) {
                return "Present";
            }
            if (text.equalsIgnoreCase("A") || text.equalsIgnoreCase("Absent")) {
                return "Absent";
            }
            System.out.println("Please type P for Present or A for Absent.");
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.equalsIgnoreCase("y") || text.equalsIgnoreCase("yes")) {
                return true;
            }
            if (text.equalsIgnoreCase("n") || text.equalsIgnoreCase("no")) {
                return false;
            }
            System.out.println("Please type y or n.");
        }
    }

    /**
     * Asks for an employee ID and loads that employee from the database.
     * Returns null (after printing a message) if no such employee exists.
     */
    private static Employee askForExistingEmployee() throws SQLException {
        int id = readPositiveInt("Employee ID: ");
        Employee e = employeeDAO.getEmployeeById(id);
        if (e == null) {
            System.out.println("No employee found with ID " + id + ".");
        }
        return e;
    }

    // =====================================================================
    // Error messages
    // =====================================================================

    /** Turns a technical SQLException into a message a beginner can act on. */
    private static void printDatabaseError(SQLException e) {
        String state = e.getSQLState();
        switch (e.getErrorCode()) {
            case 1045:
                System.out.println("Database error: access denied. The MySQL username or password in DBConnection.java is wrong.");
                break;
            case 1049:
                System.out.println("Database error: 'payroll_db' does not exist. Run database.sql in MySQL Workbench first.");
                break;
            case 1146:
                System.out.println("Database error: a table is missing. Run database.sql in MySQL Workbench first.");
                break;
            case 1451:
                System.out.println("Database error: this record is still used by other records, so it cannot be deleted.");
                break;
            case 1452:
                System.out.println("Database error: the employee for this record does not exist.");
                break;
            case 1062:
                System.out.println("Database error: this record already exists (duplicate).");
                break;
            default:
                if (state != null && state.startsWith("08")) {
                    System.out.println("Database error: cannot reach MySQL. Is the MySQL80 service running on localhost:3306?");
                } else {
                    System.out.println("Database error: " + e.getMessage());
                }
        }
    }
}
