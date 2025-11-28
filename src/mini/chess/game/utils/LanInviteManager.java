package mini.chess.game.utils;

import mini.chess.game.db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages LAN game invitations using the existing games table
 */
public class LanInviteManager {
    
    public static class Invitation {
        public final int gameId;
        public final String fromUsername;
        public final int fromUserId;
        
        public Invitation(int gameId, String fromUsername, int fromUserId) {
            this.gameId = gameId;
            this.fromUsername = fromUsername;
            this.fromUserId = fromUserId;
        }
    }
    
    /**
     * Send an invitation to another player by username
     * @return gameId if successful, -1 if failed
     */
    public static int sendInvitation(int fromUserId, String toUsername) {
        // Step 1: Get sender's player ID (uses its own connection)
        int fromPlayerId = GameDataManager.getOrCreatePlayerId(fromUserId);
        if (fromPlayerId == -1) {
            System.err.println("Failed to get/create player ID for sender");
            return -1;
        }
        
        // Step 2: Look up recipient's user ID (separate connection)
        int toUserId = -1;
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return -1;
            
            String getUserQuery = "SELECT user_id FROM users WHERE LOWER(username) = LOWER(?)";
            try (PreparedStatement ps = conn.prepareStatement(getUserQuery)) {
                ps.setString(1, toUsername);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        toUserId = rs.getInt("user_id");
                    } else {
                        System.err.println("User not found: '" + toUsername + "'");
                        return -1;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to look up user: " + e.getMessage());
            return -1;
        }
        
        // Step 3: Get recipient's player ID (uses its own connection)
        int toPlayerId = GameDataManager.getOrCreatePlayerId(toUserId);
        if (toPlayerId == -1) {
            System.err.println("Failed to get/create player ID for recipient");
            return -1;
        }
        
        // Step 4: Create the pending game (single transaction)
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return -1;
            conn.setAutoCommit(false);
            
            try {
                // Create a pending game
                int gameId;
                String createGameQuery = "INSERT INTO games (type, status, start_time) VALUES ('lan', 'pending', NULL)";
                try (PreparedStatement ps = conn.prepareStatement(createGameQuery, Statement.RETURN_GENERATED_KEYS)) {
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            gameId = rs.getInt(1);
                        } else {
                            conn.rollback();
                            return -1;
                        }
                    }
                }
                
                // Add to players_games
                String insertPGQuery = "INSERT INTO players_games (game_id, player_one_id, player_two_id) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertPGQuery)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, fromPlayerId);
                    ps.setInt(3, toPlayerId);
                    ps.executeUpdate();
                }
                
                conn.commit();
                return gameId;
                
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Failed to create game: " + e.getMessage());
                e.printStackTrace();
                return -1;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("DB connection error: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Get pending invitations for a user
     */
    public static List<Invitation> getPendingInvitations(int userId) {
        List<Invitation> invitations = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return invitations;
            
            String query = """
                SELECT g.game_id, u.username, u.user_id
                FROM games g
                JOIN players_games pg ON g.game_id = pg.game_id
                JOIN players p1 ON pg.player_one_id = p1.player_id
                JOIN users u ON p1.user_id = u.user_id
                JOIN players p2 ON pg.player_two_id = p2.player_id
                WHERE g.status = 'pending' 
                AND p2.user_id = ?
                ORDER BY g.created_at DESC
                """;
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        invitations.add(new Invitation(
                            rs.getInt("game_id"),
                            rs.getString("username"),
                            rs.getInt("user_id")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("getPendingInvitations failed: " + e.getMessage());
        }
        
        return invitations;
    }
    
    /**
     * Accept an invitation - changes game status to ongoing and initializes board
     * @return player  IDs [myPlayerId, opponentPlayerId] or null if failed
     */
    public static int[] acceptInvitation(int gameId, int myUserId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;
            conn.setAutoCommit(false);
            
            try {
                // Get player IDs
                int myPlayerId = -1;
                int opponentPlayerId = -1;
                
                String getPlayersQuery = """
                    SELECT pg.player_one_id, pg.player_two_id, p2.user_id
                    FROM players_games pg
                    JOIN players p2 ON pg.player_two_id = p2.player_id
                    WHERE pg.game_id = ?
                    """;
                
                try (PreparedStatement ps = conn.prepareStatement(getPlayersQuery)) {
                    ps.setInt(1, gameId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            opponentPlayerId = rs.getInt("player_one_id");
                            myPlayerId = rs.getInt("player_two_id");
                            int playerTwoUserId = rs.getInt("user_id");
                            
                            if (playerTwoUserId != myUserId) {
                                conn.rollback();
                                return null; // Not invited to this game
                            }
                        } else {
                            conn.rollback();
                            return null;
                        }
                    }
                }
                
                // Update game status to ongoing
                String updateGameQuery = "UPDATE games SET status = 'ongoing', start_time = CURRENT_TIMESTAMP WHERE game_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateGameQuery)) {
                    ps.setInt(1, gameId);
                    ps.executeUpdate();
                }
                
                // Initialize game state with empty board
                String boardJson = GameDataManager.boardToStringForNetwork(new mini.chess.game.Models.Board());
                String insertStateQuery = "INSERT INTO gamestate (game_id, player_turn, board_data, last_move) VALUES (?, 1, ?, NULL)";
                try (PreparedStatement ps = conn.prepareStatement(insertStateQuery)) {
                    ps.setInt(1, gameId);
                    ps.setString(2, boardJson);
                    ps.executeUpdate();
                }
                
                conn.commit();
                return new int[]{myPlayerId, opponentPlayerId};
                
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("acceptInvitation failed: " + e.getMessage());
                return null;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("DB connection error in acceptInvitation: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Decline an invitation - deletes the pending game
     */
    public static boolean declineInvitation(int gameId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            
            String deleteQuery = "DELETE FROM games WHERE game_id = ? AND status = 'pending'";
            try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
                ps.setInt(1, gameId);
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("declineInvitation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cancel an invitation (sender side)
     */
    public static boolean cancelInvitation(int gameId, int senderUserId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            
            // Verify sender owns this invitation
            String verifyQuery = """
                SELECT 1 FROM games g
                JOIN players_games pg ON g.game_id = pg.game_id
                JOIN players p ON pg.player_one_id = p.player_id
                WHERE g.game_id = ? AND g.status = 'pending' AND p.user_id = ?
                """;
            
            try (PreparedStatement ps = conn.prepareStatement(verifyQuery)) {
                ps.setInt(1, gameId);
                ps.setInt(2, senderUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false; // Not the sender
                    }
                }
            }
            
            // Delete the game
            String deleteQuery = "DELETE FROM games WHERE game_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
                ps.setInt(1, gameId);
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("cancelInvitation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if an invitation was accepted (for sender to poll)
     * @return true if game is now ongoing
     */
    public static boolean isInvitationAccepted(int gameId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            
            String query = "SELECT status FROM games WHERE game_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, gameId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return "ongoing".equals(rs.getString("status"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("isInvitationAccepted failed: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get player IDs for a game
     * @return [player1Id, player2Id] or null
     */
    public static int[] getPlayerIds(int gameId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;
            
            String query = "SELECT player_one_id, player_two_id FROM players_games WHERE game_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, gameId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new int[]{rs.getInt("player_one_id"), rs.getInt("player_two_id")};
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("getPlayerIds failed: " + e.getMessage());
        }
        return null;
    }
}
