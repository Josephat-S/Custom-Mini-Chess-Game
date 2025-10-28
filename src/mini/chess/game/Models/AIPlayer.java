// Simple CPU opponent that picks a random legal move.
package mini.chess.game.Models;

import java.util.List;
import java.util.Random;

public class AIPlayer {
    private final Random rng;

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
