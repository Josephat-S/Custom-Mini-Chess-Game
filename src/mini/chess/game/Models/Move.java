package mini.chess.game.Models;

/**
 * Simple value object representing a move on the 5x5 mini‑chess board.
 */
public class Move {
    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    public int getFromRow() { return fromRow; }
    public int getFromCol() { return fromCol; }
    public int getToRow() { return toRow; }
    public int getToCol() { return toCol; }

    @Override
    public String toString() {
        return "(" + fromRow + "," + fromCol + ") -> (" + toRow + "," + toCol + ")";
    }
}