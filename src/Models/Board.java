package Models;

import java.util.InputMismatchException;

public class Board {
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
        if (target != null) {
            System.out.println(piece.player + " captured " + target.player + "'s " + target.name);
        }

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
                            if (p.canMove(r2, c2)) {
                                // temporarily simulate move
                                Piece temp = board[r2][c2];
                                board[r2][c2] = p;
                                board[r1][c1] = null;

                                boolean stillInCheck = isLeaderInCheck(player);

                                // undo move
                                board[r1][c1] = p;
                                board[r2][c2] = temp;

                                if (!stillInCheck) return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
