package mini.chess.game.GUI;

import mini.chess.game.Models.Board;
import mini.chess.game.db.AuthManager;
import mini.chess.game.utils.GameDataManager;
import mini.chess.game.utils.LogManager;
import mini.chess.game.utils.BackupManager;

import javax.swing.*;
import java.awt.*;

/**
 * Main application UI for Mini Chess.
 * Minor changes: records LOGIN, REGISTER and LOGOUT actions using LogManager.
 */
public class GameUI extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);
    private int userId = -1;

    public GameUI() {
        super("Mini Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);

        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        add(mainPanel);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRegisterPanel(), "REGISTER");
        mainPanel.add(createMainMenuPanel(), "MENU");

        cardLayout.show(mainPanel, "LOGIN");
    }

    private JPanel createFormPanel(boolean isLogin) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(UIConstants.PADDING_BORDER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(isLogin ? "Login" : "Register");
        title.setFont(UIConstants.TITLE_FONT);
        title.setForeground(UIConstants.TEXT_COLOR);

        JTextField userField = UIConstants.createStyledTextField(20);
        JPasswordField passField = UIConstants.createStyledPasswordField(20);

        JButton submit = UIConstants.createStyledButton(isLogin ? "Sign In" : "Create Account");
        JButton switchBtn = UIConstants.createStyledButton(isLogin ? "Go to Register" : "Go to Login");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridy++;
        panel.add(UIConstants.createStyledLabel("Username"), gbc);
        gbc.gridy++;
        panel.add(userField, gbc);
        gbc.gridy++;
        panel.add(UIConstants.createStyledLabel("Password"), gbc);
        gbc.gridy++;
        panel.add(passField, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        panel.add(submit, gbc);
        gbc.gridx = 1;
        panel.add(switchBtn, gbc);

        submit.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password are required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isLogin) {
                AuthManager.AuthResult result = AuthManager.loginWithDetails(u, p);
                if (result.isSuccess()) {
                    userId = result.userId;
                    LogManager.logAction(userId, "LOGIN");
                    
                    if (AuthManager.isAdmin(userId)) {
                        showAdminDashboard();
                    } else {
                        cardLayout.show(mainPanel, "MENU");
                    }
                } else {
                    // Display specific error message based on error code
                    int messageType = "ACCOUNT_LOCKED".equals(result.errorCode) ? 
                        JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE;
                    JOptionPane.showMessageDialog(this, result.userMessage, 
                        "Login Failed", messageType);
                }
            } else {
                AuthManager.AuthResult result = AuthManager.registerWithDetails(u, p);
                if (result.isSuccess()) {
                    userId = result.userId;
                    LogManager.logAction(userId, "REGISTER");
                    cardLayout.show(mainPanel, "MENU");
                } else {
                    // Display specific error message
                    JOptionPane.showMessageDialog(this, result.userMessage, 
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        switchBtn.addActionListener(e -> cardLayout.show(mainPanel, isLogin ? "REGISTER" : "LOGIN"));

        return panel;
    }

    private JPanel createLoginPanel() { return createFormPanel(true); }

    private JPanel createRegisterPanel() { return createFormPanel(false); }

    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(UIConstants.PADDING_BORDER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("♔ Mini Chess");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setForeground(UIConstants.TEXT_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, gbc);

        gbc.insets = new Insets(5, 120, 5, 120);
        gbc.gridy++;

        JButton localGameBtn = UIConstants.createStyledButton("🎮 New Local Game");
        JButton lanGameBtn = UIConstants.createStyledButton("🌐 Play on LAN");
        JButton loadGameBtn = UIConstants.createStyledButton("💾 Load Saved Game");
        JButton leaderboardBtn = UIConstants.createStyledButton("🏆 Leaderboard");
        JButton exitBtn = UIConstants.createStyledButton("🚪 Exit");

        localGameBtn.addActionListener(e -> startNewLocalGame());
        lanGameBtn.addActionListener(e -> startLanGame());
        loadGameBtn.addActionListener(e -> loadSavedGame());
        leaderboardBtn.addActionListener(e -> showLeaderboard());
        exitBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                if (userId != -1) {
                    LogManager.logAction(userId, "LOGOUT");
                    userId = -1;
                }
                dispose();
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 15));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        buttonPanel.add(localGameBtn);
        buttonPanel.add(lanGameBtn);
        buttonPanel.add(loadGameBtn);
        buttonPanel.add(leaderboardBtn);
        buttonPanel.add(exitBtn);

        panel.add(buttonPanel, gbc);

        // Add DB Connection Info
        gbc.gridy++;
        gbc.insets = new Insets(20, 10, 5, 10);
        String dbUrl = mini.chess.game.db.DBConnection.getDbUrl();
        String host = "Unknown";
        try {
            java.net.URI uri = new java.net.URI(dbUrl.substring(5)); // remove jdbc:
            host = uri.getHost();
        } catch (Exception e) {
            if (dbUrl.contains("localhost")) host = "localhost";
            else if (dbUrl.contains("127.0.0.1")) host = "127.0.0.1";
        }
        
        JLabel dbLabel = new JLabel("DB Connected to: " + host);
        dbLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        dbLabel.setForeground(java.awt.Color.GRAY);
        dbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(dbLabel, gbc);

        return panel;
    }

    private void showLeaderboard() {
        LeaderboardPanel lbPanel = new LeaderboardPanel(() -> cardLayout.show(mainPanel, "MENU"));
        String card = "LEADERBOARD_" + System.currentTimeMillis();
        mainPanel.add(lbPanel, card);
        cardLayout.show(mainPanel, card);
    }

    private void startLanGame() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "You must be logged in to play on LAN", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get username for current user
        String myUsername = AuthManager.getUsernameById(userId);
        if (myUsername == null) myUsername = "User" + userId;
        
        LanInvitePanel invitePanel = new LanInvitePanel(
            userId, 
            myUsername,
            () -> {}, // onGameStart callback (unused, panel handles it)
            () -> cardLayout.show(mainPanel, "MENU")
        );
        
        String card = "LAN_INVITE_" + System.currentTimeMillis();
        mainPanel.add(invitePanel, card);
        cardLayout.show(mainPanel, card);
    }

    private void startNewLocalGame() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "You must be logged in to start a game", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] options = {"Human", "AI"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose opponent",
                "New Local Game",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice == -1) return;

        boolean isAI = (choice == 1);

        Board board = new Board();
        GameDataManager.GameCreateResult res = GameDataManager.createLanGameForHost(userId, board, 1);
        if (res.gameId == -1) {
            JOptionPane.showMessageDialog(this, "Failed to create game", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GameBoardPanel gameBoardPanel = new GameBoardPanel(
            res.gameId, 
            res.playerId, 
            board, 
            isAI, 
            () -> cardLayout.show(mainPanel, "MENU"), // onExit
            () -> cardLayout.show(mainPanel, "MENU")  // onPlayAgain
        );
        String card = "GAME_" + res.gameId;
        mainPanel.add(gameBoardPanel, card);
        cardLayout.show(mainPanel, card);
    }

    private void showAdminDashboard() {
        AdminDashboardPanel adminPanel = new AdminDashboardPanel(userId, () -> {
            LogManager.logAction(userId, "LOGOUT");
            userId = -1;
            cardLayout.show(mainPanel, "LOGIN");
        });
        String card = "ADMIN_" + System.currentTimeMillis();
        mainPanel.add(adminPanel, card);
        cardLayout.show(mainPanel, card);

        // Trigger Automatic Backup
        if (BackupManager.isInternetAvailable()) {
            BackupManager.performBackup(this);
        }
    }

    private void loadSavedGame() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "You must be logged in to load saved games", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        java.util.List<Integer> savedGames = GameDataManager.listSavedGamesForUser(userId);
        if (savedGames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved games found", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] options = savedGames.stream().map(id -> "Game ID: " + id).toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Select a game:",
                "Load Saved Game",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (selected != null) {
            int gameId = Integer.parseInt(selected.split(": ")[1]);
            Board board = GameDataManager.loadGameById(gameId);
            if (board != null) {
                GameBoardPanel gb = new GameBoardPanel(
                    gameId, 
                    0, 
                    board, 
                    false,
                    () -> cardLayout.show(mainPanel, "MENU"), // onExit
                    () -> cardLayout.show(mainPanel, "MENU")  // onPlayAgain
                );
                String card = "GAME_" + gameId;
                mainPanel.add(gb, card);
                cardLayout.show(mainPanel, card);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to load game", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new GameUI().setVisible(true));
    }
}
