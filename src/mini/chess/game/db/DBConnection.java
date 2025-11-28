package mini.chess.game.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static String url = "jdbc:mysql://localhost:3306/mini_chess?serverTimezone=UTC&useSSL=false";
    private static String user = "root";
    private static String password = "";

    static {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.File configFile = new java.io.File("db.properties");
            if (configFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                    url = props.getProperty("db.url", url);
                    user = props.getProperty("db.user", user);
                    password = props.getProperty("db.password", password);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load db.properties, using defaults. " + e.getMessage());
        }
    }

    private static Connection instance = null;

    public static String getDbUrl() {
        return url;
    }

    public static synchronized Connection getConnection() throws SQLException {
        try {
            if (instance == null || instance.isClosed() || !instance.isValid(2)) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    throw new SQLException("MySQL driver not found", e);
                }
                instance = DriverManager.getConnection(url, user, password);
                instance.setAutoCommit(false);
                System.out.println("Database connected to: " + url);
            }
        } catch (SQLException e) {
            // If validation fails, try to reconnect once
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException cnfe) {
                throw new SQLException("MySQL driver not found", cnfe);
            }
            instance = DriverManager.getConnection(url, user, password);
            instance.setAutoCommit(false);
        }
        return instance;
    }
}
