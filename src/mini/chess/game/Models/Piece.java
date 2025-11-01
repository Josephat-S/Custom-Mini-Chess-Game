package mini.chess.game.Models;

import java.io.Serializable;

public abstract class Piece implements Serializable {
    private static final long serialVersionUID = 1L;

    public int row, col;
    public String player;
    public String name;

    public Piece(int row, int col, String player, String name) {
        this.row = row;
        this.col = col;
        this.player = player;
        this.name = name;
    }

    public abstract boolean canMove(int toRow, int toCol);
    public abstract String getSymbol();
}
