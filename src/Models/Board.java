package Models;

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

    public void displayBoard() {
        System.out.println("\n=== MINI CHESS BOARD ===");
        for (int i = 0; i < 5; i++) {
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
}
