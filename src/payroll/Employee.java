package payroll;

import java.math.BigDecimal;

/** One row of the employees table. */
public class Employee {

    private int employeeId;
    private String name;
    private String department;
    private String designation;
    private BigDecimal monthlySalary;

    // Used when reading from the database (ID already exists).
    public Employee(int employeeId, String name, String department, String designation,
                    BigDecimal monthlySalary) {
        this.employeeId = employeeId;
        this.name = name;
        this.department = department;
        this.designation = designation;
        this.monthlySalary = monthlySalary;
    }

    // Used for a brand-new employee (MySQL will generate the ID).
    public Employee(String name, String department, String designation, BigDecimal monthlySalary) {
        this(0, name, department, designation, monthlySalary);
    }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(BigDecimal monthlySalary) { this.monthlySalary = monthlySalary; }
}
