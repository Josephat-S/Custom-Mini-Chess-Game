package mini.chess.game.utils;

import mini.chess.game.Models.Board;
import mini.chess.game.Models.Piece;
import mini.chess.game.Models.Leader;
import mini.chess.game.Models.Soldier;
import mini.chess.game.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameDataManager {

    public static class GameCreateResult {
        public final int gameId;
        public final int playerId;
        public GameCreateResult(int gameId, int playerId) {
            this.gameId = gameId;
            this.playerId = playerId;
        } 
    }

    public static int getOrCreatePlayerId(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return -1;
            String q = "SELECT player_id FROM players WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("player_id");
                }
            }
            String ins = "INSERT INTO players (user_id, score) VALUES (?, 0)";
            try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("getOrCreatePlayerId: " + e.getMessage());
        }
        return -1;
    }

    public static GameCreateResult createLanGameForHost(int userId, Board board, int currentPlayerTurn) {
        String boardJson = boardToStringForNetwork(board);
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return new GameCreateResult(-1, -1);
            conn.setAutoCommit(false);
            try {
                int playerId = getOrCreatePlayerId(userId);
                if (playerId == -1) {
                    conn.rollback();
                    return new GameCreateResult(-1, -1);
                }
                int gameId;
                String insGame = "INSERT INTO games (type, status, start_time) VALUES ('lan','ongoing',CURRENT_TIMESTAMP)";
                try (PreparedStatement ps = conn.prepareStatement(insGame, Statement.RETURN_GENERATED_KEYS)) {
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return new GameCreateResult(-1, -1);
                        }
                        gameId = rs.getInt(1);
                    }
                }
                String insPG = "INSERT INTO players_games (game_id, player_one_id) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insPG)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, playerId);
                    ps.executeUpdate();
                }
                String insState = "INSERT INTO gamestate (game_id, player_turn, board_data, last_move) VALUES (?, ?, ?, NULL)";
                try (PreparedStatement ps = conn.prepareStatement(insState)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, currentPlayerTurn);
                    ps.setString(3, boardJson);
                    ps.executeUpdate();
                }
                conn.commit();
                return new GameCreateResult(gameId, playerId);
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("createLanGameForHost failed: " + e.getMessage());
                return new GameCreateResult(-1, -1);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("DB connect error in createLanGameForHost: " + e.getMessage());
            return new GameCreateResult(-1, -1);
        }
    }

    public static int addPlayerToExistingGame(int gameId, int userId) {
        int playerId = getOrCreatePlayerId(userId);
        if (playerId == -1) return -1;
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return -1;
            String updateQuery = "UPDATE players_games SET player_two_id = ? WHERE game_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                ps.setInt(1, playerId);
                ps.setInt(2, gameId);
                int upd = ps.executeUpdate();
                if (upd == 0) return -1;
            }
            return playerId;
        } catch (SQLException e) {
            System.err.println("addPlayerToExistingGame failed: " + e.getMessage());
            return -1;
        }
    }

    public static boolean recordMoveAndUpdateState(int gameId, int playerId, Integer moveNumber, String fromCell, String toCell, Board board) {
        String boardJson = boardToStringForNetwork(board);
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                int playerTurn = getPlayerNumberForGame(conn, gameId, playerId);
                String insMove = "INSERT INTO moves (game_id, player_id, move_number, from_cell, to_cell) VALUES (?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insMove)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, playerId);
                    if (moveNumber != null) ps.setInt(3, moveNumber); else ps.setNull(3, Types.INTEGER);
                    ps.setString(4, fromCell);
                    ps.setString(5, toCell);
                    ps.executeUpdate();
                }
                String insState = "INSERT INTO gamestate (game_id, player_turn, board_data, last_move) VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insState)) {
                    int nextTurn = playerTurn == 1 ? 2 : 1;
                    ps.setInt(1, gameId);
                    ps.setInt(2, nextTurn);
                    ps.setString(3, boardJson);
                    ps.setString(4, fromCell + "->" + toCell);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("recordMoveAndUpdateState failed: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("DB connect error in recordMoveAndUpdateState: " + e.getMessage());
            return false;
        }
    }

    private static int getPlayerNumberForGame(Connection conn, int gameId, int playerId) throws SQLException {
        String q = "SELECT player_one_id, player_two_id FROM players_games WHERE game_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int p1 = rs.getInt("player_one_id");
                    int p2 = rs.getInt("player_two_id");
                    if (playerId == p1) return 1;
                    if (playerId == p2) return 2;
                }
            }
        }
        return 1;
    }

    public static List<Integer> listSavedGamesForUser(int userId) {
        List<Integer> savedGames = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return savedGames;
            String query = """
                SELECT DISTINCT g.game_id
                FROM games g
                JOIN players_games pg ON g.game_id = pg.game_id
                JOIN players p ON pg.player_one_id = p.player_id OR pg.player_two_id = p.player_id
                WHERE p.user_id = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) savedGames.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("listSavedGamesForUser failed: " + e.getMessage());
        }
        return savedGames;
    }

    public static Board loadGameById(int gameId) {
        String boardJson = null;
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;
            String query = "SELECT board_data FROM gamestate WHERE game_id = ? ORDER BY id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, gameId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) boardJson = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("loadGameById failed: " + e.getMessage());
        }
        return boardFromStringForNetwork(boardJson);
    }

    public static boolean saveUnfinishedGameWithBoardData(int userId, String boardJson, int currentTurn) {
        int playerId = getOrCreatePlayerId(userId);
        if (playerId == -1) return false;
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                int gameId;
                String insGame = "INSERT INTO games (type, status, start_time) VALUES ('single_mode','paused',CURRENT_TIMESTAMP)";
                try (PreparedStatement ps = conn.prepareStatement(insGame, Statement.RETURN_GENERATED_KEYS)) {
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        gameId = rs.getInt(1);
                    }
                }
                String insPG = "INSERT INTO players_games (game_id, player_one_id) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insPG)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, playerId);
                    ps.executeUpdate();
                }
                String insState = "INSERT INTO gamestate (game_id, player_turn, board_data, last_move) VALUES (?,?,?,NULL)";
                try (PreparedStatement ps = conn.prepareStatement(insState)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, currentTurn);
                    ps.setString(3, boardJson);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("saveUnfinishedGameWithBoardData failed: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("DB connect error in saveUnfinishedGameWithBoardData: " + e.getMessage());
            return false;
        }
    }

    public static String boardToStringForNetwork(Board board) {
        if (board == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        final int SIZE = 5;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null) continue;
                if (!first) sb.append(',');
                first = false;
                sb.append('{');
                sb.append("\"type\":\"").append(escapeJson(p.getClass().getSimpleName())).append("\",");
                sb.append("\"player\":\"").append(escapeJson(p.getPlayer())).append("\",");
                sb.append("\"row\":").append(r).append(",");
                sb.append("\"col\":").append(c);
                sb.append('}');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public static Board boardFromStringForNetwork(String json) {
        Board board = new Board();
        final int SIZE = 5;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                board.setPiece(r, c, null);
            }
        }
        if (json == null) return board;
        String trimmed = json.trim();
        if (trimmed.isEmpty() || trimmed.equals("[]")) return board;
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return board;
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return board;
        String[] items = inner.split("\\},\\s*\\{");
        for (int i = 0; i < items.length; i++) {
            String it = items[i];
            if (!it.startsWith("{")) it = "{" + it;
            if (!it.endsWith("}")) it = it + "}";
            String type = extractStringField(it, "type");
            String player = extractStringField(it, "player");
            Integer row = extractIntField(it, "row");
            Integer col = extractIntField(it, "col");
            if (type == null || player == null || row == null || col == null) continue;
            Piece p = null;
            switch (type) {
                case "Leader" -> p = new Leader(row, col, player);
                case "Soldier" -> p = new Soldier(row, col, player);
                default -> { }
            }
            if (p != null) board.setPiece(row, col, p);
        }
        return board;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String extractStringField(String s, String field) {
        String key = "\"" + field + "\":";
        int idx = s.indexOf(key);
        if (idx < 0) return null;
        int start = s.indexOf('"', idx + key.length());
        if (start < 0) return null;
        int end = s.indexOf('"', start + 1);
        if (end < 0) return null;
        return unescapeJson(s.substring(start + 1, end));
    }

    private static Integer extractIntField(String s, String field) {
        String key = "\"" + field + "\":";
        int idx = s.indexOf(key);
        if (idx < 0) return null;
        int pos = idx + key.length();
        StringBuilder num = new StringBuilder();
        while (pos < s.length()) {
            char ch = s.charAt(pos);
            if ((ch >= '0' && ch <= '9') || ch == '-') {
                num.append(ch);
                pos++;
            } else break;
        }
        try {
            return Integer.parseInt(num.toString());
        } catch (Exception e) {
            return null;
        }
    }
    public static boolean updatePlayerScore(int playerId, int scoreIncrement) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                String updateScore = "UPDATE players SET score = score + ? WHERE player_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateScore)) {
                    ps.setInt(1, scoreIncrement);
                    ps.setInt(2, playerId);
                    int rowsAffected = ps.executeUpdate();

                    if (rowsAffected == 0) {
                        conn.rollback();
                        System.err.println("updatePlayerScore: No player found with ID " + playerId);
                        return false;
                    }
                }
                conn.commit();
                System.out.println("? Updated player " + playerId + " score by +" + scoreIncrement);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("updatePlayerScore failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("updatePlayerScore connection error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public static boolean markGameAsComplete(int gameId, int winnerId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                String updateGame = "UPDATE games SET status = 'done', end_time = CURRENT_TIMESTAMP WHERE game_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateGame)) {
                    ps.setInt(1, gameId);
                    ps.executeUpdate();
                }

                String updatePlayersGames = "UPDATE players_games SET winner_id = ? WHERE game_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updatePlayersGames)) {
                    ps.setInt(1, winnerId);
                    ps.setInt(2, gameId);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("markGameAsComplete failed: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("DB connect error in markGameAsComplete: " + e.getMessage());
            return false;
        }
    }

    public static boolean updatePlayerStatus(int playerId, String result) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                // First ensure the player exists in player_status
                String checkQuery = "SELECT player_id FROM player_status WHERE player_id = ?";
                boolean exists = false;
                try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
                    ps.setInt(1, playerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) exists = true;
                    }
                }

                if (!exists) {
                    String insertQuery = "INSERT INTO player_status (player_id, wins, losses, draws) VALUES (?, 0, 0, 0)";
                    try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                        ps.setInt(1, playerId);
                        ps.executeUpdate();
                    }
                }

                // Now update the stats
                String updateQuery = "";
                switch (result) {
                    case "WIN" -> updateQuery = "UPDATE player_status SET wins = wins + 1 WHERE player_id = ?";
                    case "LOSS" -> updateQuery = "UPDATE player_status SET losses = losses + 1 WHERE player_id = ?";
                    case "DRAW" -> updateQuery = "UPDATE player_status SET draws = draws + 1 WHERE player_id = ?";
                    default -> {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                    ps.setInt(1, playerId);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("updatePlayerStatus failed: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("DB connect error in updatePlayerStatus: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Game state class for LAN polling
     */
    public static class GameState {
        public final int stateId;
        public final int playerTurn;
        public final String boardData;
        public final String lastMove;
        public final Timestamp savedAt;
        
        public GameState(int stateId, int playerTurn, String boardData, String lastMove, Timestamp savedAt) {
            this.stateId = stateId;
            this.playerTurn = playerTurn;
            this.boardData = boardData;
            this.lastMove = lastMove;
            this.savedAt = savedAt;
        }
    }
    
    /**
     * Get the latest game state for LAN polling
     */
    public static GameState getLatestGameState(int gameId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return null;
            
            String query = "SELECT id, player_turn, board_data, last_move, saved_at FROM gamestate WHERE game_id = ? ORDER BY id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, gameId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new GameState(
                            rs.getInt("id"),
                            rs.getInt("player_turn"),
                            rs.getString("board_data"),
                            rs.getString("last_move"),
                            rs.getTimestamp("saved_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("getLatestGameState failed: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get game states since a specific state ID (for polling new moves)
     */
    public static java.util.List<GameState> getGameStatesSince(int gameId, int sinceStateId) {
        java.util.List<GameState> states = new java.util.ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return states;
            
            String query = "SELECT id, player_turn, board_data, last_move, saved_at FROM gamestate WHERE game_id = ? AND id > ? ORDER BY id ASC";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, gameId);
                ps.setInt(2, sinceStateId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        states.add(new GameState(
                            rs.getInt("id"),
                            rs.getInt("player_turn"),
                            rs.getString("board_data"),
                            rs.getString("last_move"),
                            rs.getTimestamp("saved_at")
                        ));
                    }
                }
            
            }
        } catch (SQLException e) {
            System.err.println("getGameStatesSince failed: " + e.getMessage());
        }
        return states;
    }
}