/**
 * Represents the game board for the Mini Chess game.
 * Manages the 5x5 board state, piece movements, and game rules.
 */
package mini.chess.game.Models;

import java.util.InputMismatchException;
import java.util.ArrayList;
import java.util.List;

/**
 * Board class handles the game board state and game logic.
 * Includes methods for piece movement, game state validation,
 * and win condition checking.
 */
public class Board {
    /** The game board represented as a 5x5 grid of pieces */
    private Piece[][] board = new Piece[5][5];

    /**
     * Constructor initializes a new game board with starting piece positions.
     */
    public Board() {
        initializeBoard();
    }

    /**
     * Initializes the board with the starting positions of all pieces.
     * Places Leaders and Soldiers for both players in their initial positions.
     */
    public void initializeBoard() {
        board[4][2] = new Leader(4, 2, "Player1");
        board[3][1] = new Soldier(3, 1, "Player1");
        board[3][3] = new Soldier(3, 3, "Player1");

        board[0][2] = new Leader(0, 2, "Player2");
        board[1][1] = new Soldier(1, 1, "Player2");
        board[1][3] = new Soldier(1, 3, "Player2");
    }

    // ------------------------------
    // Display board with coordinates
    // ------------------------------
    public void displayBoard() {
        System.out.println("\n=== MINI CHESS BOARD ===");
        System.out.print("    ");
        for (int c = 0; c < 5; c++) {
            System.out.print(" " + c + "  ");
        }
        System.out.println();

        for (int i = 0; i < 5; i++) {
            System.out.print(" " + i + "  "); // row label
            for (int j = 0; j < 5; j++) {
                if (board[i][j] == null)
                    System.out.print("[ ] ");
                else
                    System.out.print("[" + board[i][j].getSymbol() + "] ");
            }
            System.out.println();
        }
        System.out.println("=========================");
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) throws ArrayIndexOutOfBoundsException, InputMismatchException {
        // Bounds validation
        if (fromRow < 0 || fromRow >= 5 || fromCol < 0 || fromCol >= 5 || toRow < 0 || toRow >= 5 || toCol < 0 || toCol >= 5) {
            System.out.println("Move out of bounds!");
            return false;
        }

        Piece piece = board[fromRow][fromCol];
        if (piece == null) {
            System.out.println("No piece at that position!");
            return false;
        }

        Piece target = board[toRow][toCol];
        // Cannot capture your own piece
        if (target != null && target.player.equals(piece.player)) {
            System.out.println("Cannot capture your own piece!");
            return false;
        }

        if (!piece.canMove(toRow, toCol)) {
            System.out.println("Invalid move for " + piece.name);
            return false;
        }

        if (target != null) {
            System.out.println(piece.player + " captured " + target.player + "'s " + target.name);
        }

        // Execute move
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;
        piece.row = toRow;
        piece.col = toCol;

        System.out.println(piece.player + " moved " + piece.name);
        displayBoard();
        return true;
    }

    public String checkWinner() {
        boolean player1Leader = false;
        boolean player2Leader = false;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Piece p = board[i][j];
                if (p != null && p instanceof Leader) {
                    if (p.player.equals("Player1")) player1Leader = true;
                    if (p.player.equals("Player2")) player2Leader = true;
                }
            }
        }

        if (!player1Leader) return "Player2 wins!";
        if (!player2Leader) return "Player1 wins!";
        return null;
    }

    // ================================
    // CHECK / CHECKMATE / DRAW LOGIC
    // ================================

    public String checkStatus(String currentPlayer) {
        String opponent = currentPlayer.equals("Player1") ? "Player2" : "Player1";
        boolean opponentLeaderInCheck = isLeaderInCheck(opponent);
        boolean opponentHasMoves = hasAnyLegalMoves(opponent);

        if (opponentLeaderInCheck && !opponentHasMoves) {
            return "CHECKMATE";
        } else if (!opponentLeaderInCheck && !opponentHasMoves) {
            return "DRAW";
        } else if (opponentLeaderInCheck) {
            return "CHECK";
        } else {
            return "NONE";
        }
    }

    // Check if a player's leader is currently under attack
    private boolean isLeaderInCheck(String player) {
        int leaderRow = -1, leaderCol = -1;

        // find the leader
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Piece p = board[r][c];
                if (p != null && p instanceof Leader && p.player.equals(player)) {
                    leaderRow = r;
                    leaderCol = c;
                }
            }
        }

        if (leaderRow == -1 || leaderCol == -1) return false; // no leader found

        // check all opponent pieces to see if they can capture the leader
        String opponent = player.equals("Player1") ? "Player2" : "Player1";
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Piece p = board[r][c];
                if (p != null && p.player.equals(opponent)) {
                    if (p.canMove(leaderRow, leaderCol)) {
                        return true; // leader is in check
                    }
                }
            }
        }
        return false;
    }

    // check if the player has any legal moves at all
    private boolean hasAnyLegalMoves(String player) {
        for (int r1 = 0; r1 < 5; r1++) {
            for (int c1 = 0; c1 < 5; c1++) {
                Piece p = board[r1][c1];
                if (p != null && p.player.equals(player)) {
                    for (int r2 = 0; r2 < 5; r2++) {
                        for (int c2 = 0; c2 < 5; c2++) {
                            // Skip squares occupied by own pieces
                            Piece dest = board[r2][c2];
                            if (dest != null && dest.player.equals(player)) continue;

                            if (p.canMove(r2, c2)) {
                                // temporarily simulate move
                                Piece temp = board[r2][c2];
                                int oldRow = p.row;
                                int oldCol = p.col;

                                board[r2][c2] = p;
                                board[r1][c1] = null;
                                p.row = r2;
                                p.col = c2;

                                boolean stillInCheck = isLeaderInCheck(player);

                                // undo move
                                board[r1][c1] = p;
                                board[r2][c2] = temp;
                                p.row = oldRow;
                                p.col = oldCol;

                                if (!stillInCheck) return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns all legal moves for the given player in the current position.
     * A move is considered legal if the piece's own canMove allows it and
     * executing it does not leave the player's Leader in check.
     */
    public List<Move> getAllLegalMoves(String player) {
        List<Move> result = new ArrayList<>();
        for (int r1 = 0; r1 < 5; r1++) {
            for (int c1 = 0; c1 < 5; c1++) {
                Piece p = board[r1][c1];
                if (p != null && p.player.equals(player)) {
                    for (int r2 = 0; r2 < 5; r2++) {
                        for (int c2 = 0; c2 < 5; c2++) {
                            if (p.canMove(r2, c2)) {
                                // simulate move
                                Piece captured = board[r2][c2];
                                board[r2][c2] = p;
                                board[r1][c1] = null;
                                int oldRow = p.row, oldCol = p.col;
                                p.row = r2; p.col = c2;

                                boolean stillInCheck = isLeaderInCheck(player);

                                // undo
                                p.row = oldRow; p.col = oldCol;
                                board[r1][c1] = p;
                                board[r2][c2] = captured;

                                if (!stillInCheck) {
                                    result.add(new Move(r1, c1, r2, c2));
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Simple, fast evaluation heuristic for a single legal move. Higher is better for 'player'.
     * Heuristic factors:
     * - Captures (Leader >> Soldier)
     * - Putting opponent Leader in check
     * - Small positional bonuses (advance Soldiers, centralize)
     * - Penalty if the moved piece can be legally recaptured immediately
     */
    public double evaluateMoveSimple(String player, Move m) {
        int fromRow = m.getFromRow();
        int fromCol = m.getFromCol();
        int toRow = m.getToRow();
        int toCol = m.getToCol();

        Piece piece = board[fromRow][fromCol];
        if (piece == null || !player.equals(piece.player)) return -1_000_000; // invalid input safety
        Piece capturedBefore = board[toRow][toCol];

        // simulate move
        Piece captured = board[toRow][toCol];
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;
        int oldRow = piece.row, oldCol = piece.col;
        piece.row = toRow; piece.col = toCol;

        double score = 0.0;

        // 1) capture value
        if (captured != null) {
            score += pieceValue(captured) * 10.0; // big incentive to capture
        }

        // 2) check bonus
        String opponent = player.equals("Player1") ? "Player2" : "Player1";
        if (isLeaderInCheck(opponent)) {
            score += 5.0;
        }

        // 3) simple development/positioning bonuses
        // centralization (closer to center (2,2))
        int distBefore = Math.abs(oldRow - 2) + Math.abs(oldCol - 2);
        int distAfter = Math.abs(toRow - 2) + Math.abs(toCol - 2);
        score += (distBefore - distAfter) * 0.2; // small bonus

        // Soldier advancement toward enemy side
        if (piece instanceof Soldier) {
            if (player.equals("Player1")) {
                score += (oldRow - toRow) * 0.5; // moving up reduces row index
            } else {
                score += (toRow - oldRow) * 0.5; // moving down increases row index
            }
        }

        // 4) immediate recapture risk: can opponent capture the landing square legally?
        boolean canBeRecaptured = false;
        outer:
        for (int r1 = 0; r1 < 5; r1++) {
            for (int c1 = 0; c1 < 5; c1++) {
                Piece opp = board[r1][c1];
                if (opp != null && opponent.equals(opp.player)) {
                    if (opp.canMove(toRow, toCol)) {
                        // simulate opponent capture and see if it's legal (their leader not in check)
                        Piece tmpCap = board[toRow][toCol]; // currently our moved piece
                        Piece tmpFrom = board[r1][c1];

                        board[toRow][toCol] = opp;
                        board[r1][c1] = null;
                        int oRow = opp.row, oCol = opp.col;
                        opp.row = toRow; opp.col = toCol;

                        boolean oppInCheck = isLeaderInCheck(opponent);

                        // undo opponent move
                        opp.row = oRow; opp.col = oCol;
                        board[r1][c1] = tmpFrom;
                        board[toRow][toCol] = tmpCap;

                        if (!oppInCheck) {
                            canBeRecaptured = true;
                            break outer;
                        }
                    }
                }
            }
        }

        if (canBeRecaptured) {
            // Heavier penalty if we moved the Leader into danger
            if (piece instanceof Leader) {
                score -= 100.0;
            } else {
                score -= 8.0;
            }
        }

        // undo our original simulation
        piece.row = oldRow; piece.col = oldCol;
        board[fromRow][fromCol] = piece;
        board[toRow][toCol] = capturedBefore;

        return score;
    }

    private int pieceValue(Piece p) {
        if (p == null) return 0;
        if (p instanceof Leader) return 100;
        if (p instanceof Soldier) return 10;
        return 5;
    }
}