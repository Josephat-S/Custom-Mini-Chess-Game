/**
 * Main entry point for the Mini Chess Game application.
 * This package contains the main game loop and core game mechanics.
 */
package mini.chess.game;

import java.util.InputMismatchException;
import java.util.Scanner;
import mini.chess.game.Models.Board;

/**
 * Main class that handles the game loop and player interactions.
 * Manages the game flow, player turns, and game state.
 */
public class Main {
    /**
     * Main game loop that handles player moves and game state.
     * Manages turn-based gameplay, move validation, and game ending conditions.
     *
     * @param args Command line arguments (not used)
     */
    static void main(String[] args) {
        // Initialize the game board
        Board board = new Board();
        board.displayBoard();

        Scanner sc = new Scanner(System.in);
        String currentPlayer = "Player1";
        boolean gameOver = false;

        while (!gameOver) {
           try{
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
           catch (ArrayIndexOutOfBoundsException e){
               System.out.println("Invalid Move (Out Of Bound), try again!");
           }
           catch (InputMismatchException e){
               System.out.println("Invalid input, Numbers Only!");
           }
        }

        sc.close();
    }
}
