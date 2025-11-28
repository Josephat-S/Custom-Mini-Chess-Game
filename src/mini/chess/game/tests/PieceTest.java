package mini.chess.game.tests;

import mini.chess.game.Models.Leader;
import mini.chess.game.Models.Soldier;

public class PieceTest {

    public static boolean runTests() {
        System.out.println("Running PieceTest...");
        boolean passed = true;
        passed &= testLeaderMovement();
        passed &= testSoldierMovement();
        return passed;
    }

    private static boolean testLeaderMovement() {
        Leader leader = new Leader(2, 2, "Player1");
        
        // Valid moves (1 square radius)
        if (!leader.canMove(1, 1)) return fail("Leader should move diagonal");
        if (!leader.canMove(1, 2)) return fail("Leader should move up");
        if (!leader.canMove(1, 3)) return fail("Leader should move diagonal");
        if (!leader.canMove(2, 1)) return fail("Leader should move left");
        if (!leader.canMove(2, 3)) return fail("Leader should move right");
        if (!leader.canMove(3, 1)) return fail("Leader should move diagonal");
        if (!leader.canMove(3, 2)) return fail("Leader should move down");
        if (!leader.canMove(3, 3)) return fail("Leader should move diagonal");

        // Invalid moves
        if (leader.canMove(0, 0)) return fail("Leader cannot move 2 squares");
        if (leader.canMove(2, 4)) return fail("Leader cannot move 2 squares");
        
        System.out.println("PASS: Leader movement.");
        return true;
    }

    private static boolean testSoldierMovement() {
        // Player 1 Soldier (moves UP, row decreases)
        Soldier p1Soldier = new Soldier(3, 3, "Player1");
        if (!p1Soldier.canMove(2, 3)) return fail("P1 Soldier should move up (row - 1)");
        if (p1Soldier.canMove(4, 3)) return fail("P1 Soldier cannot move backwards");
        if (p1Soldier.canMove(3, 2)) return fail("P1 Soldier cannot move sideways (per code)");
        if (p1Soldier.canMove(2, 2)) return fail("P1 Soldier cannot move diagonal");

        // Player 2 Soldier (moves DOWN, row increases)
        Soldier p2Soldier = new Soldier(1, 1, "Player2");
        if (!p2Soldier.canMove(2, 1)) return fail("P2 Soldier should move down (row + 1)");
        if (p2Soldier.canMove(0, 1)) return fail("P2 Soldier cannot move backwards");

        System.out.println("PASS: Soldier movement.");
        return true;
    }

    private static boolean fail(String msg) {
        System.out.println("FAIL: " + msg);
        return false;
    }
}
