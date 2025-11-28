package mini.chess.game.GUI;

import mini.chess.game.Models.Board;
import mini.chess.game.utils.LanInviteManager;
import mini.chess.game.utils.GameDataManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LanInvitePanel extends JPanel {
    
    private final int userId;
    private final String myUsername;
    private final Runnable onGameStart;
    private final Runnable onClose;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollThread;
    
    // Send Invitation Tab
    private final JTextField usernameField = UIConstants.createStyledTextField(20);
    private final JButton sendButton = UIConstants.createStyledButton("📤 Send Invitation");
    private final JLabel sendStatusLabel = UIConstants.createStyledLabel("Enter opponent's username");
    private int sentGameId = -1;
    
    // Pending Invitations Tab
    private final DefaultListModel<String> invitationListModel = new DefaultListModel<>();
    private final JList<String> invitationList = new JList<>(invitationListModel);
    private final JButton acceptButton = UIConstants.createStyledButton("✅ Accept");
    private final JButton declineButton = UIConstants.createStyledButton("❌ Decline");
    private final JButton refreshButton = UIConstants.createStyledButton("🔄 Refresh");
    
    private List<LanInviteManager.Invitation> currentInvitations;
    
    public LanInvitePanel(int userId, String myUsername, Runnable onGameStart, Runnable onClose) {
        this.userId = userId;
        this.myUsername = myUsername;
        this.onGameStart = onGameStart;
        this.onClose = onClose;
        
        setLayout(new BorderLayout(15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(UIConstants.PADDING_BORDER);
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(UIConstants.BACKGROUND_COLOR);
        tabbedPane.setForeground(UIConstants.TEXT_COLOR);
        
        tabbedPane.addTab("Send Invitation", createSendTab());
        tabbedPane.addTab("Pending Invitations", createPendingTab());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add back button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        JButton backButton = UIConstants.createStyledButton("⬅ Back");
        backButton.setBackground(UIConstants.DANGER_COLOR);
        backButton.addActionListener(e -> {
            stopPolling();
            if (onClose != null) onClose.run();
        });
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Start polling
        startPolling();
    }
    
    private JPanel createSendTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(UIConstants.PADDING_BORDER);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel titleLabel = new JLabel("🎮 Challenge a Player");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setForeground(UIConstants.TEXT_COLOR);
        panel.add(titleLabel, gbc);
        
        gbc.gridy++;
        JLabel instructionLabel = UIConstants.createStyledLabel("Enter the username of the player you want to challenge:");
        panel.add(instructionLabel, gbc);
        
        gbc.gridy++;
        panel.add(usernameField, gbc);
        
        gbc.gridy++;
        panel.add(sendButton, gbc);
        
        gbc.gridy++;
        sendStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(sendStatusLabel, gbc);
        
        // Event handlers
        sendButton.addActionListener(e -> sendInvitation());
        usernameField.addActionListener(e -> sendInvitation());
        
        return panel;
    }
    
    private JPanel createPendingTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(UIConstants.PADDING_BORDER);
        
        JLabel titleLabel = new JLabel("📬 Pending Invitations");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setForeground(UIConstants.TEXT_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // List
        invitationList.setBackground(UIConstants.PANEL_COLOR);
        invitationList.setForeground(UIConstants.TEXT_COLOR);
        invitationList.setSelectionBackground(UIConstants.PRIMARY_COLOR);
        invitationList.setSelectionForeground(Color.WHITE);
        invitationList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(invitationList);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 2));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        acceptButton.setBackground(UIConstants.SUCCESS_COLOR);
        declineButton.setBackground(UIConstants.DANGER_COLOR);
        
        acceptButton.addActionListener(e -> acceptInvitation());
        declineButton.addActionListener(e -> declineInvitation());
        refreshButton.addActionListener(e -> refreshInvitations());
        
        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void sendInvitation() {
        String targetUsername = usernameField.getText().trim();
        
        if (targetUsername.isEmpty()) {
            sendStatusLabel.setText("❌ Please enter a username");
            sendStatusLabel.setForeground(UIConstants.DANGER_COLOR);
            return;
        }
        
        if (targetUsername.equalsIgnoreCase(myUsername)) {
            sendStatusLabel.setText("❌ You cannot invite yourself!");
            sendStatusLabel.setForeground(UIConstants.DANGER_COLOR);
            return;
        }
        
        sendStatusLabel.setText("⏳ Sending invitation...");
        sendStatusLabel.setForeground(UIConstants.TEXT_COLOR);
        sendButton.setEnabled(false);
        
        new Thread(() -> {
            int gameId = LanInviteManager.sendInvitation(userId, targetUsername);
            
            SwingUtilities.invokeLater(() -> {
                if (gameId != -1) {
                    sentGameId = gameId;
                    sendStatusLabel.setText("✅ Invitation sent! Waiting for " + targetUsername + " to accept...");
                    sendStatusLabel.setForeground(UIConstants.SUCCESS_COLOR);
                    usernameField.setText("");
                } else {
                    sendStatusLabel.setText("❌ Failed to send invitation. User may not exist.");
                    sendStatusLabel.setForeground(UIConstants.DANGER_COLOR);
                    sendButton.setEnabled(true);
                }
            });
        }).start();
    }
    
    private void refreshInvitations() {
        new Thread(() -> {
            currentInvitations = LanInviteManager.getPendingInvitations(userId);
            
            SwingUtilities.invokeLater(() -> {
                invitationListModel.clear();
                if (currentInvitations.isEmpty()) {
                    invitationListModel.addElement("No pending invitations");
                } else {
                    for (LanInviteManager.Invitation inv : currentInvitations) {
                        invitationListModel.addElement("🎮 " + inv.fromUsername + " wants to play with you!");
                    }
                }
            });
        }).start();
    }
    
    private void acceptInvitation() {
        int selectedIndex = invitationList.getSelectedIndex();
        if (selectedIndex < 0 || currentInvitations == null || selectedIndex >= currentInvitations.size()) {
            JOptionPane.showMessageDialog(this, "Please select an invitation to accept", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        LanInviteManager.Invitation invitation = currentInvitations.get(selectedIndex);
        
        new Thread(() -> {
            int[] playerIds = LanInviteManager.acceptInvitation(invitation.gameId, userId);
            
            if (playerIds != null) {
                SwingUtilities.invokeLater(() -> {
                    stopPolling();
                    startGameBoard(invitation.gameId, playerIds[0], playerIds[1]);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Failed to accept invitation", "Error", JOptionPane.ERROR_MESSAGE);
                    refreshInvitations();
                });
            }
        }).start();
    }
    
    private void declineInvitation() {
        int selectedIndex = invitationList.getSelectedIndex();
        if (selectedIndex < 0 || currentInvitations == null || selectedIndex >= currentInvitations.size()) {
            JOptionPane.showMessageDialog(this, "Please select an invitation to decline", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        LanInviteManager.Invitation invitation = currentInvitations.get(selectedIndex);
        
        new Thread(() -> {
            boolean success = LanInviteManager.declineInvitation(invitation.gameId);
            
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    refreshInvitations();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to decline invitation", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }
    
    private void startPolling() {
        running.set(true);
        pollThread = new Thread(() -> {
            while (running.get()) {
                try {
                    // Check if sent invitation was accepted
                    if (sentGameId != -1 && LanInviteManager.isInvitationAccepted(sentGameId)) {
                        int[] playerIds = LanInviteManager.getPlayerIds(sentGameId);
                        if (playerIds != null) {
                            final int gameId = sentGameId;
                            sentGameId = -1;
                            SwingUtilities.invokeLater(() -> {
                                stopPolling();
                                startGameBoard(gameId, playerIds[0], playerIds[1]);
                            });
                            break;
                        }
                    }
                    
                    // Refresh pending invitations
                    SwingUtilities.invokeLater(this::refreshInvitations);
                    
                    Thread.sleep(2000); // Poll every 2 seconds
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "LAN-Invite-Poll");
        pollThread.setDaemon(true);
        pollThread.start();
    }
    
    private void stopPolling() {
        running.set(false);
        if (pollThread != null) {
            pollThread.interrupt();
        }
    }
    
    private void startGameBoard(int gameId, int myPlayerId, int opponentPlayerId) {
        Board board = new Board(); // Fresh board
        
        // Determine if I'm player 1 or player 2
        int[] playerIds = LanInviteManager.getPlayerIds(gameId);
        boolean isPlayerOne = (playerIds != null && myPlayerId == playerIds[0]);
        
        GameBoardPanel gameBoardPanel = new GameBoardPanel(
            gameId, 
            myPlayerId, 
            opponentPlayerId,
            board,
            isPlayerOne
        );
        
        // Trigger callback to switch to game board
        if (onGameStart != null) {
            onGameStart.run();
        }
        
        // Add to parent container
        Container parent = getParent();
        if (parent != null) {
            String card = "GAME_" + gameId;
            parent.add(gameBoardPanel, card);
            if (parent.getLayout() instanceof CardLayout) {
                ((CardLayout) parent.getLayout()).show(parent, card);
            }
        }
    }
}
