/**
 * Represents the Soldier piece in the Mini Chess game.
 * The Soldier is similar to a Pawn in traditional chess, but with simplified movement.
 */
package mini.chess.game.Models;

/**
 * Soldier class implements the movement rules and behavior for the Soldier piece.
 * Soldiers can only move forward one square (direction depends on the player).
 */
public class Soldier extends Piece {
    /**
     * Creates a new Soldier piece at the specified position.
     *
     * @param row    The initial row position
     * @param col    The initial column position
     * @param player The player who owns this Soldier
     */
    public Soldier(int row, int col, String player) {
        super("Soldier", 'S', row, col, player);
    }

    /**
     * Checks if the Soldier can move to the specified position.
     * Soldiers can only move forward one square (up for Player2, down for Player1).
     *
     * @param newRow Target row position
     * @param newCol Target column position
     * @return true if the move is valid (one square forward), false otherwise
     */
    @Override
    public boolean canMove(int newRow, int newCol) {
        if (player.equals("Player1")) {
            return newRow == row - 1 && newCol == col;
        } else {
            return newRow == row + 1 && newCol == col;
        }
    }
}
