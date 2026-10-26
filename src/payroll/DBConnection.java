package payroll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates connections to the MySQL database.
 * Every DAO class calls DBConnection.getConnection() whenever it needs one.
 */
public class DBConnection {

    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE = "payroll_db";
    private static final String USERNAME = "root";

    // >>> REPLACE THIS with YOUR OWN MySQL root password before running. <<<
    private static final String PASSWORD = "Person@12345";

    // allowPublicKeyRetrieval + useSSL=false avoid common login errors on a local MySQL 8 server.
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?useSSL=false&allowPublicKeyRetrieval=true";

    private DBConnection() {
        // Utility class: nobody should create objects of it.
    }

    /**
     * Opens a NEW connection each time. The caller must close it
     * (we always do that with try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
