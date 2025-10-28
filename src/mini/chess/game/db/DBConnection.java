package mini.chess.game.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Oracle JDBC connection helper for the Mini Chess game.
public final class DBConnection {
    // --- Connection configuration (adjust if needed) ---
    private static final String HOST = "localhost";
    private static final int PORT = 1521;
    private static final String SERVICE = "mini_chess"; // PDB service name

    private static final String USER = "adminOper";
    private static final String PASSWORD = "Admin@Group123!";

    // Oracle Thin JDBC URL using EZCONNECT syntax
    private static final String JDBC_URL = String.format(
            "jdbc:oracle:thin:@//%s:%d/%s", HOST, PORT, SERVICE
    );

    static {
        // Optional: Explicitly load the Oracle driver for older environments.
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ignored) {
            // If the driver isn't found here, DriverManager may still locate it
            // via the Service Provider mechanism when the driver jar is on the classpath.
        }
        // Optional: Set a global login timeout (in seconds)
        DriverManager.setLoginTimeout(10);
    }

    private DBConnection() {
        // Utility class; prevent instantiation
    }

    /**
     * Obtain a new JDBC connection to the configured Oracle PDB.
     *
     * @return an open {@link Connection}
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }

    /**
     * Expose the JDBC URL for logging or diagnostics.
     */
    public static String getJdbcUrl() {
        return JDBC_URL;
    }
}
