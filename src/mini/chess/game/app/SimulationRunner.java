package mini.chess.game.app;

import mini.chess.game.Models.AIPlayer;
import mini.chess.game.Models.Board;

import java.util.Random;

/**
 * Runs bulk AI-vs-AI matches to evaluate AI performance.
 *
 * Usage:
 *   java mini.chess.game.app.SimulationRunner [games] [p1Depth] [p2Depth] [seed]
 * Defaults: games=50, p1Depth=2, p2Depth=2, seed=random
 */
public class SimulationRunner {
    public static void main(String[] args) {
        int games = args.length > 0 ? parseInt(args[0], 50) : 50;
        int p1Depth = args.length > 1 ? parseInt(args[1], 2) : 2;
        int p2Depth = args.length > 2 ? parseInt(args[2], 2) : 2;
        Long seed = args.length > 3 ? parseLong(args[3], null) : null;

        Random rng = seed == null ? new Random() : new Random(seed);

        int p1Wins = 0, p2Wins = 0, draws = 0;
        for (int g = 1; g <= games; g++) {
            Board board = new Board();
            AIPlayer p1 = new AIPlayer(new Random(rng.nextLong()));
            AIPlayer p2 = new AIPlayer(new Random(rng.nextLong()));
            p1.setSearchDepth(p1Depth);
            p2.setSearchDepth(p2Depth);

            String current = "Player1";
            int turn = 0;
            final int maxTurns = 200; // safety cap
            boolean gameOver = false;

            while (!gameOver && turn < maxTurns) {
                boolean moved;
                if (current.equals("Player1")) {
                    moved = (p1.getSearchDepth() <= 0) ? p1.makeRandomMove(board, current) : p1.makeBestMove(board, current);
                } else {
                    moved = (p2.getSearchDepth() <= 0) ? p2.makeRandomMove(board, current) : p2.makeBestMove(board, current);
                }

                // Winner by Leader capture?
                String winner = board.checkWinner();
                if (winner != null) {
                    if (winner.contains("Player1")) p1Wins++; else p2Wins++;
                    gameOver = true;
                    break;
                }

                // Status based results
                String status = board.checkStatus(current);
                if ("CHECKMATE".equals(status)) {
                    // current player just delivered checkmate
                    if (current.equals("Player1")) p1Wins++; else p2Wins++;
                    gameOver = true;
                    break;
                } else if ("DRAW".equals(status)) {
                    draws++;
                    gameOver = true;
                    break;
                }

                // no move available (should be captured by status), but guard
                if (!moved) {
                    // Consider this a draw in simulation context
                    draws++;
                    gameOver = true;
                    break;
                }

                current = current.equals("Player1") ? "Player2" : "Player1";
                turn++;
            }

            if (!gameOver) {
                // Reached turn cap, count as draw
                draws++;
            }
        }

        System.out.println("=== Simulation Results ===");
        System.out.println("Games: " + games + ", P1 depth=" + p1Depth + ", P2 depth=" + p2Depth + (seed == null ? "" : ", seed=" + seed));
        System.out.println("Player1 wins: " + p1Wins);
        System.out.println("Player2 wins: " + p2Wins);
        System.out.println("Draws: " + draws);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
    private static Long parseLong(String s, Long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }
}
