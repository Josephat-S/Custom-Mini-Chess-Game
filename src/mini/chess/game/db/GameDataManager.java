package mini.chess.game.db;

import mini.chess.game.Models.Board;
import mini.chess.game.Models.Piece;
import mini.chess.game.Models.Leader;
import mini.chess.game.Models.Soldier;

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
                String insertGame = "INSERT INTO games (`type`, `start_time`, `status`) VALUES (?, NOW(), ?)";
                int gameId;
                try (PreparedStatement ps = conn.prepareStatement(insertGame, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, "lan");
                    ps.setString(2, "ongoing");
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        gameId = rs.getInt(1);
                    }
                }

                int playerId = getOrCreatePlayerId(userId);
                if (playerId == -1) throw new SQLException("Player creation failed");

                String insertPlayersGames = "INSERT INTO players_games (game_id, player_one_id, player_two_id) VALUES (?, ?, NULL)";
                try (PreparedStatement ps = conn.prepareStatement(insertPlayersGames)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, playerId);
                    ps.executeUpdate();
                }

                String insertState = "INSERT INTO gamestate (game_id, player_turn, board_data, last_move) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertState)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, currentPlayerTurn);
                    ps.setString(3, boardJson != null ? boardJson : "[]");
                    ps.setString(4, "initial");
                    ps.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(true);
                return new GameCreateResult(gameId, playerId);
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("createLanGameForHost failed: " + ex.getMessage());
                conn.setAutoCommit(true);
                return new GameCreateResult(-1, -1);
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
                ps.executeUpdate();
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
                String insertMove = "INSERT INTO moves (game_id, player_id, move_number, from_cell, to_cell) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertMove)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, playerId);
                    ps.setInt(3, moveNumber);
                    ps.setString(4, fromCell);
                    ps.setString(5, toCell);
                    ps.executeUpdate();
                }

                int nextTurn = getPlayerNumberForGame(conn, gameId, playerId) == 1 ? 2 : 1;
                String lastMove = fromCell + "->" + toCell;

                String insertState = "INSERT INTO gamestate (game_id, player_turn, board_data, last_move) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertState)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, nextTurn);
                    ps.setString(3, boardJson);
                    ps.setString(4, lastMove);
                    ps.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(true);
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("recordMoveAndUpdateState failed: " + ex.getMessage());
                conn.setAutoCommit(true);
                return false;
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
                    if (rs.getInt("player_one_id") == playerId) return 1;
                    if (rs.getInt("player_two_id") == playerId) return 2;
                }
            }
        }
        return 1;
    }

    public static List<Integer> listSavedGamesForUser(int userId) {
        List<Integer> savedGames = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return savedGames;
            String query = "SELECT g.game_id FROM games g JOIN players_games pg ON g.game_id = pg.game_id JOIN players p ON pg.player_one_id = p.player_id OR pg.player_two_id = p.player_id WHERE p.user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        savedGames.add(rs.getInt("game_id"));
                    }
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
                    if (rs.next()) {
                        boardJson = rs.getString("board_data");
                    }
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
                Integer gameId = null;
                String qGame = "SELECT pg.game_id FROM players_games pg JOIN games g ON pg.game_id = g.game_id WHERE (pg.player_one_id = ? OR pg.player_two_id = ?) AND g.status = 'ongoing' ORDER BY pg.pg_id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(qGame)) {
                    ps.setInt(1, playerId);
                    ps.setInt(2, playerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) gameId = rs.getInt("game_id");
                    }
                }

                if (gameId == null) {
                    String insGame = "INSERT INTO games (type, start_time, status) VALUES ('lan', NOW(), 'ongoing')";
                    try (PreparedStatement ps = conn.prepareStatement(insGame, Statement.RETURN_GENERATED_KEYS)) {
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) gameId = rs.getInt(1);
                        }
                    }
                    if (gameId == null) throw new SQLException("Failed to create game");
                    String insPg = "INSERT INTO players_games (game_id, player_one_id, player_two_id) VALUES (?, ?, NULL)";
                    try (PreparedStatement ps = conn.prepareStatement(insPg)) {
                        ps.setInt(1, gameId);
                        ps.setInt(2, playerId);
                        ps.executeUpdate();
                    }
                }

                String insState = "INSERT INTO gamestate (game_id, player_turn, board_data, last_move) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insState)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, currentTurn);
                    ps.setString(3, boardJson != null && !boardJson.isEmpty() ? boardJson : "[]");
                    ps.setString(4, "autosave");
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("saveUnfinishedGameWithBoardData failed: " + ex.getMessage());
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
                default -> {
                }
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
}
