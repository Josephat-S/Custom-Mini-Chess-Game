package mini.chess.game.Models;

import java.io.Serializable;

/**
 * Represents a single move from (fromRow, fromCol) to (toRow, toCol)
 */
public class Move implements Serializable {
    private static final long serialVersionUID = 1L;
    public int fromRow, fromCol, toRow, toCol;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }
}
