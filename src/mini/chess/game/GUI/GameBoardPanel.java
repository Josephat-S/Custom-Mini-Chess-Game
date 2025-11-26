package mini.chess.game.GUI;

import mini.chess.game.Models.AIPlayer;
import mini.chess.game.Models.Board;
import mini.chess.game.Models.Move;
import mini.chess.game.Models.Piece;
import mini.chess.game.utils.GameDataManager;
import mini.chess.game.db.AuthManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GameBoardPanel extends JPanel {

    public interface MoveListener {
        void onLocalMove(int fromRow, int fromCol, int toRow, int toCol, Board board);
        void onLocalVictory(String winner, Board board);
    }

    private final JButton[][] boardSquares = new JButton[5][5];
    private Board board;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private final JLabel statusLabel;
    private final JLabel turnLabel;
    private final JLabel player1Label;
    private final JLabel player2Label;
    private String currentPlayer = "Player1";
    private final int gameId;
    private final int playerId;
    private int moveCounter = 0;
    private boolean gameOver = false;
    
    // Game statistics
    private final long gameStartTime;
    private String player1Name = "Player 1";
    private String player2Name = "Player 2";
    private final int player1UserId;
    private final int player2UserId;

    private final boolean networkMode;
    private final String localPlayer;
    private final MoveListener moveListener;

    private final boolean aiMode;
    private AIPlayer aiPlayer;

    public GameBoardPanel(int gameId, int playerId, Board initialBoard, boolean isAI) {
        this(gameId, playerId, initialBoard, null, null, isAI, -1, -1);
    }

    public GameBoardPanel(int gameId, int playerId, Board initialBoard, String localPlayer, MoveListener listener) {
        this(gameId, playerId, initialBoard, localPlayer, listener, false, -1, -1);
    }
    
    public GameBoardPanel(int gameId, int playerId, Board initialBoard, boolean isAI, int player1UserId, int player2UserId) {
        this(gameId, playerId, initialBoard, null, null, isAI, player1UserId, player2UserId);
    }

    private GameBoardPanel(int gameId, int playerId, Board initialBoard, String localPlayer, MoveListener listener, boolean aiMode, int p1UserId, int p2UserId) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.board = initialBoard != null ? initialBoard : new Board();
        this.localPlayer = localPlayer;
        this.moveListener = listener;
        this.networkMode = (localPlayer != null && listener != null);
        this.aiMode = aiMode;
        this.gameStartTime = System.currentTimeMillis();
        this.player1UserId = p1UserId;
        this.player2UserId = p2UserId;
        
        // Get player names from database if available
        if (p1UserId != -1) {
            String name = AuthManager.getUsernameById(p1UserId);
            if (name != null) player1Name = name;
        }
        if (p2UserId != -1) {
            String name = AuthManager.getUsernameById(p2UserId);
            if (name != null) player2Name = name;
        } else if (aiMode) {
            player2Name = "AI";
        }

        setLayout(new BorderLayout(15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(UIConstants.PADDING_BORDER);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(UIConstants.BACKGROUND_COLOR);
        JLabel titleLabel = new JLabel("♔ Mini Chess ♔");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setForeground(UIConstants.TEXT_COLOR);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(5, 5, 2, 2));
        boardPanel.setBackground(UIConstants.BORDER_COLOR);
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 5),
                BorderFactory.createLineBorder(Color.BLACK, 1)
        ));

        Font pieceFont = new Font("Segoe UI Symbol", Font.BOLD, 36);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                JButton square = new JButton();
                square.setFont(pieceFont);
                square.setFocusPainted(false);
                square.setBorderPainted(true);
                Color bgColor = (i + j) % 2 == 0 ? UIConstants.LIGHT_SQUARE : UIConstants.DARK_SQUARE;
                square.setBackground(bgColor);
                square.setOpaque(true);

                int row = i;
                int col = j;
                square.addActionListener(e -> handleSquareClick(row, col));

                boardSquares[i][j] = square;
                boardPanel.add(square);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(UIConstants.PANEL_COLOR);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        infoPanel.setPreferredSize(new Dimension(250, 0));

        JPanel gameInfoPanel = new JPanel();
        gameInfoPanel.setLayout(new BoxLayout(gameInfoPanel, BoxLayout.Y_AXIS));
        gameInfoPanel.setBackground(UIConstants.PANEL_COLOR);
        gameInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel gameIdLabel = UIConstants.createStyledLabel("Game ID: " + gameId);
        gameIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gameInfoPanel.add(gameIdLabel);
        gameInfoPanel.add(Box.createVerticalStrut(5));

        player1Label = UIConstants.createStyledLabel("Player 1: ♔×1 ♙×4");
        player1Label.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
        player1Label.setForeground(new Color(0x00, 0x7B, 0xFF));
        gameInfoPanel.add(player1Label);
        gameInfoPanel.add(Box.createVerticalStrut(5));

        player2Label = UIConstants.createStyledLabel("Player 2: ♔×1 ♙×4");
        player2Label.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
        player2Label.setForeground(new Color(0xF4, 0x43, 0x36));
        gameInfoPanel.add(player2Label);

        infoPanel.add(gameInfoPanel);
        infoPanel.add(new JSeparator());
        infoPanel.add(Box.createVerticalStrut(15));

        turnLabel = new JLabel("Turn: Player 1");
        turnLabel.setFont(UIConstants.SUBTITLE_FONT);
        turnLabel.setForeground(UIConstants.TEXT_COLOR);
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(turnLabel);

        infoPanel.add(Box.createVerticalStrut(10));

        statusLabel = new JLabel(networkMode ? "Waiting for move..." : "Select a piece");
        statusLabel.setFont(UIConstants.LABEL_FONT);
        statusLabel.setForeground(UIConstants.TEXT_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(statusLabel);

        infoPanel.add(Box.createVerticalStrut(20));
        infoPanel.add(new JSeparator());
        infoPanel.add(Box.createVerticalStrut(20));

        JButton saveButton = UIConstants.createStyledButton("💾 Save Game");
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setMaximumSize(new Dimension(200, 40));
        saveButton.addActionListener(this::handleSaveGame);
        infoPanel.add(saveButton);

        infoPanel.add(Box.createVerticalStrut(10));

        JButton exitButton = UIConstants.createStyledButton("🚪 Exit Game");
        exitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exitButton.setMaximumSize(new Dimension(200, 40));
        exitButton.setBackground(UIConstants.DANGER_COLOR);
        exitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Exit this game?",
                    "Exit Game",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JFrame f) f.dispose();
            }
        });
        exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(UIConstants.DANGER_COLOR.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(UIConstants.DANGER_COLOR);
            }
        });
        infoPanel.add(exitButton);

        infoPanel.add(Box.createVerticalGlue());
        add(infoPanel, BorderLayout.EAST);

        if (this.aiMode) {
            this.aiPlayer = new AIPlayer();
            this.aiPlayer.setSearchDepth(2);
        }

        updateBoardDisplay();
        updatePieceCount();
    }

    private void handleSquareClick(int row, int col) {
        if (gameOver) {
            statusLabel.setText("Game is over!");
            statusLabel.setForeground(UIConstants.DANGER_COLOR);
            return;
        }

        if (networkMode && !currentPlayer.equals(localPlayer)) {
            statusLabel.setText("Wait for opponent turn");
            statusLabel.setForeground(UIConstants.DANGER_COLOR);
            return;
        }

        if (aiMode && currentPlayer.equals("Player2")) {
            statusLabel.setText("AI is thinking...");
            statusLabel.setForeground(UIConstants.TEXT_COLOR);
            return;
        }

        if (selectedRow == -1) {
            Piece piece = board.getPieceAt(row, col);
            if (piece != null && piece.getPlayer().equals(currentPlayer)) {
                selectedRow = row;
                selectedCol = col;
                highlightSquare(row, col);
                statusLabel.setText("Selected " + getPieceName(piece) + " at (" + row + "," + col + ")");
                statusLabel.setForeground(UIConstants.TEXT_COLOR);
            } else {
                statusLabel.setText("Select your piece!");
                statusLabel.setForeground(UIConstants.DANGER_COLOR);
            }
        } else {
            try {
                Move move = new Move(selectedRow, selectedCol, row, col);

                // Directly attempt the move (allows moving the Leader into danger).
                board.applyAndAnnounceMove(move, currentPlayer, "GUI");
                moveCounter++;

                if (networkMode) {
                    moveListener.onLocalMove(selectedRow, selectedCol, row, col, board);
                } else {
                    String fromCell = "(" + selectedRow + "," + selectedCol + ")";
                    String toCell = "(" + row + "," + col + ")";
                    GameDataManager.recordMoveAndUpdateState(gameId, playerId, moveCounter, fromCell, toCell, board);
                }

                updateBoardDisplay();
                updatePieceCount();
                clearHighlight();

                String winner = board.checkWinner();
                if (winner != null) {
                    setGameOver(winner);
                    if (networkMode) {
                        moveListener.onLocalVictory(winner, board);
                    }
                } else {
                    currentPlayer = currentPlayer.equals("Player1") ? "Player2" : "Player1";
                    turnLabel.setText("Turn: " + (currentPlayer.equals("Player1") ? "Player 1" : "Player 2"));
                    statusLabel.setText("Move successful");
                    statusLabel.setForeground(UIConstants.SUCCESS_COLOR);

                    if (aiMode && currentPlayer.equals("Player2")) {
                        performAIMove();
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                statusLabel.setText("Invalid move!");
                statusLabel.setForeground(UIConstants.DANGER_COLOR);
            } catch (Exception ex) {
                statusLabel.setText("Move error: " + ex.getMessage());
                statusLabel.setForeground(UIConstants.DANGER_COLOR);
            }
            selectedRow = -1;
            selectedCol = -1;
            clearHighlight();
        }
    }

    private void performAIMove() {
        statusLabel.setText("AI is thinking...");
        statusLabel.setForeground(UIConstants.TEXT_COLOR);

        SwingWorker<Move, Void> worker = new SwingWorker<>() {
            @Override
            protected Move doInBackground() {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                return aiPlayer.chooseBestMove(board, "Player2");
            }

            @Override
            protected void done() {
                try {
                    Move aiMove = get();
                    if (aiMove != null) {
                        board.applyAndAnnounceMove(aiMove, "Player2", "AI");
                        moveCounter++;

                        String fromCell = "(" + aiMove.fromRow + "," + aiMove.fromCol + ")";
                        String toCell = "(" + aiMove.toRow + "," + aiMove.toCol + ")";
                        GameDataManager.recordMoveAndUpdateState(gameId, playerId, moveCounter, fromCell, toCell, board);

                        updateBoardDisplay();
                        updatePieceCount();

                        String winner = board.checkWinner();
                        if (winner != null) {
                            setGameOver(winner);
                        } else {
                            currentPlayer = "Player1";
                            turnLabel.setText("Turn: Player 1");
                            statusLabel.setText("AI moved");
                            statusLabel.setForeground(UIConstants.SUCCESS_COLOR);
                        }
                    } else {
                        statusLabel.setText("AI has no moves");
                        statusLabel.setForeground(UIConstants.DANGER_COLOR);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("AI error: " + ex.getMessage());
                    statusLabel.setForeground(UIConstants.DANGER_COLOR);
                }
            }
        };
        worker.execute();
    }

    private void updateBoardDisplay() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Piece piece = board.getPieceAt(i, j);
                JButton square = boardSquares[i][j];
                if (piece != null) {
                    square.setText(getPieceSymbol(piece));
                    square.setForeground(piece.getPlayer().equals("Player1") ?
                            new Color(0x00, 0x7B, 0xFF) : new Color(0xF4, 0x43, 0x36));
                } else {
                    square.setText("");
                }
                Color bgColor = (i + j) % 2 == 0 ? UIConstants.LIGHT_SQUARE : UIConstants.DARK_SQUARE;
                square.setBackground(bgColor);
            }
        }
    }

    private void updatePieceCount() {
        int p1Leaders = 0, p1Soldiers = 0, p2Leaders = 0, p2Soldiers = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Piece piece = board.getPieceAt(i, j);
                if (piece != null) {
                    boolean isLeader = piece.getClass().getSimpleName().equals("Leader");
                    if (piece.getPlayer().equals("Player1")) {
                        if (isLeader) p1Leaders++; else p1Soldiers++;
                    } else {
                        if (isLeader) p2Leaders++; else p2Soldiers++;
                    }
                }
            }
        }
        player1Label.setText("Player 1: ♔×" + p1Leaders + " ♙×" + p1Soldiers);
        player2Label.setText("Player 2: ♔×" + p2Leaders + " ♙×" + p2Soldiers);
    }

    private String getPieceSymbol(Piece piece) {
        String type = piece.getClass().getSimpleName();
        if (type.equals("Leader")) return "♔";
        if (type.equals("Soldier")) return "♙";
        return "?";
    }

    private String getPieceName(Piece piece) {
        String type = piece.getClass().getSimpleName();
        return type.equals("Leader") ? "Leader" : "Soldier";
    }

    private void highlightSquare(int row, int col) {
        boardSquares[row][col].setBorder(BorderFactory.createLineBorder(UIConstants.HIGHLIGHT_COLOR, 4));
    }

    private void clearHighlight() {
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 5; j++)
                boardSquares[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
    }

    private void handleSaveGame(ActionEvent e) {
        try {
            boolean saved;
            if (!networkMode) {
                String boardJson = GameDataManager.boardToStringForNetwork(board);
                int currentTurn = currentPlayer.equals("Player1") ? 1 : 2;
                saved = GameDataManager.saveUnfinishedGameWithBoardData(playerId, boardJson, currentTurn);
            } else {
                JOptionPane.showMessageDialog(this, "Network games are auto-saved", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (saved) {
                statusLabel.setText("Game saved!");
                statusLabel.setForeground(UIConstants.SUCCESS_COLOR);
            } else {
                statusLabel.setText("Save failed");
                statusLabel.setForeground(UIConstants.DANGER_COLOR);
            }
        } catch (Exception ex) {
            statusLabel.setText("Error saving");
            statusLabel.setForeground(UIConstants.DANGER_COLOR);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void applyExternalSync(Board newBoard, String nextTurn) {
        this.board = newBoard != null ? newBoard : new Board();
        updateBoardDisplay();
        updatePieceCount();
        if (nextTurn != null) {
            currentPlayer = nextTurn;
            turnLabel.setText("Turn: " + (currentPlayer.equals("Player1") ? "Player 1" : "Player 2"));
        }
        statusLabel.setText("Board synced");
        statusLabel.setForeground(UIConstants.TEXT_COLOR);
    }

    public void setGameOver(String winner) {
        gameOver = true;
        
        // Update status label
        if (winner != null) {
            statusLabel.setText("GAME OVER: " + winner);
            statusLabel.setForeground(UIConstants.SUCCESS_COLOR);
        } else {
            statusLabel.setText("GAME OVER");
            statusLabel.setForeground(UIConstants.SUCCESS_COLOR);
        }

        // Handle DB updates for local games (not network mode)
        if (!networkMode && winner != null) {
            int winnerId = -1;
            int loserId = -1;

            if (winner.contains("Player1")) {
                winnerId = player1UserId;
                loserId = player2UserId;
            } else if (winner.contains("Player2")) {
                winnerId = player2UserId;
                loserId = player1UserId;
            }

            if (winnerId != -1) {
                GameDataManager.updatePlayerScore(winnerId, 5);
                GameDataManager.markGameAsComplete(gameId, winnerId);
                GameDataManager.updatePlayerStatus(winnerId, "WIN");
            }
            
            if (loserId != -1) {
                GameDataManager.updatePlayerStatus(loserId, "LOSS");
            }
        }
        
        // Determine winner name
        String winnerName;
        if (winner != null && winner.contains("Player1")) {
            winnerName = player1Name;
        } else if (winner != null && winner.contains("Player2")) {
            winnerName = player2Name;
        } else {
            winnerName = "Draw";
        }
        
        // Calculate game duration
        long durationMs = System.currentTimeMillis() - gameStartTime;
        int seconds = (int) (durationMs / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        String duration = String.format("%d:%02d", minutes, seconds);
        
        // Get piece counts
        int p1Leaders = 0, p1Soldiers = 0, p2Leaders = 0, p2Soldiers = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Piece piece = board.getPieceAt(i, j);
                if (piece != null) {
                    boolean isLeader = piece.getClass().getSimpleName().equals("Leader");
                    if (piece.getPlayer().equals("Player1")) {
                        if (isLeader) p1Leaders++; else p1Soldiers++;
                    } else {
                        if (isLeader) p2Leaders++; else p2Soldiers++;
                    }
                }
            }
        }
        String winnerPieces = winner != null && winner.contains("Player1") ?
            "♔×" + p1Leaders + " ♙×" + p1Soldiers :
            "♔×" + p2Leaders + " ♙×" + p2Soldiers;
        
        // Show winner dialog
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JFrame) {
                WinnerDialog dialog = new WinnerDialog(
                    (JFrame) window,
                    winnerName,
                    moveCounter,
                    duration,
                    winnerPieces,
                    null,  // Play again - not implemented here
                    () -> {
                        // Return to main menu
                        Container parent = this.getParent();
                        if (parent instanceof JPanel) {
                            Container grandParent = parent.getParent();
                            if (grandParent instanceof JPanel) {
                                // Navigate to MAIN_MENU card
                                ((CardLayout)((JPanel)grandParent).getLayout()).show((Container)grandParent, "MAIN_MENU");
                            }
                        }
                    }
                );
                dialog.setVisible(true);
            }
        });
    }

    public Board getBoard() {
        return board;
    }
}
