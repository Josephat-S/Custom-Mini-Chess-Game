package Models;

public class Soldier extends Piece {
    public Soldier(int row, int col, String player) {
        super("Soldier", 'S', row, col, player);
    }

    @Override
    public boolean canMove(int newRow, int newCol) {
        if (player.equals("Player1")) {
            return newRow == row - 1 && newCol == col;
        } else {
            return newRow == row + 1 && newCol == col;
        }
    }
}
