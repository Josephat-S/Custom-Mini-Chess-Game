package mini.chess.game.Models;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board implements Serializable {
    private static final long serialVersionUID = 1L;
    private Piece[][] board = new Piece[5][5];

    public Board() {
        initializeBoard();
    }

    public void initializeBoard() {
        board[4][2] = new Leader(4, 2, "Player1");
        board[3][1] = new Soldier(3, 1, "Player1");
        board[3][3] = new Soldier(3, 3, "Player1");

        board[0][2] = new Leader(0, 2, "Player2");
        board[1][1] = new Soldier(1, 1, "Player2");
        board[1][3] = new Soldier(1, 3, "Player2");
    }

    public void displayBoard() {
        System.out.println("\n=== MINI CHESS BOARD ===");
        System.out.print("    ");
        for (int c = 0; c < 5; c++) System.out.print(" " + c + "  ");
        System.out.println();
        for (int i = 0; i < 5; i++) {
            System.out.print(" " + i + "  ");
            for (int j = 0; j < 5; j++) {
                if (board[i][j] == null) System.out.print("[ ] ");
                else System.out.print("[" + board[i][j].getSymbol() + "] ");
            }
            System.out.println();
        }
        System.out.println("=========================");
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        if (piece == null) {
            System.out.println("No piece at that position!");
            return false;
        }
        if (!piece.canMove(toRow, toCol)) {
            System.out.println("Invalid move for " + piece.name);
            return false;
        }
        Piece target = board[toRow][toCol];
        if (target != null) System.out.println(piece.player + " captured " + target.player + "'s " + target.name);

        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;
        piece.row = toRow; piece.col = toCol;

        System.out.println(piece.player + " moved " + piece.name);
        displayBoard();
        return true;
    }

    public String checkWinner() {
        boolean player1Leader = false, player2Leader = false;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Piece p = board[i][j];
                if (p instanceof Leader) {
                    if (p.player.equals("Player1")) player1Leader = true;
                    if (p.player.equals("Player2")) player2Leader = true;
                }
            }
        }
        if (!player1Leader) return "Player2 wins!";
        if (!player2Leader) return "Player1 wins!";
        return null;
    }

    public String checkStatus(String currentPlayer) {
        String opponent = currentPlayer.equals("Player1") ? "Player2" : "Player1";
        boolean opponentLeaderInCheck = isLeaderInCheck(opponent);
        boolean opponentHasMoves = hasAnyLegalMoves(opponent);

        if (opponentLeaderInCheck && !opponentHasMoves) return "CHECKMATE";
        else if (!opponentLeaderInCheck && !opponentHasMoves) return "DRAW";
        else if (opponentLeaderInCheck) return "CHECK";
        else return "NONE";
    }

    private boolean isLeaderInCheck(String player) {
        int leaderRow = -1, leaderCol = -1;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Piece p = board[r][c];
                if (p instanceof Leader && p.player.equals(player)) {
                    leaderRow = r; leaderCol = c;
                }
            }
        }
        if (leaderRow == -1) return false;
        String opponent = player.equals("Player1") ? "Player2" : "Player1";
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Piece p = board[r][c];
                if (p != null && p.player.equals(opponent) && p.canMove(leaderRow, leaderCol)) return true;
            }
        }
        return false;
    }

    private boolean hasAnyLegalMoves(String player) {
        return !getAllLegalMoves(player).isEmpty();
    }

    public List<Move> getAllLegalMoves(String player) {
        List<Move> result = new ArrayList<>();
        for (int r1 = 0; r1 < 5; r1++) {
            for (int c1 = 0; c1 < 5; c1++) {
                Piece p = board[r1][c1];
                if (p != null && p.player.equals(player)) {
                    for (int r2 = 0; r2 < 5; r2++) {
                        for (int c2 = 0; c2 < 5; c2++) {
                            if (p.canMove(r2, c2)) {
                                Piece captured = board[r2][c2];
                                board[r2][c2] = p; board[r1][c1] = null;
                                int oldRow = p.row, oldCol = p.col;
                                p.row = r2; p.col = c2;

                                boolean stillInCheck = isLeaderInCheck(player);

                                p.row = oldRow; p.col = oldCol;
                                board[r1][c1] = p; board[r2][c2] = captured;

                                if (!stillInCheck) result.add(new Move(r1, c1, r2, c2));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public void makeAIMove() {
        List<Move> legalMoves = getAllLegalMoves("Player2");
        if (legalMoves.isEmpty()) {
            System.out.println("AI has no legal moves!");
            return;
        }
        Random rand = new Random();
        Move move = legalMoves.get(rand.nextInt(legalMoves.size()));
        Piece piece = board[move.fromRow][move.fromCol];
        Piece target = board[move.toRow][move.toCol];

        if (target != null) System.out.println("AI captured " + target.player + "'s " + target.name);

        board[move.toRow][move.toCol] = piece;
        board[move.fromRow][move.fromCol] = null;
        piece.row = move.toRow; piece.col = move.toCol;

        System.out.println("AI moved " + piece.name + " from (" + move.fromRow + "," + move.fromCol + ") to (" + move.toRow + "," + move.toCol + ")");
        displayBoard();
    }

    // Optional: keep save/load methods if needed
    public void saveToFile(String filename) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this);
            System.out.println("Game saved to " + filename);
        } catch (Exception e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }

    public static Board loadFromFile(String filename) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            return (Board) in.readObject();
        } catch (Exception e) {
            System.out.println("Failed to load game: " + e.getMessage());
            return null;
        }
    }
}
