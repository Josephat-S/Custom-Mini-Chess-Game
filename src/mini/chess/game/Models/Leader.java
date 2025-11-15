package mini.chess.game.Models;

public class Leader extends Piece {
    private static final long serialVersionUID = 1L;

    public Leader(int row, int col, String player) {
        super(row, col, player, "Leader");
    }

    @Override
    public boolean canMove(int toRow, int toCol) {
        // Prevent no-op move
        if (toRow == row && toCol == col) return false;

        int dr = Math.abs(toRow - row);
        int dc = Math.abs(toCol - col);

        // Must be within one square in any direction (orthogonal or diagonal)
        if (dr > 1 || dc > 1) return false;

        // Board bounds (5x5)
        return toRow >= 0 && toRow < 5 && toCol >= 0 && toCol < 5;
    }

    @Override
    public String getSymbol() {return"L";}
}