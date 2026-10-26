package payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the payroll table, plus the payroll calculation rules. */
public class PayrollDAO {

    private static final int ERROR_DUPLICATE_ENTRY = 1062;

    // ------------------------------------------------------------------
    // Calculation rules (no database needed, so they are static helpers)
    // ------------------------------------------------------------------

    /**
     * RULE: working days = calendar days in the month, excluding Sundays.
     * Example: September 2026 has 30 days and 4 Sundays -> 26 working days.
     */
    public static int calculateWorkingDays(YearMonth month) {
        int workingDays = 0;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            if (month.atDay(day).getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
        }
        return workingDays;
    }

    /**
     * Deduction = (Monthly Salary / Working Days) x Absent Days.
     * We multiply FIRST and divide ONCE at the end, then round to 2 decimals
     * with HALF_UP (0.005 rounds up to 0.01). This is mathematically the same
     * formula but avoids rounding the daily salary and then multiplying the error.
     */
    public static BigDecimal calculateDeduction(BigDecimal monthlySalary, int workingDays, int absentDays) {
        if (workingDays <= 0 || absentDays <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return monthlySalary
                .multiply(BigDecimal.valueOf(absentDays))
                .divide(BigDecimal.valueOf(workingDays), 2, RoundingMode.HALF_UP);
    }

    /** Daily salary, rounded to 2 decimals. Shown for information only; it is not stored. */
    public static BigDecimal calculateDailySalary(BigDecimal monthlySalary, int workingDays) {
        return monthlySalary.divide(BigDecimal.valueOf(workingDays), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // Database methods
    // ------------------------------------------------------------------

    /** True if payroll was already generated for this employee and month (e.g. "2026-09"). */
    public boolean payrollExists(int employeeId, String payrollMonth) throws SQLException {
        String sql = "SELECT 1 FROM payroll WHERE employee_id = ? AND payroll_month = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, payrollMonth);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Saves a payroll record. Returns false if the UNIQUE constraint says it already exists. */
    public boolean savePayroll(PayrollRecord record) throws SQLException {
        String sql = "INSERT INTO payroll (employee_id, payroll_month, total_working_days, "
                   + "present_days, absent_days, deduction, net_salary) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, record.getEmployeeId());
            ps.setString(2, record.getPayrollMonth());
            ps.setInt(3, record.getTotalWorkingDays());
            ps.setInt(4, record.getPresentDays());
            ps.setInt(5, record.getAbsentDays());
            ps.setBigDecimal(6, record.getDeduction());
            ps.setBigDecimal(7, record.getNetSalary());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == ERROR_DUPLICATE_ENTRY) {
                return false;
            }
            throw e;
        }
    }

    /** All payroll records of all employees. */
    public List<PayrollRecord> getAllPayroll() throws SQLException {
        String sql = "SELECT p.payroll_id, p.employee_id, e.name, p.payroll_month, "
                   + "p.total_working_days, p.present_days, p.absent_days, p.deduction, p.net_salary "
                   + "FROM payroll p JOIN employees e ON p.employee_id = e.employee_id "
                   + "ORDER BY p.payroll_month DESC, p.employee_id";
        List<PayrollRecord> records = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(mapRow(rs));
            }
        }
        return records;
    }

    /** Payroll history of one employee. */
    public List<PayrollRecord> getPayrollByEmployee(int employeeId) throws SQLException {
        String sql = "SELECT p.payroll_id, p.employee_id, e.name, p.payroll_month, "
                   + "p.total_working_days, p.present_days, p.absent_days, p.deduction, p.net_salary "
                   + "FROM payroll p JOIN employees e ON p.employee_id = e.employee_id "
                   + "WHERE p.employee_id = ? ORDER BY p.payroll_month DESC";
        List<PayrollRecord> records = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
            }
        }
        return records;
    }

    /** How many payroll rows belong to this employee (used to decide if deleting is allowed). */
    public int countForEmployee(int employeeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payroll WHERE employee_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private PayrollRecord mapRow(ResultSet rs) throws SQLException {
        return new PayrollRecord(
                rs.getInt("payroll_id"),
                rs.getInt("employee_id"),
                rs.getString("name"),
                rs.getString("payroll_month"),
                rs.getInt("total_working_days"),
                rs.getInt("present_days"),
                rs.getInt("absent_days"),
                rs.getBigDecimal("deduction"),
                rs.getBigDecimal("net_salary"));
    }
}
