package mini.chess.game.Models;

public class Leader extends Piece {
    private static final long serialVersionUID = 1L;

    public Leader(int row, int col, String player) {
        super(row, col, player, "Leader");
    }

    @Override
    public boolean canMove(int toRow, int toCol) {
        return Math.abs(toRow - row) <= 1 && Math.abs(toCol - col) <= 1;
    }

    @Override
    public String getSymbol() { return "L"; }
}
