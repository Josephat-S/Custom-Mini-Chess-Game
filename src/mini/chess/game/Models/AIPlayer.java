// Strategic CPU opponent with minimax (alpha-beta) and random fallback.
package mini.chess.game.Models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AIPlayer {
    private final Random rng;
    private int searchDepth = 2; // default depth

    // Non-deterministic RNG.
    public AIPlayer() {
        this(new Random());
    }

    /**
     * Creates an AI player with an injectable {@link Random} for reproducible tests.
     */
    public AIPlayer(Random rng) {
        this.rng = rng;
    }

    /** Configure search depth (>=0). Depth 0 falls back to evaluation only. */
    public void setSearchDepth(int depth) {
        if (depth < 0) depth = 0;
        this.searchDepth = depth;
    }

    public int getSearchDepth() { return searchDepth; }

    /**
     * Chooses a random legal move for the given player on the given board.
     *
     * @param board  current game board
     * @param player the player side to move ("Player1" or "Player2")
     * @return a randomly chosen {@link Move}, or null if there are no legal moves
     */
    public Move chooseRandomMove(Board board, String player) {
        List<Move> legal = board.getAllLegalMoves(player);
        if (legal.isEmpty()) return null;
        int idx = rng.nextInt(legal.size());
        return legal.get(idx);
    }

    /**
     * Chooses a strategic move using minimax with alpha-beta pruning.
     * Falls back to a random move if no move scores better deterministically.
     */
    public Move chooseBestMove(Board board, String player) {
        List<Move> legal = board.getAllLegalMoves(player);
        if (legal.isEmpty()) return null;
        if (searchDepth <= 0) {
            // pick by static eval tie-broken randomly
            return pickByEvaluation(board, player, legal);
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        List<Move> bestMoves = new ArrayList<>();
        String opponent = opponentOf(player);

        // Randomize move order slightly to diversify play when scores tie
        Collections.shuffle(legal, rng);

        for (Move m : legal) {
            Board.AppliedMove am = board.applyMoveSilently(m);
            double score = minimax(board, searchDepth - 1, false, opponent, player, -1_000_000, 1_000_000);
            board.undoMoveSilently(am);
            if (score > bestScore + 1e-9) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(m);
            } else if (Math.abs(score - bestScore) <= 1e-9) {
                bestMoves.add(m);
            }
        }

        if (bestMoves.isEmpty()) {
            return chooseRandomMove(board, player);
        }
        return bestMoves.get(rng.nextInt(bestMoves.size()));
    }

    /** Executes the strategic best move if available, returns false if no legal moves. */
    public boolean makeBestMove(Board board, String player) {
        Move m = chooseBestMove(board, player);
        if (m == null) return false;
        return board.movePiece(m.getFromRow(), m.getFromCol(), m.getToRow(), m.getToCol());
    }

    /**
     * Basic alpha-beta minimax.
     * @param board game state
     * @param depth remaining depth
     * @param maximizing true if it's AI (rootPlayer) turn, false for opponent
     * @param sideToMove whose turn string
     * @param rootPlayer the AI player's side
     */
    private double minimax(Board board, int depth, boolean maximizing, String sideToMove, String rootPlayer,
                           double alpha, double beta) {
        // Terminal checks
        List<Move> legal = board.getAllLegalMoves(sideToMove);
        if (depth == 0 || legal.isEmpty()) {
            return evaluate(board, rootPlayer);
        }

        if (maximizing) {
            double best = Double.NEGATIVE_INFINITY;
            Collections.shuffle(legal, rng);
            for (Move m : legal) {
                Board.AppliedMove am = board.applyMoveSilently(m);
                double val = minimax(board, depth - 1, false, opponentOf(sideToMove), rootPlayer, alpha, beta);
                board.undoMoveSilently(am);
                if (val > best) best = val;
                if (val > alpha) alpha = val;
                if (beta <= alpha) break; // beta cut-off
            }
            return best;
        } else {
            double best = Double.POSITIVE_INFINITY;
            Collections.shuffle(legal, rng);
            for (Move m : legal) {
                Board.AppliedMove am = board.applyMoveSilently(m);
                double val = minimax(board, depth - 1, true, opponentOf(sideToMove), rootPlayer, alpha, beta);
                board.undoMoveSilently(am);
                if (val < best) best = val;
                if (val < beta) beta = val;
                if (beta <= alpha) break; // alpha cut-off
            }
            return best;
        }
    }

    private String opponentOf(String player) {
        return player.equals("Player1") ? "Player2" : "Player1";
    }

    private Move pickByEvaluation(Board board, String player, List<Move> legal) {
        // rank moves by resulting evaluation
        List<MoveScore> ranked = new ArrayList<>();
        String opponent = opponentOf(player);
        for (Move m : legal) {
            Board.AppliedMove am = board.applyMoveSilently(m);
            double score = minimax(board, 0, false, opponent, player, -1_000_000, 1_000_000); // evaluate leaf
            board.undoMoveSilently(am);
            ranked.add(new MoveScore(m, score));
        }
        ranked.sort(Comparator.comparingDouble(ms -> -ms.score));
        double top = ranked.get(0).score;
        // keep all near-top moves within small epsilon
        List<Move> best = new ArrayList<>();
        for (MoveScore ms : ranked) {
            if (Math.abs(ms.score - top) <= 1e-9) best.add(ms.move);
        }
        return best.get(rng.nextInt(best.size()));
    }

    private static class MoveScore {
        final Move move; final double score;
        MoveScore(Move m, double s) { this.move = m; this.score = s; }
    }

    /**
     * Static evaluation of the board from the perspective of rootPlayer.
     * Positive is good for rootPlayer, negative is bad.
     */
    private double evaluate(Board board, String rootPlayer) {
        // Material values
        int p1Material = 0, p2Material = 0, p1Leaders = 0, p2Leaders = 0, p1Soldiers = 0, p2Soldiers = 0;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Piece p = board.getPieceAt(r, c);
                if (p == null) continue;
                int val = (p instanceof Leader) ? 100 : 3; // Leader high value, Soldier small
                if (p.player.equals("Player1")) {
                    p1Material += val;
                    if (p instanceof Leader) p1Leaders++; else p1Soldiers++;
                } else {
                    p2Material += val;
                    if (p instanceof Leader) p2Leaders++; else p2Soldiers++;
                }
            }
        }

        // Terminal leader captured bonus/penalty
        if (p1Leaders == 0 && p2Leaders > 0) return rootPlayer.equals("Player1") ? -10_000 : 10_000;
        if (p2Leaders == 0 && p1Leaders > 0) return rootPlayer.equals("Player2") ? -10_000 : 10_000;
        if (p1Leaders == 0 && p2Leaders == 0) return 0; // bizarre, but neutral

        // Mobility
        int p1Mob = board.getAllLegalMoves("Player1").size();
        int p2Mob = board.getAllLegalMoves("Player2").size();

        // Checks
        boolean p1InCheck = board.isLeaderInCheck("Player1");
        boolean p2InCheck = board.isLeaderInCheck("Player2");

        int materialScore = p1Material - p2Material;
        int mobilityScore = (p1Mob - p2Mob);
        int checkScore = (p2InCheck ? 2 : 0) - (p1InCheck ? 2 : 0);

        int totalForP1 = materialScore + mobilityScore + checkScore;
        int totalForP2 = -totalForP1;

        return rootPlayer.equals("Player1") ? totalForP1 : totalForP2;
    }

    /**
     * Chooses and executes a random legal move for the given player.
     *
     * @param board  current game board
     * @param player the player side to move
     * @return true if a move was made; false if no legal moves exist
     */
    public boolean makeRandomMove(Board board, String player) {
        Move m = chooseRandomMove(board, player);
        if (m == null) return false;
        try {
            return board.movePiece(m.getFromRow(), m.getFromCol(), m.getToRow(), m.getToCol());
        } catch (RuntimeException ex) {
            // Should not happen because we picked from legal moves, but guard anyway.
            System.err.println("AI move failed: " + ex.getMessage());
            return false;
        }
    }
}
