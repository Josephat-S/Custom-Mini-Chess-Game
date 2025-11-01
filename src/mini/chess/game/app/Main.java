package mini.chess.game;

import java.util.InputMismatchException;
import java.util.Scanner;
import mini.chess.game.Models.Board;

class main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        boolean exitGame = false;

        while (!exitGame) {
            System.out.println("\n=== MINI CHESS GAME ===");
            System.out.println("1. New Game");
            System.out.println("2. Load Saved Game");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int choice = 0;
            try { choice = sc.nextInt(); }
            catch (InputMismatchException e) { System.out.println("Numbers only!"); sc.nextLine(); continue; }

            switch (choice) {
                case 1 -> startNewGame(sc, null);
                case 2 -> loadSavedGame(sc);
                case 3 -> { System.out.println("Exiting..."); exitGame = true; }
                default -> System.out.println("Invalid choice!");
            }
        }
        sc.close();
    }

    private static void startNewGame(Scanner sc, Board loadedBoard) {
        int opponentChoice = 1; // default Human vs Human
        if (loadedBoard == null) {
            System.out.println("\nSelect opponent type:");
            System.out.println("1. Human vs Human");
            System.out.println("2. Human vs AI");
            System.out.print("Choose an option: ");
            try { opponentChoice = sc.nextInt(); }
            catch (InputMismatchException e) { System.out.println("Numbers only!"); sc.nextLine(); return; }
        }

        Board board = (loadedBoard != null) ? loadedBoard : new Board();
        board.displayBoard();
        String currentPlayer = "Player1";
        boolean gameOver = false;

        while (!gameOver) {
            try {
                if (opponentChoice == 2 && currentPlayer.equals("Player2")) {
                    board.makeAIMove();
                } else {
                    System.out.println(currentPlayer + ", enter move (fromRow fromCol toRow toCol):");
                    int fromRow = sc.nextInt();
                    int fromCol = sc.nextInt();
                    int toRow = sc.nextInt();
                    int toCol = sc.nextInt();

                    boolean moved = board.movePiece(fromRow, fromCol, toRow, toCol);
                    if (!moved) continue;
                }

                String winner = board.checkWinner();
                if (winner != null) { System.out.println("\n=== GAME OVER ===\n" + winner); break; }

                String status = board.checkStatus(currentPlayer);
                switch (status) {
                    case "CHECK" -> System.out.println("⚠️  CHECK! " + (currentPlayer.equals("Player1") ? "Player2" : "Player1") + "'s Leader is under threat!");
                    case "CHECKMATE" -> { System.out.println("\n=== GAME OVER ===\n♛ CHECKMATE! " + currentPlayer + " wins!"); gameOver = true; }
                    case "DRAW" -> { System.out.println("\n=== GAME OVER ===\n🤝 DRAW! No legal moves remain."); gameOver = true; }
                }

                if (!gameOver) currentPlayer = currentPlayer.equals("Player1") ? "Player2" : "Player1";

            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Invalid Move (Out Of Bound)!");
            } catch (InputMismatchException e) {
                System.out.println("Numbers only!");
                sc.nextLine();
            }
        }
    }

    private static void loadSavedGame(Scanner sc) {
        Board loaded = Board.loadFromFile("saved_game.dat");
        if (loaded != null) {
            System.out.println("Loaded saved game:");
            startNewGame(sc, loaded);
        }
    }
}
