// java
package mini.chess.game.GUI;

import mini.chess.game.Models.Board;
import mini.chess.game.Network.Server;
import mini.chess.game.utils.GameDataManager;
import mini.chess.game.utils.FirewallRuleManager;
import mini.chess.game.utils.NetworkInfo;
import mini.chess.game.utils.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LanHostPanel extends JPanel {

    private final JLabel statusLabel = UIConstants.createStyledLabel("Status: Initializing...");
    private final JLabel connLabel = UIConstants.createStyledLabel("Connection: Waiting...");
    private final JLabel gameIdLabel = UIConstants.createStyledLabel("Game ID: -");
    private final JLabel firewallLabel = UIConstants.createStyledLabel("Firewall: Checking...");
    private final JButton stopButton = UIConstants.createStyledButton("🛑 Stop Hosting");

    private final Object boardLock = new Object();

    private Server server;
    private Thread acceptThread;
    private Thread listenerThread;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean gameOver = new AtomicBoolean(false);
    private final AtomicInteger moveCounter = new AtomicInteger(0);

    private final int hostUserId;
    private int gameId = -1;
    private int hostPlayerId = -1;
    private Integer clientPlayerId = null;

    private GameBoardPanel boardPanel;

    private final Runnable onClose;

    private int hostingPort = -1;

    public LanHostPanel(int hostUserId, Runnable onClose) {
        this.hostUserId = hostUserId;
        this.onClose = onClose;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(UIConstants.PADDING_BORDER);

        JPanel topPanel = new JPanel(new GridLayout(2, 2, 15, 10));
        topPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        topPanel.add(statusLabel);
        topPanel.add(connLabel);
        topPanel.add(gameIdLabel);
        topPanel.add(firewallLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(UIConstants.PANEL_COLOR);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        stopButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        stopButton.addActionListener(e -> stopHosting());
        rightPanel.add(stopButton);
        rightPanel.add(Box.createVerticalStrut(15));

        JButton backBtn = UIConstants.createStyledButton("⬅ Back");
        backBtn.setBackground(UIConstants.DANGER_COLOR);
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> stopHosting());
        rightPanel.add(backBtn);
        rightPanel.add(Box.createVerticalGlue());
        add(rightPanel, BorderLayout.EAST);

        startHostingFlow();
    }

    private void startHostingFlow() {
        Integer port = askPort();
        if (port == null) {
            statusLabel.setText("Status: Cancelled");
            return;
        }
        try {
            server = new Server(port);
            hostingPort = port;

            // Check admin privileges first
            boolean isAdmin = FirewallRuleManager.isRunningAsAdmin();
            
            // Attempt to add firewall rule
            boolean firewallOk = FirewallRuleManager.addFirewallRule(port);
            
            String ip = NetworkInfo.getLocalIPv4();
            statusLabel.setText("Status: Listening on " + ip + ":" + port);
            connLabel.setText("Connection: Awaiting client...");
            
            // Update firewall status label
            if (firewallOk) {
                firewallLabel.setText("Firewall: Rule created ✓");
                firewallLabel.setForeground(UIConstants.SUCCESS_COLOR);
            } else {
                if (!isAdmin) {
                    firewallLabel.setText("Firewall: Warning - Not Admin");
                    firewallLabel.setForeground(UIConstants.WARNING_COLOR);
                    JOptionPane.showMessageDialog(this,
                            "⚠ Not running as Administrator!\n\n" +
                            "Firewall rule could not be created automatically.\n" +
                            "Clients may not be able to connect.\n\n" +
                            "Solutions:\n" +
                            "1. Restart the application as Administrator (recommended)\n" +
                            "2. Manually add firewall rule for port " + port + "\n" +
                            "3. Temporarily disable Windows Firewall (not recommended)",
                            "Administrator Rights Required",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    firewallLabel.setText("Firewall: Rule creation failed");
                    firewallLabel.setForeground(UIConstants.DANGER_COLOR);
                }
            }
            
            // Show connection instructions
            if (firewallOk || isAdmin) {
                JOptionPane.showMessageDialog(this,
                        "🎮 Server is ready!\n\n" +
                        "Share this information with other players:\n" +
                        "• IP Address: " + ip + "\n" +
                        "• Port: " + port + "\n\n" +
                        "Clients should connect to: " + ip + ":" + port,
                        "Server Ready",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            statusLabel.setText("Status: Server error");
            connLabel.setText("Connection: " + e.getMessage());
            firewallLabel.setText("Firewall: N/A");
            firewallLabel.setForeground(UIConstants.TEXT_COLOR);
            return;
        }

        Board initialBoard = new Board();
        GameDataManager.GameCreateResult res = GameDataManager.createLanGameForHost(hostUserId, initialBoard, 1);
        if (res.gameId == -1) {
            statusLabel.setText("Status: DB game creation failed");
            return;
        }
        gameId = res.gameId;
        hostPlayerId = res.playerId;
        gameIdLabel.setText("Game ID: " + gameId);

        // record host action
        LogManager.logAction(hostUserId, "HOSTED_GAME " + gameId);

        boardPanel = new GameBoardPanel(gameId, hostPlayerId, initialBoard, "Player1", new GameBoardPanel.MoveListener() {
            @Override
            public void onLocalMove(int fromRow, int fromCol, int toRow, int toCol, Board board) {
                if (gameOver.get()) return;
                synchronized (boardLock) {
                    GameDataManager.recordMoveAndUpdateState(gameId, hostPlayerId, moveCounter.getAndIncrement(),
                            fromRow + "_" + fromCol, toRow + "_" + toCol, board);
                }
                if (server != null) server.send("SYNC " + GameDataManager.boardToStringForNetwork(board));
                boardPanel.applyExternalSync(board, "Player2");
                String winner = board.checkWinner();
                if (winner != null) handleVictory(winner);
            }

            @Override
            public void onLocalVictory(String winner, Board board) {
                handleVictory(winner);
            }
        });

        add(boardPanel, BorderLayout.CENTER);
        revalidate();
        repaint();

        Thread acceptThreadLocal = new Thread(() -> {
            try {
                server.accept();
                connLabel.setText("Connection: Client connected ✓");
                connLabel.setForeground(UIConstants.SUCCESS_COLOR);
                server.send("GAMEID " + gameId);
            } catch (java.net.SocketTimeoutException e) {
                connLabel.setText("Connection: Accept timeout (still listening)");
                connLabel.setForeground(UIConstants.WARNING_COLOR);
            } catch (IOException e) {
                connLabel.setText("Connection: Accept failed - " + e.getMessage());
                connLabel.setForeground(UIConstants.DANGER_COLOR);
            }
        }, "Host-Accept");
        acceptThread = acceptThreadLocal;
        acceptThread.setDaemon(true);
        acceptThread.start();

        running.set(true);
        startListener();
    }

    private void startListener() {
        listenerThread = new Thread(() -> {
            while (running.get() && !gameOver.get()) {
                try {
                    String msg = server.pollMessage();
                    if (msg == null) {
                        TimeUnit.MILLISECONDS.sleep(50);
                        continue;
                    }
                    if (msg.startsWith("HELLO ")) {
                        try {
                            int clientUserId = Integer.parseInt(msg.substring(6).trim());
                            int addedId = GameDataManager.addPlayerToExistingGame(gameId, clientUserId);
                            if (addedId != -1) {
                                clientPlayerId = addedId;
                                server.send("HELLO_ACK " + addedId);
                                statusLabel.setText("Status: Client joined (playerId " + addedId + ")");
                                // log client joined
                                LogManager.logAction(hostUserId, "CLIENT_JOINED game=" + gameId + " playerId=" + addedId + " userId=" + clientUserId);
                            } else {
                                statusLabel.setText("Status: Failed adding client");
                            }
                        } catch (NumberFormatException ex) {
                            statusLabel.setText("Status: Bad HELLO");
                        }
                    } else if (msg.startsWith("MOVE ")) {
                        String[] parts = msg.substring(5).trim().split("\\s+");
                        if (parts.length == 4 && clientPlayerId != null) {
                            try {
                                int fromRow = Integer.parseInt(parts[0]);
                                int fromCol = Integer.parseInt(parts[1]);
                                int toRow = Integer.parseInt(parts[2]);
                                int toCol = Integer.parseInt(parts[3]);
                                synchronized (boardLock) {
                                    boardPanel.getBoard().movePiece(fromRow, fromCol, toRow, toCol);
                                    GameDataManager.recordMoveAndUpdateState(gameId, clientPlayerId, moveCounter.getAndIncrement(),
                                            fromRow + "_" + fromCol, toRow + "_" + toCol, boardPanel.getBoard());
                                }
                                String json = GameDataManager.boardToStringForNetwork(boardPanel.getBoard());
                                server.send("SYNC " + json);
                                boardPanel.applyExternalSync(boardPanel.getBoard(), "Player1");
                                String winner = boardPanel.getBoard().checkWinner();
                                if (winner != null) handleVictory(winner);
                            } catch (Exception ex) {
                                statusLabel.setText("Status: Invalid client move");
                            }
                        }
                    } else if (msg.startsWith("WIN ")) {
                        String[] parts = msg.substring(4).trim().split("\\s+");
                        if (parts.length == 2 && parts[0].equals("Player2")) {
                            try {
                                int winnerPlayerId = Integer.parseInt(parts[1]);
                                handleVictory("Player2");
                            } catch (NumberFormatException ex) {
                                statusLabel.setText("Status: Bad WIN message");
                            }
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    statusLabel.setText("Status: Listener error");
                }
            }
        }, "Host-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleVictory(String winner) {
        gameOver.set(true);
        boardPanel.setGameOver(winner);
        statusLabel.setText("Status: " + winner + " wins");

        int winnerPlayerId = winner.equals("Player1") ? hostPlayerId :
                (clientPlayerId != null ? clientPlayerId : -1);

        if (winnerPlayerId != -1) {
            boolean scoreUpdated = GameDataManager.updatePlayerScore(winnerPlayerId, 5);
            boolean gameMarked = GameDataManager.markGameAsComplete(gameId, winnerPlayerId);

            if (!scoreUpdated || !gameMarked) {
                statusLabel.setText("Status: " + winner + " wins (DB update failed)");
            }
        }

        if (server != null) server.send("SYNC " + GameDataManager.boardToStringForNetwork(boardPanel.getBoard()));
    }

    private Integer askPort() {
        String s = JOptionPane.showInputDialog(this, "Port to host on:", "9000");
        if (s == null) return null;
        try {
            int p = Integer.parseInt(s.trim());
            if (p < 1024 || p > 65535) throw new NumberFormatException();
            return p;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid port", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void stopHosting() {
        running.set(false);
        gameOver.set(true);
        try {
            if (server != null) server.close();
        } catch (Exception ignored) {}
        if (acceptThread != null) acceptThread.interrupt();
        if (listenerThread != null) listenerThread.interrupt();

        if (hostingPort > 0) {
            FirewallRuleManager.removeFirewallRule(hostingPort);
            hostingPort = -1;
        }

        statusLabel.setText("Status: Hosting stopped");
        connLabel.setText("Connection: Closed");
        if (onClose != null) onClose.run();
    }
}
