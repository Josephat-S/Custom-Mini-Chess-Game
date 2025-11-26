package mini.chess.game.utils;

import mini.chess.game.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class VerifyPlayerStatus {
    public static void main(String[] args) {
        int playerId = -1;
        try (Connection conn = DBConnection.getConnection()) {
            // Get first available player
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT player_id FROM players LIMIT 1")) {
                if (rs.next()) {
                    playerId = rs.getInt(1);
                }
            }
            
            // If no player, try to create one for user_id 1
            if (playerId == -1) {
                playerId = GameDataManager.getOrCreatePlayerId(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if (playerId == -1) {
            System.out.println("Could not find or create a player.");
            return;
        }
        
        System.out.println("Verifying player_status update for player " + playerId);
        
        int initialWins = getStat(playerId, "wins");
        int initialLosses = getStat(playerId, "losses");
        
        System.out.println("Initial Wins: " + initialWins);
        System.out.println("Initial Losses: " + initialLosses);
        
        // Simulate a Win
        System.out.println("Simulating WIN...");
        boolean success = GameDataManager.updatePlayerStatus(playerId, "WIN");
        if (!success) {
            System.out.println("Failed to update status (WIN)");
            return;
        }
        
        int newWins = getStat(playerId, "wins");
        if (newWins == initialWins + 1) {
            System.out.println("SUCCESS: Wins incremented correctly.");
        } else {
            System.out.println("FAILURE: Wins did not increment. Got " + newWins);
        }
        
        // Simulate a Loss
        System.out.println("Simulating LOSS...");
        success = GameDataManager.updatePlayerStatus(playerId, "LOSS");
        if (!success) {
            System.out.println("Failed to update status (LOSS)");
            return;
        }
        
        int newLosses = getStat(playerId, "losses");
        if (newLosses == initialLosses + 1) {
            System.out.println("SUCCESS: Losses incremented correctly.");
        } else {
            System.out.println("FAILURE: Losses did not increment. Got " + newLosses);
        }
    }
    
    private static int getStat(int playerId, String col) {
        try (Connection conn = DBConnection.getConnection()) {
            String q = "SELECT " + col + " FROM player_status WHERE player_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0; // Default or if not found (though updatePlayerStatus should create it)
    }
}
