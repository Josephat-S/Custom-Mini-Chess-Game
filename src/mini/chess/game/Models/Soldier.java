package mini.chess.game.Models;

public class Soldier extends Piece {
    private static final long serialVersionUID = 1L;

    public Soldier(int row, int col, String player) {
        super(row, col, player, "Soldier");
    }

    @Override
    public boolean canMove(int toRow, int toCol) {
        if (player.equals("Player1")) return row - 1 == toRow && col == toCol;
        else return row + 1 == toRow && col == toCol;
    }

    @Override
    public String getSymbol() { return "S"; }
}
