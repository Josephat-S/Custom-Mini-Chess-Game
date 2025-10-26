/**
 * Represents the Leader piece in the Mini Chess game.
 * The Leader is the most important piece, similar to a King in traditional chess.
 */
package mini.chess.game.Models;

/**
 * Leader class implements the movement rules and behavior for the Leader piece.
 * The Leader can move one square in any direction (horizontally, vertically, or diagonally).
 */
public class Leader extends Piece {
    /**
     * Creates a new Leader piece at the specified position.
     *
     * @param row    The initial row position
     * @param col    The initial column position
     * @param player The player who owns this Leader
     */
    public Leader(int row, int col, String player) {
        super("Leader", 'L', row, col, player);
    }

    /**
     * Checks if the Leader can move to the specified position.
     * Leaders can move one square in any direction.
     *
     * @param newRow Target row position
     * @param newCol Target column position
     * @return true if the move is valid (one square in any direction), false otherwise
     */
    @Override
    public boolean canMove(int newRow, int newCol) {
        int rowDiff = Math.abs(newRow - row);
        int colDiff = Math.abs(newCol - col);
        return rowDiff <= 1 && colDiff <= 1;
    }
}
