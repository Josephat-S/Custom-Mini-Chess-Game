package mini.chess.game.utils;

import mini.chess.game.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TableCheck {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.out.println("Failed to connect to database.");
                return;
            }
            String query = "SELECT COUNT(*) FROM players_games";
            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("players_games count: " + rs.getInt(1));
                }
            }
            
            query = "SELECT * FROM players_games LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                System.out.println("Sample data players_games:");
                while (rs.next()) {
                    System.out.println("pg_id: " + rs.getInt("pg_id") + 
                                       ", game_id: " + rs.getInt("game_id"));
                }
            }

            query = "SELECT * FROM games LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                System.out.println("Sample data games:");
                while (rs.next()) {
                    System.out.println("game_id: " + rs.getInt("game_id") + 
                                       ", type: " + rs.getString("type") +
                                       ", status: " + rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
