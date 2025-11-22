package mini.chess.game.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AdminDAO {

    public static Vector<Vector<Object>> getAllLogs() {
        Vector<Vector<Object>> data = new Vector<>();
        String query = "SELECT l.log_id, u.username, l.action, l.log_time " +
                       "FROM user_logs l " +
                       "JOIN users u ON l.user_id = u.user_id " +
                       "ORDER BY l.log_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("log_id"));
                row.add(rs.getString("username"));
                row.add(rs.getString("action"));
                row.add(rs.getTimestamp("log_time"));
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static Vector<Vector<Object>> getAllGames() {
        Vector<Vector<Object>> data = new Vector<>();
        String query = "SELECT g.game_id, g.type, g.status, g.start_time, g.end_time, " +
                       "p1_u.username as player1, p2_u.username as player2, w_u.username as winner " +
                       "FROM games g " +
                       "JOIN players_games pg ON g.game_id = pg.game_id " +
                       "JOIN players p1 ON pg.player_one_id = p1.player_id " +
                       "JOIN users p1_u ON p1.user_id = p1_u.user_id " +
                       "LEFT JOIN players p2 ON pg.player_two_id = p2.player_id " +
                       "LEFT JOIN users p2_u ON p2.user_id = p2_u.user_id " +
                       "LEFT JOIN players w ON pg.winner_id = w.player_id " +
                       "LEFT JOIN users w_u ON w.user_id = w_u.user_id " +
                       "ORDER BY g.start_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("game_id"));
                row.add(rs.getString("type"));
                row.add(rs.getString("player1"));
                row.add(rs.getString("player2") != null ? rs.getString("player2") : "AI/None");
                row.add(rs.getString("winner") != null ? rs.getString("winner") : "-");
                row.add(rs.getString("status"));
                row.add(rs.getTimestamp("start_time"));
                row.add(rs.getTimestamp("end_time"));
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static Vector<Vector<Object>> getAllPlayers() {
        Vector<Vector<Object>> data = new Vector<>();
        String query = "SELECT p.player_id, u.username, p.score, " +
                       "COALESCE(ps.wins, 0) as wins, COALESCE(ps.losses, 0) as losses, COALESCE(ps.draws, 0) as draws " +
                       "FROM players p " +
                       "JOIN users u ON p.user_id = u.user_id " +
                       "LEFT JOIN player_status ps ON p.player_id = ps.player_id " +
                       "ORDER BY p.player_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("player_id"));
                row.add(rs.getString("username"));
                row.add(rs.getInt("score"));
                row.add(rs.getInt("wins"));
                row.add(rs.getInt("losses"));
                row.add(rs.getInt("draws"));
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static boolean updatePlayerScore(int playerId, int newScore) {
        String query = "UPDATE players SET score = ? WHERE player_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, newScore);
            ps.setInt(2, playerId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                conn.commit();
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Vector<Vector<Object>> getAllUsers() {
        Vector<Vector<Object>> data = new Vector<>();
        String query = "SELECT u.user_id, u.username, u.is_locked, " +
                       "CASE WHEN a.admin_id IS NOT NULL THEN 'Admin' ELSE 'Player' END as role " +
                       "FROM users u " +
                       "LEFT JOIN admins a ON u.user_id = a.user_id " +
                       "ORDER BY u.user_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("user_id"));
                row.add(rs.getString("username"));
                row.add(rs.getString("role"));
                row.add(rs.getInt("is_locked") == 1 ? "Locked" : "Active");
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static boolean updateUser(int userId, String newUsername) {
        String query = "UPDATE users SET username = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, newUsername);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                conn.commit();
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUser(int userId) {
        String query = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                conn.commit();
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean toggleUserLock(int userId, boolean lock) {
        String query = "UPDATE users SET is_locked = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, lock ? 1 : 0);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                conn.commit();
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
