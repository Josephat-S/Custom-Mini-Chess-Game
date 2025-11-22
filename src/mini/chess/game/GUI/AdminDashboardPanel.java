package mini.chess.game.GUI;

import mini.chess.game.db.AdminDAO;
import mini.chess.game.db.AuthManager;
import mini.chess.game.utils.LogManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

public class AdminDashboardPanel extends JPanel {

    private final Runnable onLogout;
    private JTable logsTable;
    private JTable gamesTable;
    private JTable playersTable;

    public AdminDashboardPanel(int userId, Runnable onLogout) {
        this.onLogout = onLogout;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);

        add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("System Logs", createLogsPanel());
        tabbedPane.addTab("Game History", createGamesPanel());
        tabbedPane.addTab("User Management", createUserManagementPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ... (keep createHeader, createLogsPanel, createGamesPanel as is)

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PANEL_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(UIConstants.TITLE_FONT);
        title.setForeground(UIConstants.TEXT_COLOR);

        JButton logoutBtn = UIConstants.createStyledButton("Logout");
        logoutBtn.setBackground(UIConstants.DANGER_COLOR);
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Logout?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                onLogout.run();
            }
        });

        header.add(title, BorderLayout.WEST);
        header.add(logoutBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        Vector<String> columns = new Vector<>();
        columns.add("Log ID");
        columns.add("Username");
        columns.add("Action");
        columns.add("Time");

        Vector<Vector<Object>> data = AdminDAO.getAllLogs();
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        logsTable = new JTable(model);
        styleTable(logsTable);

        JScrollPane scroll = new JScrollPane(logsTable);
        panel.add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = UIConstants.createStyledButton("Refresh Logs");
        refreshBtn.addActionListener(e -> refreshLogs());
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        btnPanel.add(refreshBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshLogs() {
        DefaultTableModel model = (DefaultTableModel) logsTable.getModel();
        model.setDataVector(AdminDAO.getAllLogs(), getLogsColumns());
    }

    private Vector<String> getLogsColumns() {
        Vector<String> c = new Vector<>();
        c.add("Log ID"); c.add("Username"); c.add("Action"); c.add("Time");
        return c;
    }

    private JPanel createGamesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        Vector<String> columns = new Vector<>();
        columns.add("ID"); columns.add("Type"); columns.add("Player 1"); columns.add("Player 2");
        columns.add("Winner"); columns.add("Status"); columns.add("Start"); columns.add("End");

        Vector<Vector<Object>> data = AdminDAO.getAllGames();
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        gamesTable = new JTable(model);
        styleTable(gamesTable);

        JScrollPane scroll = new JScrollPane(gamesTable);
        panel.add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = UIConstants.createStyledButton("Refresh Games");
        refreshBtn.addActionListener(e -> {
            DefaultTableModel m = (DefaultTableModel) gamesTable.getModel();
            m.setDataVector(AdminDAO.getAllGames(), columns);
        });
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        btnPanel.add(refreshBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);

        Vector<String> columns = new Vector<>();
        columns.add("ID"); columns.add("Username"); columns.add("Role"); columns.add("Status");

        Vector<Vector<Object>> data = AdminDAO.getAllUsers();
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable usersTable = new JTable(model);
        styleTable(usersTable);

        JScrollPane scroll = new JScrollPane(usersTable);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JButton refreshBtn = UIConstants.createStyledButton("Refresh");
        JButton editBtn = UIConstants.createStyledButton("Edit Username");
        JButton lockBtn = UIConstants.createStyledButton("Lock/Unlock");
        JButton deleteBtn = UIConstants.createStyledButton("Delete User");
        deleteBtn.setBackground(UIConstants.DANGER_COLOR);

        refreshBtn.addActionListener(e -> {
            DefaultTableModel m = (DefaultTableModel) usersTable.getModel();
            m.setDataVector(AdminDAO.getAllUsers(), columns);
        });

        editBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a user to edit", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int userId = Integer.parseInt(usersTable.getValueAt(row, 0).toString());
            String currentName = usersTable.getValueAt(row, 1).toString();
            String newName = JOptionPane.showInputDialog(this, "Enter new username:", currentName);
            
            if (newName != null && !newName.trim().isEmpty() && !newName.equals(currentName)) {
                if (AdminDAO.updateUser(userId, newName.trim())) {
                    JOptionPane.showMessageDialog(this, "Username updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshBtn.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "Update failed (username might be taken)", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        lockBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a user to lock/unlock", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int userId = Integer.parseInt(usersTable.getValueAt(row, 0).toString());
            String status = usersTable.getValueAt(row, 3).toString();
            boolean isLocked = status.equals("Locked");
            
            if (AdminDAO.toggleUserLock(userId, !isLocked)) {
                JOptionPane.showMessageDialog(this, "User " + (isLocked ? "unlocked" : "locked") + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshBtn.doClick();
            } else {
                JOptionPane.showMessageDialog(this, "Action failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a user to delete", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int userId = Integer.parseInt(usersTable.getValueAt(row, 0).toString());
            String username = usersTable.getValueAt(row, 1).toString();
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete user '" + username + "'?\nThis cannot be undone.", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
            if (confirm == JOptionPane.YES_OPTION) {
                if (AdminDAO.deleteUser(userId)) {
                    JOptionPane.showMessageDialog(this, "User deleted!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshBtn.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "Delete failed", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(editBtn);
        btnPanel.add(lockBtn);
        btnPanel.add(deleteBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(25);
        table.getTableHeader().setFont(UIConstants.BUTTON_FONT);
        table.setFont(UIConstants.LABEL_FONT);
        table.setSelectionBackground(UIConstants.PRIMARY_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(UIConstants.BORDER_COLOR);
    }
}
