package mini.chess.game.tests;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("   Mini Chess Game Unit Tests    ");
        System.out.println("=================================");
        
        boolean allPassed = true;
        
        allPassed &= BoardTest.runTests();
        System.out.println("---------------------------------");
        
        allPassed &= PieceTest.runTests();
        System.out.println("---------------------------------");
        
        allPassed &= GameTest.runTests();
        System.out.println("---------------------------------");
        
        if (allPassed) {
            System.out.println("✅ ALL TESTS PASSED");
        } else {
            System.out.println("❌ SOME TESTS FAILED");
            System.exit(1);
        }
    }
}
