package mini.chess.game.Network;

import mini.chess.game.Models.Board;
import mini.chess.game.db.GameDataManager;

public class LANHandler {
    // (existing fields & methods)

    /**
     * Call this from your game move logic when a local player makes a move.
     * It persists the move + new gamestate and then broadcasts the updated board JSON to peers.
     */
    public void onLocalMovePlayed(int gameId,
                                  int playerId,
                                  Integer moveNumber,
                                  String fromCell,
                                  String toCell,
                                  Board board) {
        // persist move + gamestate
        boolean ok = GameDataManager.recordMoveAndUpdateState(gameId, playerId, moveNumber, fromCell, toCell, board);

        // Always broadcast the new board to peers (LAN clients/host) so both UIs stay in sync.
        // Use existing broadcast/send code in this class to push the new board.
        String boardJson = GameDataManager.boardToStringForNetwork(board);
        broadcastBoardUpdate(gameId, boardJson, fromCell, toCell, moveNumber);

        if (!ok) {
            System.err.println("Warning: failed to persist move for game " + gameId);
        }
    }

    // Example stub - replace with actual sending code already present in LANHandler
    private void broadcastBoardUpdate(int gameId, String boardJson, String fromCell, String toCell, Integer moveNumber) {
        // iterate connected peers and send a message with boardJson + lastMove info
        // e.g. for each connection: sendMessage(new MoveMessage(gameId, boardJson, fromCell, toCell, moveNumber));
    }
}
