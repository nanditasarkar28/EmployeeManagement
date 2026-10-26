package payroll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the attendance table. */
public class AttendanceDAO {

    // MySQL error code for "duplicate entry" (UNIQUE constraint violated).
    private static final int ERROR_DUPLICATE_ENTRY = 1062;

    /** True if this employee already has a record for this date. */
    public boolean attendanceExists(int employeeId, LocalDate date) throws SQLException {
        String sql = "SELECT 1 FROM attendance WHERE employee_id = ? AND attendance_date = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setObject(2, date); // LocalDate -> SQL DATE
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Saves one attendance record. status must be "Present" or "Absent".
     * Returns false (instead of crashing) if the UNIQUE constraint rejects a duplicate.
     */
    public boolean markAttendance(int employeeId, LocalDate date, String status) throws SQLException {
        String sql = "INSERT INTO attendance (employee_id, attendance_date, status) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setObject(2, date);
            ps.setString(3, status);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == ERROR_DUPLICATE_ENTRY) {
                return false;
            }
            throw e; // any other database problem is reported by Main
        }
    }

    /** Every attendance record of every employee, newest date first. */
    public List<AttendanceRecord> getAllAttendance() throws SQLException {
        String sql = "SELECT a.attendance_id, a.employee_id, e.name, a.attendance_date, a.status "
                   + "FROM attendance a JOIN employees e ON a.employee_id = e.employee_id "
                   + "ORDER BY a.attendance_date DESC, a.employee_id";
        List<AttendanceRecord> records = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(mapRow(rs));
            }
        }
        return records;
    }

    /** One employee's records for one month, oldest date first. */
    public List<AttendanceRecord> getMonthlyAttendance(int employeeId, YearMonth month)
            throws SQLException {
        String sql = "SELECT a.attendance_id, a.employee_id, e.name, a.attendance_date, a.status "
                   + "FROM attendance a JOIN employees e ON a.employee_id = e.employee_id "
                   + "WHERE a.employee_id = ? AND a.attendance_date >= ? AND a.attendance_date < ? "
                   + "ORDER BY a.attendance_date";
        List<AttendanceRecord> records = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setObject(2, month.atDay(1));                  // first day of the month
            ps.setObject(3, month.plusMonths(1).atDay(1));    // first day of NEXT month (excluded)
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
            }
        }
        return records;
    }

    /** Counts this employee's "Present" or "Absent" records in a month. */
    public int countByStatus(int employeeId, YearMonth month, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance "
                   + "WHERE employee_id = ? AND status = ? "
                   + "AND attendance_date >= ? AND attendance_date < ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setString(2, status);
            ps.setObject(3, month.atDay(1));
            ps.setObject(4, month.plusMonths(1).atDay(1));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); // COUNT(*) always returns exactly one row
                return rs.getInt(1);
            }
        }
    }

    /** How many attendance rows belong to this employee (used to decide if deleting is allowed). */
    public int countForEmployee(int employeeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE employee_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private AttendanceRecord mapRow(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
                rs.getInt("attendance_id"),
                rs.getInt("employee_id"),
                rs.getString("name"),
                rs.getObject("attendance_date", LocalDate.class),
                rs.getString("status"));
    }
}
