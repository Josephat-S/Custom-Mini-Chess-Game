// Console entry point for the Mini Chess game.
package mini.chess.game.app;

import java.util.InputMismatchException;
import java.util.Scanner;
import mini.chess.game.Models.Board;
import mini.chess.game.Models.AIPlayer;

public class Main {
    // Starts the console game.
    public static void main(String[] args) {
        // Initialize the game board
        Board board = new Board();
        Scanner sc = new Scanner(System.in);

        // Game mode selection
        System.out.println("Select game mode:");
        System.out.println("1) Play vs Computer (AI)\n2) Play vs Human");
        int mode = 0;
        while (mode != 1 && mode != 2) {
            System.out.print("Enter 1 or 2: ");
            try {
                mode = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                mode = 0;
            }
        }

        boolean vsAI = (mode == 1);
        String aiSide = null; // "Player1" or "Player2" when vs AI
        AIPlayer ai = null;
        if (vsAI) {
            ai = new AIPlayer();
            System.out.println("Choose your side:");
            System.out.println("1) You play first (You = Player1)\n2) You play second (AI = Player1)");
            int side = 0;
            while (side != 1 && side != 2) {
                System.out.print("Enter 1 or 2: ");
                try {
                    side = Integer.parseInt(sc.nextLine().trim());
                } catch (NumberFormatException e) {
                    side = 0;
                }
            }
            aiSide = (side == 1) ? "Player2" : "Player1";
            System.out.println("You are " + (aiSide.equals("Player1") ? "Player2" : "Player1") + ". Good luck!");
        } else {
            System.out.println("Two-player mode selected. Player1 starts.");
        }

        // Show initial board
        board.displayBoard();

        String currentPlayer = "Player1";
        boolean gameOver = false;

        while (!gameOver) {
            try {
                if (vsAI && currentPlayer.equals(aiSide)) {
                    System.out.println("AI (" + currentPlayer + ") is thinking...");
                    boolean moved = ai.makeRandomMove(board, currentPlayer);
                    if (!moved) {
                        // No legal moves; fall through to status checks
                        System.out.println("AI has no legal moves.");
                    } else {
                        board.displayBoard();
                    }
                } else {
                    System.out.println(currentPlayer + ", enter move (fromRow fromCol toRow toCol):");
                    int fromRow = sc.nextInt();
                    int fromCol = sc.nextInt();
                    int toRow = sc.nextInt();
                    int toCol = sc.nextInt();

                    boolean moved = board.movePiece(fromRow, fromCol, toRow, toCol);
                    if (!moved) {
                        continue; // invalid move, retry same player
                    }
                    board.displayBoard();
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
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Invalid Move (Out Of Bound), try again!");
            } catch (InputMismatchException e) {
                System.out.println("Invalid input, Numbers Only!");
                sc.nextLine(); // clear invalid token(s)
            }
        }

        sc.close();
    }
}