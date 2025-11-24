package mini.chess.game.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class AuthManager {

    /**
     * Result class for authentication operations with detailed error information.
     */
    public static class AuthResult {
        public final int userId;
        public final String errorCode;  // null if success
        public final String userMessage;  // user-friendly message
        
        private AuthResult(int userId, String errorCode, String userMessage) {
            this.userId = userId;
            this.errorCode = errorCode;
            this.userMessage = userMessage;
        }
        
        public static AuthResult success(int userId) {
            return new AuthResult(userId,null, "Success");
        }
        
        public static AuthResult error(String errorCode, String userMessage) {
            return new AuthResult(-1, errorCode, userMessage);
        }
        
        public boolean isSuccess() {
            return userId != -1 && errorCode == null;
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte by : b) sb.append(String.format("%02x", by));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Validates username format.
     * @return true if valid, false otherwise
     */
    private static boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]+$");
    }
    
    /**
     * Get username by user ID.
     * @param userId The user ID
     * @return username or null if not found
     */
    public static String getUsernameById(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT username FROM users WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("username");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("getUsernameById failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Register user with detailed error reporting.
     * @param username The username
     * @param password The password
     * @return AuthResult with success or detailed error
     */
    public static AuthResult registerWithDetails(String username, String password) {
        // Validate username format
        if (!isValidUsername(username)) {
            return AuthResult.error("INVALID_USERNAME", 
                "Username must be 3-20 characters and contain only letters, numbers, and underscores.");
        }
        
        // Validate password strength
        if (password == null || password.length() < 6) {
            return AuthResult.error("WEAK_PASSWORD", 
                "Password must be at least 6 characters long.");
        }
        
        String hashed = sha256(password);
        try (Connection conn = DBConnection.getConnection()) {
            // Check if username already exists
            String check = "SELECT user_id FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return AuthResult.error("USERNAME_EXISTS", 
                            "This username is already taken. Please choose another.");
                    }
                }
            }

            // Insert new user
            String insertUser = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            int userId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, hashed);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) userId = rs.getInt(1);
                }
            }

            if (userId != -1) {
                // Create a players row
                String insertPlayer = "INSERT INTO players (user_id, score) VALUES (?, 0)";
                try (PreparedStatement ps2 = conn.prepareStatement(insertPlayer, Statement.RETURN_GENERATED_KEYS)) {
                    ps2.setInt(1, userId);
                    ps2.executeUpdate();
                }
                conn.commit();
                return AuthResult.success(userId);
            } else {
                conn.rollback();
                return AuthResult.error("DATABASE_ERROR", 
                    "Unable to create account. Please try again later.");
            }
        } catch (SQLException e) {
            System.err.println("Register failed: " + e.getMessage());
            return AuthResult.error("DATABASE_ERROR", 
                "Unable to create account. Please try again later.");
        }
    }
    
    // Register user. Returns user_id or -1 on failure.
    // Kept for backward compatibility
    public static int register(String username, String password) {
        AuthResult result = registerWithDetails(username, password);
        return result.userId;
    }

    /**
     * Login user with detailed error reporting.
     * @param username The username
     * @param password The password
     * @return AuthResult with success or detailed error
     */
    public static AuthResult loginWithDetails(String username, String password) {
        String hashed = sha256(password);
        try (Connection conn = DBConnection.getConnection()) {
            String q = "SELECT user_id, password_hash, is_locked FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Check lock status first
                        if (rs.getInt("is_locked") == 1) {
                            return AuthResult.error("ACCOUNT_LOCKED", 
                                "Your account has been locked. Please contact an administrator.");
                        }

                        String stored = rs.getString("password_hash");
                        int userId = rs.getInt("user_id");
                        if (stored != null && stored.equals(hashed)) {
                            // Ensure players row exists
                            String chkPlayer = "SELECT player_id FROM players WHERE user_id = ?";
                            try (PreparedStatement ps2 = conn.prepareStatement(chkPlayer)) {
                                ps2.setInt(1, userId);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    if (!rs2.next()) {
                                        String insertPlayer = "INSERT INTO players (user_id, score) VALUES (?, 0)";
                                        try (PreparedStatement ps3 = conn.prepareStatement(insertPlayer)) {
                                            ps3.setInt(1, userId);
                                            ps3.executeUpdate();
                                        }
                                    }
                                }
                            }
                            conn.commit();
                            return AuthResult.success(userId);
                        } else {
                            conn.rollback();
                            return AuthResult.error("INVALID_PASSWORD", 
                                "Incorrect password. Please try again.");
                        }
                    } else {
                        conn.rollback();
                        return AuthResult.error("ACCOUNT_NOT_FOUND", 
                            "No account found with this username.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Login failed: " + e.getMessage());
            return AuthResult.error("DATABASE_ERROR", 
                "Login failed. Please check your connection and try again.");
        }
    }
    
    // Login user. Returns user_id or -1 on failure.
    // Kept for backward compatibility
    public static int login(String username, String password) {
        AuthResult result = loginWithDetails(username, password);
        return result.userId == -1 && "ACCOUNT_LOCKED".equals(result.errorCode) ? -2 : result.userId;
    }

    // Lookup player_id by user_id
    public static int getPlayerIdForUser(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            String q = "SELECT player_id FROM players WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int pid = rs.getInt("player_id");
                        conn.commit();
                        return pid;
                    }
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            System.err.println("getPlayerIdForUser failed: " + e.getMessage());
        }
        return -1;
    }
    
    // Check if user is admin
    public static boolean isAdmin(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT 1 FROM admins WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("isAdmin check failed: " + e.getMessage());
            return false;
        }
    }
}
