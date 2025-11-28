package mini.chess.game.tests;

import mini.chess.game.Models.Board;
import mini.chess.game.Models.Piece;

public class BoardTest {

    public static boolean runTests() {
        System.out.println("Running BoardTest...");
        boolean passed = true;
        passed &= testInitialization();
        passed &= testMovePiece();
        passed &= testInvalidMove();
        return passed;
    }

    private static boolean testInitialization() {
        Board board = new Board();
        Piece p1Leader = board.getPieceAt(4, 2);
        if (p1Leader == null || !p1Leader.getSymbol().equals("L") || !p1Leader.getPlayer().equals("Player1")) {
            System.out.println("FAIL: Board initialization - Player1 Leader missing or incorrect.");
            return false;
        }
        
        Piece p2Leader = board.getPieceAt(0, 2);
        if (p2Leader == null || !p2Leader.getSymbol().equals("L") || !p2Leader.getPlayer().equals("Player2")) {
            System.out.println("FAIL: Board initialization - Player2 Leader missing or incorrect.");
            return false;
        }
        
        System.out.println("PASS: Board initialization.");
        return true;
    }

    private static boolean testMovePiece() {
        Board board = new Board();
        // Player 1 Soldier at (3, 1) moves to (2, 1) - Valid forward move
        boolean result = board.movePiece(3, 1, 2, 1);
        if (!result) {
            System.out.println("FAIL: Valid move rejected.");
            return false;
        }
        
        Piece moved = board.getPieceAt(2, 1);
        if (moved == null || !moved.getPlayer().equals("Player1")) {
            System.out.println("FAIL: Piece not found at destination.");
            return false;
        }
        
        if (board.getPieceAt(3, 1) != null) {
            System.out.println("FAIL: Piece still at source.");
            return false;
        }

        System.out.println("PASS: Move piece.");
        return true;
    }

    private static boolean testInvalidMove() {
        Board board = new Board();
        // Player 1 Soldier at (3, 1) tries to move to (3, 2) - Invalid sideways for Soldier (based on code)
        // Wait, code says Soldier only moves forward.
        boolean result = board.movePiece(3, 1, 3, 2);
        if (result) {
            System.out.println("FAIL: Invalid move accepted (Soldier sideways).");
            return false;
        }
        
        // Move to occupied square (friendly)
        // P1 Leader at (4,2), P1 Soldier at (3,3). Try move Leader to (3,3) - should fail
        // Wait, Leader moves 1 square. (4,2) to (3,3) is valid geometry (diagonal), but occupied by friendly.
        result = board.movePiece(4, 2, 3, 3);
        if (result) {
            System.out.println("FAIL: Friendly fire move accepted.");
            return false;
        }

        System.out.println("PASS: Invalid move.");
        return true;
    }
}
