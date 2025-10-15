/**
 * Base class for all chess pieces in the game.
 * Provides common functionality and properties for game pieces.
 */
package Models;

/**
 * Abstract base class that defines the common properties and behaviors
 * of all chess pieces in the game. Each specific piece type (Leader, Soldier)
 * extends this class and implements its own movement rules.
 */
public abstract class Piece {
    /** Name of the piece (e.g., "Leader", "Soldier") */
    protected String name;
    /** Symbol used to represent the piece on the board */
    protected char symbol;
    /** Current row position of the piece */
    protected int row;
    /** Current column position of the piece */
    protected int col;
    /** Player who owns this piece ("Player1" or "Player2") */
    protected String player;

    /**
     * Constructor for creating a new piece.
     * 
     * @param name   The name of the piece
     * @param symbol The character symbol representing the piece
     * @param row    Initial row position
     * @param col    Initial column position
     * @param player The player who owns this piece
     */
    public Piece(String name, char symbol, int row, int col, String player) {
        this.name = name;
        this.symbol = symbol;
        this.row = row;
        this.col = col;
        this.player = player;
    }

    /**
     * Gets the symbol character used to represent this piece on the board.
     * 
     * @return The piece's symbol character
     */
    public char getSymbol() {
        return symbol;
    }

    /**
     * Checks if the piece can move to the specified position.
     * Each piece type implements its own movement rules.
     * 
     * @param newRow The target row position
     * @param newCol The target column position
     * @return true if the move is valid for this piece type, false otherwise
     */
    public abstract boolean canMove(int newRow, int newCol);
}
