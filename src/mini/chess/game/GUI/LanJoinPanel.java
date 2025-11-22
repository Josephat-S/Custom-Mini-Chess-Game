// java
package mini.chess.game.GUI;

import mini.chess.game.Models.Board;
import mini.chess.game.Network.Client;
import mini.chess.game.utils.GameDataManager;
import mini.chess.game.utils.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class LanJoinPanel extends JPanel {

    private final JLabel statusLabel = UIConstants.createStyledLabel("Status: Initializing...");
    private final JLabel connLabel = UIConstants.createStyledLabel("Connection: Disconnected");
    private final JLabel gameIdLabel = UIConstants.createStyledLabel("Game ID: -");
    private final JButton disconnectButton = UIConstants.createStyledButton("🔌 Disconnect");

    private final Object boardLock = new Object();

    private final int userId;
    private final Runnable onClose;

    private volatile Client client;
    private Thread listenerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean gameOver = new AtomicBoolean(false);

    private volatile Integer gameId = null;
    private volatile Integer playerId = null;

    private GameBoardPanel boardPanel;

    public LanJoinPanel(int userId, Runnable onClose) {
        this.userId = userId;
        this.onClose = onClose;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(UIConstants.PADDING_BORDER);

        JPanel topPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        topPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        topPanel.add(statusLabel);
        topPanel.add(connLabel);
        topPanel.add(gameIdLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(UIConstants.PANEL_COLOR);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        disconnectButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        disconnectButton.setBackground(UIConstants.DANGER_COLOR);
        disconnectButton.addActionListener(e -> disconnect());
        rightPanel.add(disconnectButton);

        JButton backBtn = UIConstants.createStyledButton("⬅ Back");
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> disconnect());
        rightPanel.add(Box.createVerticalStrut(12));
        rightPanel.add(backBtn);

        rightPanel.add(Box.createVerticalGlue());
        add(rightPanel, BorderLayout.EAST);

        startJoinFlow();
    }

    private void startJoinFlow() {
        HostPort hp = askHostAndPort();
        if (hp == null) {
            statusLabel.setText("Status: Cancelled");
            return;
        }

        try {
            client = new Client(hp.host, hp.port);
            connLabel.setText("Connection: Connected to " + hp.host + ":" + hp.port);
            statusLabel.setText("Status: Waiting for GAMEID...");
        } catch (IOException e) {
            statusLabel.setText("Status: Connect failed");
            connLabel.setText("Connection: " + e.getMessage());
            return;
        }

        running.set(true);

        listenerThread = new Thread(() -> {
            boolean helloSent = false;
            while (running.get() && !gameOver.get()) {
                try {
                    String incoming = client.pollMessage();
                    if (incoming == null) {
                        TimeUnit.MILLISECONDS.sleep(50);
                        continue;
                    }

                    if (incoming.startsWith("GAMEID ")) {
                        String idStr = incoming.substring(7).trim();
                        try {
                            int gid = Integer.parseInt(idStr);
                            gameId = gid;
                            SwingUtilities.invokeLater(() -> {
                                gameIdLabel.setText("Game ID: " + gid);
                                statusLabel.setText("Status: Sending HELLO...");
                            });
                            client.send("HELLO " + userId);
                            helloSent = true;
                        } catch (NumberFormatException ex) {
                            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Bad GAMEID"));
                        }
                    } else if (incoming.startsWith("HELLO_ACK")) {
                        Integer pid = parsePlayerIdFromHelloAck(incoming);
                        if (pid != null) {
                            playerId = pid;
                            // log joined (if gameId already known)
                            if (gameId != null) {
                                LogManager.logAction(userId, "JOINED_GAME " + gameId);
                            }
                        }
                        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Joined game"));
                        ensureBoardPanel();
                    } else if (incoming.startsWith("SYNC ")) {
                        String boardJson = incoming.substring(5);
                        Board newBoard = GameDataManager.boardFromStringForNetwork(boardJson);
                        SwingUtilities.invokeLater(() -> {
                            ensureBoardPanel();
                            boardPanel.applyExternalSync(newBoard, "Player2");
                            String winner = newBoard.checkWinner();
                            if (winner != null) {
                                boardPanel.setGameOver(winner);
                                statusLabel.setText("Status: " + winner + " wins");
                                gameOver.set(true);

                                if (winner.equals("Player2") && playerId != null) {
                                    try {
                                        client.send("WIN Player2 " + playerId);
                                    } catch (Exception ex) {
                                        statusLabel.setText("Status: Failed to notify host");
                                    }
                                }
                            }
                        });
                    }

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Listener error"));
                }
            }
        }, "Client-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void ensureBoardPanel() {
        if (boardPanel != null || gameId == null) return;

        Board initial = new Board();
        int pid = playerId != null ? playerId : 0;

        boardPanel = new GameBoardPanel(gameId, pid, initial, "Player2", new GameBoardPanel.MoveListener() {
            @Override
            public void onLocalMove(int fromRow, int fromCol, int toRow, int toCol, Board board) {
                if (!running.get() || gameOver.get()) return;
                try {
                    synchronized (boardLock) {
                        client.send("MOVE " + fromRow + " " + fromCol + " " + toRow + " " + toCol);
                    }
                    boardPanel.applyExternalSync(board, "Player1");
                    statusLabel.setText("Status: Move sent. Waiting for host...");
                } catch (Exception ex) {
                    statusLabel.setText("Status: Send failed");
                }
            }

            @Override
            public void onLocalVictory(String winner, Board board) {
                boardPanel.setGameOver(winner);
                statusLabel.setText("Status: " + winner + " (local)");
            }
        });

        add(boardPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private Integer parsePlayerIdFromHelloAck(String msg) {
        String rest = msg.substring("HELLO_ACK".length()).trim();
        if (rest.isEmpty()) return null;
        try {
            return Integer.parseInt(rest);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private HostPort askHostAndPort() {
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(UIConstants.createStyledLabel("Host/IP:"));
        JTextField hostField = UIConstants.createStyledTextField(16);
        hostField.setText("127.0.0.1");
        form.add(hostField);
        form.add(UIConstants.createStyledLabel("Port:"));
        JTextField portField = UIConstants.createStyledTextField(6);
        portField.setText("9000");
        form.add(portField);

        int resp = JOptionPane.showConfirmDialog(this, form, "Join LAN Game", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resp != JOptionPane.OK_OPTION) return null;

        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        try {
            int port = Integer.parseInt(portStr);
            if (host.isEmpty() || port < 1024 || port > 65535) throw new NumberFormatException();
            return new HostPort(host, port);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid host or port", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void disconnect() {
        running.set(false);
        gameOver.set(true);
        try {
            if (client != null) client.close();
        } catch (Exception ignored) {}
        if (listenerThread != null) listenerThread.interrupt();
        statusLabel.setText("Status: Disconnected");
        connLabel.setText("Connection: Closed");
        if (onClose != null) onClose.run();
    }

    private record HostPort(String host, int port) {}
}
