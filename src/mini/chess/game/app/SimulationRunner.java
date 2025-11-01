package mini.chess.game.app;

import mini.chess.game.Models.AIPlayer;
import mini.chess.game.Models.Board;
import mini.chess.game.Models.Move;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SimulationRunner {
    public static void main(String[] args) {
        int games = 50;
        int p1Depth = 2;
        int p2Depth = 2;
        long seed = System.currentTimeMillis();
        try {
            if (args.length > 0) games = Integer.parseInt(args[0]);
            if (args.length > 1) p1Depth = Integer.parseInt(args[1]);
            if (args.length > 2) p2Depth = Integer.parseInt(args[2]);
            if (args.length > 3) seed = Long.parseLong(args[3]);
        } catch (Exception ignored) { }
        runBatch(games, p1Depth, p2Depth, seed, true);
    }

    public static void runInteractive(Scanner sc) {
        try {
            System.out.print("Number of games [50]: ");
            String g = sc.next();
            int games = g.trim().isEmpty() ? 50 : Integer.parseInt(g);

            System.out.print("Player1 depth (0-3) [2]: ");
            String d1 = sc.next();
            int p1Depth = d1.trim().isEmpty() ? 2 : Integer.parseInt(d1);

            System.out.print("Player2 depth (0-3) [2]: ");
            String d2 = sc.next();
            int p2Depth = d2.trim().isEmpty() ? 2 : Integer.parseInt(d2);

            System.out.print("Seed (enter for random): ");
            String s = sc.next();
            long seed = s.trim().isEmpty() ? System.currentTimeMillis() : Long.parseLong(s);

            runBatch(games, p1Depth, p2Depth, seed, false);
        } catch (Exception e) {
            System.out.println("Invalid input. Aborting simulation.");
        }
    }

    public static void runBatch(int games, int p1Depth, int p2Depth, long seed, boolean printEachGame) {
        int p1Wins = 0, p2Wins = 0, draws = 0;
        Random rng = new Random(seed);
        for (int i = 1; i <= games; i++) {
            String result = playSingleGame(p1Depth, p2Depth, rng.nextLong(), printEachGame);
            if (result.contains("Player1")) p1Wins++;
            else if (result.contains("Player2")) p2Wins++;
            else draws++;
        }
        System.out.println("=== Batch Results ===");
        System.out.println("Games: " + games + ", P1 depth=" + p1Depth + ", P2 depth=" + p2Depth + ", seed=" + seed);
        System.out.println("Player1 wins: " + p1Wins);
        System.out.println("Player2 wins: " + p2Wins);
        System.out.println("Draws: " + draws);
    }

    private static String playSingleGame(int p1Depth, int p2Depth, long seed, boolean printEachMove) {
        Board board = new Board();
        AIPlayer p1 = new AIPlayer(seed ^ 0x9E3779B97F4A7C15L);
        AIPlayer p2 = new AIPlayer(seed ^ 0xC3A5C85C97CB3127L);
        p1.setSearchDepth(p1Depth);
        p2.setSearchDepth(p2Depth);

        String current = "Player1";
        int moveLimit = 200; // avoid infinite loops
        int movesPlayed = 0;
        while (movesPlayed < moveLimit) {
            String winner = board.checkWinner();
            if (winner != null) {
                if (printEachMove) System.out.println("Winner: " + winner);
                return winner;
            }

            boolean moved;
            if (current.equals("Player1")) {
                moved = (p1Depth == 0) ? p1.makeRandomMove(board, current) : p1.makeBestMove(board, current);
            } else {
                moved = (p2Depth == 0) ? p2.makeRandomMove(board, current) : p2.makeBestMove(board, current);
            }

            if (!moved) {
                // No legal moves: determine status
                String status = board.checkStatus(current);
                if (status.equals("CHECKMATE")) {
                    String winnerText = current.equals("Player1") ? "Player2 wins!" : "Player1 wins!";
                    if (printEachMove) System.out.println("Winner: " + winnerText);
                    return winnerText;
                } else {
                    if (printEachMove) System.out.println("Draw by no legal moves.");
                    return "DRAW";
                }
            }

            current = current.equals("Player1") ? "Player2" : "Player1";
            movesPlayed++;
        }
        if (printEachMove) System.out.println("Draw by move limit.");
        return "DRAW";
    }
}
