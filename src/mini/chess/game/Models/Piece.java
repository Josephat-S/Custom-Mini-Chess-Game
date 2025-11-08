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

    // Added getter for player to support database layer expectations
    public String getPlayer() {
        return player;
    }

    // Added accessors so callers can safely mutate coordinates if needed
    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public abstract boolean canMove(int toRow, int toCol);
    public abstract String getSymbol();
}