package mini.chess.game.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class AuthManager {

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

    // Register user. Returns user_id or -1 on failure.
    public static int register(String username, String password) {
        String hashed = sha256(password);
        try (Connection conn = DBConnection.getConnection()) {
            String check = "SELECT user_id FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return -1; // already exists
                    }
                }
            }

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
                // create a players row referencing this user
                String insertPlayer = "INSERT INTO players (user_id, score) VALUES (?, 0)";
                try (PreparedStatement ps2 = conn.prepareStatement(insertPlayer, Statement.RETURN_GENERATED_KEYS)) {
                    ps2.setInt(1, userId);
                    ps2.executeUpdate();
                }
                conn.commit();
                return userId;
            } else {
                conn.rollback();
                return -1;
            }
        } catch (SQLException e) {
            System.err.println("Register failed: " + e.getMessage());
            return -1;
        }
    }

    // Login user. Returns user_id or -1 on failure.
    public static int login(String username, String password) {
        String hashed = sha256(password);
        try (Connection conn = DBConnection.getConnection()) {
            String q = "SELECT user_id, password_hash FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String stored = rs.getString("password_hash");
                        int userId = rs.getInt("user_id");
                        if (stored != null && stored.equals(hashed)) {
                            // ensure players row exists
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
                            return userId;
                        }
                    }
                }
            }
            conn.rollback();
            return -1;
        } catch (SQLException e) {
            System.err.println("Login failed: " + e.getMessage());
            return -1;
        }
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
}