package Models;

public abstract class Piece {
    protected String name;
    protected char symbol;
    protected int row;
    protected int col;
    protected String player;

    public Piece(String name, char symbol, int row, int col, String player) {
        this.name = name;
        this.symbol = symbol;
        this.row = row;
        this.col = col;
        this.player = player;
    }

    public char getSymbol() {
        return symbol;
    }

    public abstract boolean canMove(int newRow, int newCol);
}
