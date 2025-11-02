package mini.chess.game.db;

import mini.chess.game.Models.*;
import java.sql.*;

public class GameDataManager {

    // ✅ Connect to Oracle database
    private static Connection connect() {
        Connection conn = null;
        try {
            String url = "jdbc:oracle:thin:@//localhost:1521/mini_chess";
            String user = "chess_adm";
            String password = "chess123";

            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false);
            System.out.println("✅ Connected to database successfully!");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
        return conn;
    }

    // ✅ Save game into database
    public static int saveGame(Board board, String currentPlayer) {
        int gameId = -1;

        try (Connection conn = connect()) {
            if (conn == null) return -1;

            // Insert into SAVED_GAMES (auto-incremented ID)
            String insertGame = "INSERT INTO SAVED_GAMES (CURRENT_PLAYER) VALUES (?)";
            PreparedStatement ps = conn.prepareStatement(insertGame, new String[]{"GAME_ID"});
            ps.setString(1, currentPlayer);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) gameId = rs.getInt(1);
            rs.close();
            ps.close();

            // Insert all pieces into SAVED_PIECES
            String insertPiece = "INSERT INTO SAVED_PIECES (GAME_ID, ROW_INDEX, COL_INDEX, PIECE_TYPE, PLAYER_NAME) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement psPiece = conn.prepareStatement(insertPiece);

            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    Piece p = board.getPiece(r, c);
                    if (p != null) {
                        psPiece.setInt(1, gameId);
                        psPiece.setInt(2, r);
                        psPiece.setInt(3, c);
                        psPiece.setString(4, p.getClass().getSimpleName());
                        psPiece.setString(5, p.getPlayer());
                        psPiece.addBatch();
                    }
                }
            }

            psPiece.executeBatch();
            conn.commit();

            System.out.println("✅ Game saved successfully with ID: " + gameId);

        } catch (SQLException e) {
            System.err.println("❌ Error while saving game: " + e.getMessage());
        }
        return gameId;
    }

    // ✅ Load a saved game
    public static Board loadGame(int gameId) {
        Board board = new Board();
        board.clear();

        try (Connection conn = connect()) {
            if (conn == null) {
                System.err.println("❌ Cannot load game: Database connection is null.");
                return board;
            }

            String query = "SELECT ROW_INDEX, COL_INDEX, PIECE_TYPE, PLAYER_NAME FROM SAVED_PIECES WHERE GAME_ID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, gameId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int row = rs.getInt("ROW_INDEX");
                int col = rs.getInt("COL_INDEX");
                String type = rs.getString("PIECE_TYPE");
                String player = rs.getString("PLAYER_NAME");

                Piece piece = switch (type) {
                    case "Leader" -> new Leader(row, col, player);
                    case "Soldier" -> new Soldier(row, col, player);
                    default -> null;
                };

                if (piece != null) board.setPiece(row, col, piece);
            }

            System.out.println("✅ Game loaded successfully from database!");

        } catch (SQLException e) {
            System.err.println("❌ Error while loading game: " + e.getMessage());
        }

        return board;
    }

    // ✅ Print saved game info (for testing/debug)
    public static void printSavedGame(int gameId) {
        try (Connection conn = connect()) {
            if (conn == null) return;

            System.out.println("\n=== SAVED GAME DATA (GAME_ID = " + gameId + ") ===");

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM SAVED_GAMES WHERE GAME_ID = ?");
            ps1.setInt(1, gameId);
            ResultSet rs1 = ps1.executeQuery();

            if (rs1.next()) {
                System.out.println("Game ID: " + rs1.getInt("GAME_ID"));
                System.out.println("Current Player: " + rs1.getString("CURRENT_PLAYER"));
            }

            System.out.println("\n--- SAVED PIECES ---");
            PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT ROW_INDEX, COL_INDEX, PIECE_TYPE, PLAYER_NAME FROM SAVED_PIECES WHERE GAME_ID = ? ORDER BY ROW_INDEX, COL_INDEX"
            );
            ps2.setInt(1, gameId);
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
                System.out.printf("Row:%d Col:%d | %s (%s)\n",
                        rs2.getInt("ROW_INDEX"),
                        rs2.getInt("COL_INDEX"),
                        rs2.getString("PIECE_TYPE"),
                        rs2.getString("PLAYER_NAME"));
            }

            System.out.println("===============================");

        } catch (SQLException e) {
            System.err.println("❌ Error while printing saved game: " + e.getMessage());
        }
    }
}











