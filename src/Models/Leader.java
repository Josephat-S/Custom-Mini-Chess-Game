package Models;

public class Leader extends Piece {
    public Leader(int row, int col, String player) {
        super("Leader", 'L', row, col, player);
    }

    @Override
    public boolean canMove(int newRow, int newCol) {
        int rowDiff = Math.abs(newRow - row);
        int colDiff = Math.abs(newCol - col);
        return rowDiff <= 1 && colDiff <= 1;
    }
}
