package mini.chess.game.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated winner dialog with confetti and trophy animations.
 */
public class WinnerDialog extends JDialog {
    
    private static class Confetti {
        double x, y, vx, vy;
        Color color;
        int size;
        
        Confetti(double x, double y) {
            this.x = x;
            this.y = y;
            Random rand = new Random();
            this.vx = (rand.nextDouble() - 0.5) * 2;
            this.vy = rand.nextDouble() * 2 + 1;
            this.size = rand.nextInt(8) + 4;
            Color[] colors = {
                new Color(255, 107, 107),  // Red
                new Color(78, 205, 196),   // Teal
                new Color(255, 195, 0),    // Gold
                new Color(199, 0, 57),     // Pink
                new Color(106, 176, 76)    // Green
            };
            this.color = colors[rand.nextInt(colors.length)];
        }
        
        void update() {
            x += vx;
            y += vy;
            vy += 0.15; // gravity
        }
    }
    
    private final List<Confetti> confettiList = new ArrayList<>();
    private Timer animationTimer;
    private float trophyScale = 1.0f;
    private boolean trophyGrowing = true;
    private int fadeAlpha = 0;
    
    private final String winnerName;
    private final int totalMoves;
    private final String duration;
    private final String piecesCount;
    private final Runnable onPlayAgain;
    private final Runnable onReturnToMenu;
    
    public WinnerDialog(JFrame parent, String winnerName, int totalMoves, 
                        String duration, String piecesCount,
                        Runnable onPlayAgain, Runnable onReturnToMenu) {
        super(parent, "Victory!", true);
        this.winnerName = winnerName;
        this.totalMoves = totalMoves;
        this.duration = duration;
        this.piecesCount = piecesCount;
        this.onPlayAgain = onPlayAgain;
        this.onReturnToMenu = onReturnToMenu;
        
        setupUI();
        initializeConfetti();
        startAnimations();
    }
    
    private void setupUI() {
        setUndecorated(true);
        setSize(500, 450);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        
        // Main content panel with custom painting
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw confetti
                for (Confetti c : confettiList) {
                    g2d.setColor(c.color);
                    g2d.fillOval((int)c.x, (int)c.y, c.size, c.size);
                }
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(240, 240, 245));
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.PRIMARY_COLOR, 3),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));
        
        // Trophy and title panel
        JPanel topPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw animated trophy
                int centerX = getWidth() / 2;
                Font trophyFont = new Font("Segoe UI Emoji", Font.PLAIN, (int)(60 * trophyScale));
                g2d.setFont(trophyFont);
                FontMetrics fm = g2d.getFontMetrics();
                String trophy = "🏆";
                int trophyWidth = fm.stringWidth(trophy);
                g2d.setColor(new Color(255, 215, 0));  // Gold color
                g2d.drawString(trophy, centerX - trophyWidth / 2, 50);
            }
        };
        topPanel.setOpaque(false);
        topPanel.setPreferredSize(new Dimension(500, 70));
        topPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        contentPanel.add(topPanel);
        
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Victory title
        JLabel victoryLabel = new JLabel("VICTORY!");
        victoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        victoryLabel.setForeground(UIConstants.PRIMARY_COLOR);
        victoryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(victoryLabel);
        
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Winner name
        JLabel winnerLabel = new JLabel(winnerName + " Wins!");
        winnerLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        winnerLabel.setForeground(new Color(46, 125, 50));
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(winnerLabel);
        
        contentPanel.add(Box.createVerticalStrut(25));
        
        // Statistics panel
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
            "📊 Game Statistics",
            0,
            0,
            new Font("Segoe UI", Font.BOLD, 14),
            UIConstants.TEXT_COLOR
        ));
        
        statsPanel.add(Box.createVerticalStrut(10));
        statsPanel.add(createStatLabel("Moves: " + totalMoves));
        statsPanel.add(Box.createVerticalStrut(5));
        statsPanel.add(createStatLabel("Duration: " + duration));
        statsPanel.add(Box.createVerticalStrut(5));
        statsPanel.add(createStatLabel("Pieces: " + piecesCount));
        statsPanel.add(Box.createVerticalStrut(10));
        
        contentPanel.add(statsPanel);
        
        contentPanel.add(Box.createVerticalStrut(25));
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        
        JButton playAgainBtn = UIConstants.createStyledButton("🎮 Play Again");
        playAgainBtn.setBackground(UIConstants.SUCCESS_COLOR);
        playAgainBtn.addActionListener(e -> {
            stopAnimations();
            dispose();
            if (onPlayAgain != null) onPlayAgain.run();
        });
        
        JButton menuBtn = UIConstants.createStyledButton("🏠 Main Menu");
        menuBtn.addActionListener(e -> {
            stopAnimations();
            dispose();
            if (onReturnToMenu != null) onReturnToMenu.run();
        });
        
        buttonPanel.add(playAgainBtn);
        buttonPanel.add(menuBtn);
        contentPanel.add(buttonPanel);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // ESC to close
        getRootPane().registerKeyboardAction(
            e -> {
                stopAnimations();
                dispose();
                if (onReturnToMenu != null) onReturnToMenu.run();
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel("  • " + text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        label.setForeground(UIConstants.TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
    
    private void initializeConfetti() {
        Random rand = new Random();
        for (int i = 0; i < 50; i++) {
            confettiList.add(new Confetti(
                rand.nextInt(getWidth()),
                -rand.nextInt(200)
            ));
        }
    }
    
    private void startAnimations() {
        // Fade-in animation
        Timer fadeTimer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fadeAlpha < 255) {
                    fadeAlpha += 15;
                    if (fadeAlpha > 255) fadeAlpha = 255;
                } else {
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        fadeTimer.start();
        
        // Main animation timer (confetti + trophy pulse)
        animationTimer = new Timer(16, e -> {
            // Update confetti
            for (Confetti c : confettiList) {
                c.update();
                // Reset confetti that falls below screen
                if (c.y > getHeight()) {
                    c.y = -10;
                    c.x = new Random().nextInt(getWidth());
                    c.vy = new Random().nextDouble() * 2 + 1;
                }
            }
            
            // Animate trophy scale (pulse effect)
            if (trophyGrowing) {
                trophyScale += 0.01f;
                if (trophyScale >= 1.2f) trophyGrowing = false;
            } else {
                trophyScale -= 0.01f;
                if (trophyScale <= 1.0f) trophyGrowing = true;
            }
            
            repaint();
        });
        animationTimer.start();
    }
    
    private void stopAnimations() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
    
    @Override
    public void dispose() {
        stopAnimations();
        super.dispose();
    }
}
