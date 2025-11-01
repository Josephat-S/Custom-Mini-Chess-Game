package mini.chess.game.Models;

import java.util.List;
import java.util.Random;

/**
 * Simple AI player using minimax with alpha-beta pruning.
 * Depth 0 falls back to random moves.
 */
public class AIPlayer {
    private int searchDepth = 2;
    private final Random rng;

    public AIPlayer() {
        this(System.currentTimeMillis());
    }

    public AIPlayer(long seed) {
        this.rng = new Random(seed);
    }

    public void setSearchDepth(int depth) {
        if (depth < 0) depth = 0;
        if (depth > 5) depth = 5; // keep it small for 5x5 board
        this.searchDepth = depth;
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    public Move chooseRandomMove(Board board, String player) {
        List<Move> moves = board.getAllLegalMoves(player);
        if (moves.isEmpty()) return null;
        return moves.get(rng.nextInt(moves.size()));
    }

    public boolean makeRandomMove(Board board, String player) {
        Move m = chooseRandomMove(board, player);
        if (m == null) return false;
        board.applyAndAnnounceMove(m, player, "AI");
        return true;
    }

    public Move chooseBestMove(Board board, String player) {
        if (searchDepth == 0) return chooseRandomMove(board, player);
        List<Move> moves = board.getAllLegalMoves(player);
        if (moves.isEmpty()) return null;

        String opponent = player.equals("Player1") ? "Player2" : "Player1";
        double bestScore = -Double.MAX_VALUE;
        Move bestMove = null;

        // Small random jitter to diversify equal-score choices
        double epsilon = 1e-6;
        for (Move m : moves) {
            Board.AppliedMove am = board.applyMoveSilently(m);
            double score = minimax(board, searchDepth - 1, -Double.MAX_VALUE, Double.MAX_VALUE, false, player, opponent);
            board.undoMoveSilently(am);

            // add tiny random noise for tie-breaking
            score += (rng.nextDouble() - 0.5) * epsilon;
            if (score > bestScore) {
                bestScore = score;
                bestMove = m;
            }
        }
        return bestMove;
    }

    public boolean makeBestMove(Board board, String player) {
        Move m = chooseBestMove(board, player);
        if (m == null) return false;
        board.applyAndAnnounceMove(m, player, "AI");
        return true;
    }

    private double minimax(Board board, int depth, double alpha, double beta, boolean maximizing,
                           String me, String opponent) {
        // Terminal checks
        String winner = board.checkWinner();
        if (winner != null) {
            if (winner.contains("Player1") && me.equals("Player1")) return 10_000;
            if (winner.contains("Player2") && me.equals("Player2")) return 10_000;
            return -10_000;
        }
        if (depth == 0) return evaluate(board, me, opponent);

        String current = maximizing ? me : opponent;
        List<Move> moves = board.getAllLegalMoves(current);
        if (moves.isEmpty()) {
            // No moves: checkmate or draw is handled by evaluate/status
            return evaluate(board, me, opponent);
        }

        if (maximizing) {
            double value = -Double.MAX_VALUE;
            for (Move m : moves) {
                Board.AppliedMove am = board.applyMoveSilently(m);
                value = Math.max(value, minimax(board, depth - 1, alpha, beta, false, me, opponent));
                board.undoMoveSilently(am);
                alpha = Math.max(alpha, value);
                if (alpha >= beta) break; // beta cut-off
            }
            return value;
        } else {
            double value = Double.MAX_VALUE;
            for (Move m : moves) {
                Board.AppliedMove am = board.applyMoveSilently(m);
                value = Math.min(value, minimax(board, depth - 1, alpha, beta, true, me, opponent));
                board.undoMoveSilently(am);
                beta = Math.min(beta, value);
                if (alpha >= beta) break; // alpha cut-off
            }
            return value;
        }
    }

    private double evaluate(Board board, String me, String opponent) {
        // Material
        int myMaterial = 0, oppMaterial = 0;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Piece p = board.getPieceAt(r, c);
                if (p == null) continue;
                int val = (p instanceof Leader) ? 100 : (p instanceof Soldier ? 3 : 1);
                if (p.player.equals(me)) myMaterial += val; else oppMaterial += val;
            }
        }
        double score = (myMaterial - oppMaterial);

        // Mobility
        int myMoves = board.getAllLegalMoves(me).size();
        int oppMoves = board.getAllLegalMoves(opponent).size();
        score += 0.1 * (myMoves - oppMoves);

        // Checks
        if (board.isLeaderInCheck(opponent)) score += 0.5;
        if (board.isLeaderInCheck(me)) score -= 0.5;

        return score;
    }
}
