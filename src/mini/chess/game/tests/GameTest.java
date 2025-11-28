package mini.chess.game.tests;

import mini.chess.game.Models.Board;
import mini.chess.game.Models.Leader;
import mini.chess.game.Models.Piece;

public class GameTest {

    public static boolean runTests() {
        System.out.println("Running GameTest...");
        boolean passed = true;
        passed &= testCheckWinner();
        passed &= testLeaderInCheck();
        return passed;
    }

    private static boolean testCheckWinner() {
        Board board = new Board();
        // Initial state: both leaders present
        if (board.checkWinner() != null) return fail("Game should not have winner initially");

        // Remove Player 2 Leader
        board.setPiece(0, 2, null);
        String winner = board.checkWinner();
        if (winner == null || !winner.contains("Player1")) return fail("Player 1 should win when P2 Leader is gone");

        System.out.println("PASS: Check winner.");
        return true;
    }
    
    private static boolean testLeaderInCheck() {
        Board board = new Board();
        board.clear();
        
        // Setup: P1 Leader at (2,2), P2 Leader at (2,3) -> P1 Leader is in check (Leader attacks 1 sq radius)
        board.setPiece(2, 2, new Leader(2, 2, "Player1"));
        board.setPiece(2, 3, new Leader(2, 3, "Player2"));
        
        if (!board.isLeaderInCheck("Player1")) return fail("Player 1 Leader should be in check");
        
        // Move P2 Leader away
        board.setPiece(2, 3, null);
        board.setPiece(0, 0, new Leader(0, 0, "Player2"));
        
        if (board.isLeaderInCheck("Player1")) return fail("Player 1 Leader should NOT be in check");
        
        System.out.println("PASS: Leader in check.");
        return true;
    }

    private static boolean fail(String msg) {
        System.out.println("FAIL: " + msg);
        return false;
    }
}
