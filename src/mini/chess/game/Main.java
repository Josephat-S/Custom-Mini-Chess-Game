package mini.chess.game;

import java.util.Scanner;
import Models.Board;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();
        board.displayBoard();

        Scanner sc = new Scanner(System.in);
        String currentPlayer = "Player1";
        boolean gameOver = false;

        while (!gameOver) {
            System.out.println(currentPlayer + ", enter move (fromRow fromCol toRow toCol):");
            int fromRow = sc.nextInt();
            int fromCol = sc.nextInt();
            int toRow = sc.nextInt();
            int toCol = sc.nextInt();

            boolean moved = board.movePiece(fromRow, fromCol, toRow, toCol);
            if (!moved) {
                continue; // invalid move, retry same player
            }

            // Check if someone’s leader got captured
            String winner = board.checkWinner();
            if (winner != null) {
                System.out.println("\n=== GAME OVER ===");
                System.out.println(winner);
                break;
            }

            // Check game status (Check, Checkmate, Draw)
            String status = board.checkStatus(currentPlayer);

            switch (status) {
                case "CHECK":
                    System.out.println("⚠️  CHECK! " +
                            (currentPlayer.equals("Player1") ? "Player2" : "Player1") +
                            "'s Leader is under threat!");
                    break;

                case "CHECKMATE":
                    System.out.println("\n=== GAME OVER ===");
                    System.out.println("♛ CHECKMATE! " + currentPlayer + " wins!");
                    gameOver = true;
                    break;

                case "DRAW":
                    System.out.println("\n=== GAME OVER ===");
                    System.out.println("🤝 DRAW! No legal moves remain.");
                    gameOver = true;
                    break;
            }

            // Switch player if game not finished
            if (!gameOver) {
                currentPlayer = currentPlayer.equals("Player1") ? "Player2" : "Player1";
            }
        }

        sc.close();
    }
}
