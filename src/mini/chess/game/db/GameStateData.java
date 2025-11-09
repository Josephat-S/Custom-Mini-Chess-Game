// java
package mini.chess.game.db;

import java.sql.Timestamp;

public class GameStateData {
    private final String boardJson;
    private final int playerTurn;
    private final String lastMove;
    private final Timestamp savedAt;

    public GameStateData(String boardJson, int playerTurn, String lastMove, Timestamp savedAt) {
        this.boardJson = boardJson;
        this.playerTurn = playerTurn;
        this.lastMove = lastMove;
        this.savedAt = savedAt;
    }

    public String getBoardJson() { return boardJson; }
    public int getPlayerTurn() { return playerTurn; }
    public String getLastMove() { return lastMove; }
    public Timestamp getSavedAt() { return savedAt; }
<<<<<<< HEAD
}
=======
}
>>>>>>> 2d6404598e9a28c009378acce2df4ffd913c2536
