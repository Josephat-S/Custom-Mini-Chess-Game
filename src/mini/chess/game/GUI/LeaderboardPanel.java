package mini.chess.game.GUI;

import mini.chess.game.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LeaderboardPanel extends JPanel {

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Rank", "Player", "Score"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel statusLabel = UIConstants.createStyledLabel("Loading leaderboard...");

    private final Runnable onBack;

    public LeaderboardPanel(Runnable onBack) {
        this.onBack = onBack;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(UIConstants.PADDING_BORDER);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(UIConstants.BACKGROUND_COLOR);
        JLabel titleLabel = new JLabel("🏆 Leaderboard");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setForeground(UIConstants.TEXT_COLOR);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        table.setRowHeight(28);
        table.setFont(UIConstants.LABEL_FONT);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(UIConstants.BORDER_COLOR);
        table.getTableHeader().setFont(UIConstants.SUBTITLE_FONT);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(UIConstants.BACKGROUND_COLOR);
        add(scroll, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(UIConstants.PANEL_COLOR);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JButton refreshBtn = UIConstants.createStyledButton("🔄 Refresh");
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(e -> loadData());

        JButton backBtn = UIConstants.createStyledButton("⬅ Back");
        backBtn.setBackground(UIConstants.DANGER_COLOR);
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> { if (onBack != null) onBack.run(); });

        rightPanel.add(statusLabel);
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(refreshBtn);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(backBtn);
        rightPanel.add(Box.createVerticalGlue());

        add(rightPanel, BorderLayout.EAST);

        loadData();
    }

    private void loadData() {
        statusLabel.setText("Loading leaderboard...");
        statusLabel.setForeground(UIConstants.TEXT_COLOR);
        model.setRowCount(0);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                String[] views = { "leaderboards", "leaderboard" }; // prefer plural; fallback to singular
                boolean loaded = false;

                try (Connection conn = DBConnection.getConnection()) {
                    if (conn == null) {
                        setStatus("DB connection failed", true);
                        return null;
                    }
                    for (String vw : views) {
                        String sql = "SELECT rank_no, username, score FROM " + vw + " ORDER BY rank_no ASC";
                        try (PreparedStatement ps = conn.prepareStatement(sql);
                             ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int rank = rs.getInt(1);
                                String name = rs.getString(2);
                                int score = rs.getInt(3);
                                model.addRow(new Object[]{rank, name, score});
                            }
                            loaded = true;
                            break;
                        } catch (SQLException ex) {
                            // try next view name
                        }
                    }
                } catch (SQLException e) {
                    setStatus("DB error: " + e.getMessage(), true);
                    return null;
                }

                if (!loaded) {
                    setStatus("Leaderboard view not found", true);
                }
                return null;
            }

            @Override
            protected void done() {
                if (model.getRowCount() > 0) {
                    setStatus("Loaded " + model.getRowCount() + " entries", false);
                } else {
                    setStatus("No leaderboard data", false);
                }
            }
        };
        worker.execute();
    }

    private void setStatus(String msg, boolean danger) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setForeground(danger ? UIConstants.DANGER_COLOR : UIConstants.TEXT_COLOR);
        });
    }
}