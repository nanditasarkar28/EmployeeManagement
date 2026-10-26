package payroll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO = Data Access Object. All SQL for the employees table lives here.
 * Every query uses "?" placeholders (PreparedStatement) - user input is NEVER
 * glued into the SQL text, which prevents SQL injection.
 */
public class EmployeeDAO {

    /** Inserts a new employee and returns the ID that MySQL generated. */
    public int addEmployee(Employee employee) throws SQLException {
        String sql = "INSERT INTO employees (name, department, designation, monthly_salary) "
                   + "VALUES (?, ?, ?, ?)";
        // try-with-resources: Connection and PreparedStatement are closed automatically.
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getDepartment());
            ps.setString(3, employee.getDesignation());
            ps.setBigDecimal(4, employee.getMonthlySalary());
            ps.executeUpdate(); // executeUpdate() = INSERT / UPDATE / DELETE
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /** Returns every employee, ordered by ID. */
    public List<Employee> getAllEmployees() throws SQLException {
        String sql = "SELECT employee_id, name, department, designation, monthly_salary "
                   + "FROM employees ORDER BY employee_id";
        List<Employee> employees = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) { // executeQuery() = SELECT
            while (rs.next()) {
                employees.add(mapRow(rs));
            }
        }
        return employees;
    }

    /** Returns the employee with this ID, or null if there is none. */
    public Employee getEmployeeById(int employeeId) throws SQLException {
        String sql = "SELECT employee_id, name, department, designation, monthly_salary "
                   + "FROM employees WHERE employee_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /** Updates name, department, designation and salary. Returns true if a row changed. */
    public boolean updateEmployee(Employee employee) throws SQLException {
        String sql = "UPDATE employees SET name = ?, department = ?, designation = ?, "
                   + "monthly_salary = ? WHERE employee_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getDepartment());
            ps.setString(3, employee.getDesignation());
            ps.setBigDecimal(4, employee.getMonthlySalary());
            ps.setInt(5, employee.getEmployeeId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes the employee. If attendance or payroll rows still point to this
     * employee, MySQL refuses (foreign key) and throws SQLException (error 1451).
     * Main checks for that case first so it can show a friendlier message.
     */
    public boolean deleteEmployee(int employeeId) throws SQLException {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    // Turns the current row of a ResultSet into an Employee object.
    private Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("employee_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("designation"),
                rs.getBigDecimal("monthly_salary"));
    }
}
