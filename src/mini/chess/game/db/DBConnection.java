package mini.chess.game.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection class manages Oracle database connectivity
 * including establishing, reusing, and closing connections.
 */
public class DBConnection {

    // JDBC URL for Oracle PDB MINI_CHESS
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/MINI_CHESS";

    private static final String USER = "chess_adm";
    private static final String PASSWORD = "chess123";

    // Singleton connection instance
    private static Connection connection = null;

    /**
     * Get a database connection (singleton)
     * @return Connection object
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Load Oracle JDBC driver
                Class.forName("oracle.jdbc.driver.OracleDriver");

                // Establish connection
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Database connected successfully!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Failed to connect to database.");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Close the database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simple test for connection
     */
    public static void main(String[] args) {
        Connection conn = DBConnection.getConnection();
        DBConnection.closeConnection();
    }
}



