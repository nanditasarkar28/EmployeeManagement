package payroll;

import java.time.LocalDate;

/**
 * Helper class: one row of the attendance table (plus the employee's name from a JOIN).
 * It lets AttendanceDAO RETURN data instead of printing it, so printing stays in Main.
 */
public class AttendanceRecord {

    private final int attendanceId;
    private final int employeeId;
    private final String employeeName;
    private final LocalDate date;
    private final String status;

    public AttendanceRecord(int attendanceId, int employeeId, String employeeName,
                            LocalDate date, String status) {
        this.attendanceId = attendanceId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.status = status;
    }

    public int getAttendanceId() { return attendanceId; }
    public int getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public LocalDate getDate() { return date; }
    public String getStatus() { return status; }
}
