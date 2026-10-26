package payroll;

import java.math.BigDecimal;

/**
 * Helper class: one row of the payroll table (plus the employee's name from a JOIN).
 * The payroll table has no "monthly salary" column, so we work it out:
 * monthly salary = net salary + deduction (always exactly true).
 */
public class PayrollRecord {

    private final int payrollId;
    private final int employeeId;
    private final String employeeName;
    private final String payrollMonth; // YYYY-MM
    private final int totalWorkingDays;
    private final int presentDays;
    private final int absentDays;
    private final BigDecimal deduction;
    private final BigDecimal netSalary;

    public PayrollRecord(int payrollId, int employeeId, String employeeName, String payrollMonth,
                         int totalWorkingDays, int presentDays, int absentDays,
                         BigDecimal deduction, BigDecimal netSalary) {
        this.payrollId = payrollId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.payrollMonth = payrollMonth;
        this.totalWorkingDays = totalWorkingDays;
        this.presentDays = presentDays;
        this.absentDays = absentDays;
        this.deduction = deduction;
        this.netSalary = netSalary;
    }

    public int getPayrollId() { return payrollId; }
    public int getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getPayrollMonth() { return payrollMonth; }
    public int getTotalWorkingDays() { return totalWorkingDays; }
    public int getPresentDays() { return presentDays; }
    public int getAbsentDays() { return absentDays; }
    public BigDecimal getDeduction() { return deduction; }
    public BigDecimal getNetSalary() { return netSalary; }

    public BigDecimal getMonthlySalary() {
        return netSalary.add(deduction);
    }
}
