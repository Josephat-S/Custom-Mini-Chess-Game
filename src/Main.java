package mini.chess.game;
import java.util.Scanner;
import Models.Board;



public class Main {
    public static void main(String[] args) {
        Board board = new Board();
        board.displayBoard();

        Scanner sc = new Scanner(System.in);
        String currentPlayer = "Player1";
        String winner = null;

        while (winner == null) {
            System.out.println(currentPlayer + ", enter move (fromRow fromCol toRow toCol):");
            int fromRow = sc.nextInt();
            int fromCol = sc.nextInt();
            int toRow = sc.nextInt();
            int toCol = sc.nextInt();

            boolean moved = board.movePiece(fromRow, fromCol, toRow, toCol);
            if (moved) {
                currentPlayer = currentPlayer.equals("Player1") ? "Player2" : "Player1";
            }

            winner = board.checkWinner();
        }

        System.out.println("\n=== GAME OVER ===");
        System.out.println(winner);
        sc.close();
    }
}
