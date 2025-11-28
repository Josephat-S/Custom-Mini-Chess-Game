package mini.chess.game.GUI;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class UIConstants {

    public static final Color BACKGROUND_COLOR = new Color(0x2C, 0x2C, 0x2C);
    public static final Color PANEL_COLOR = new Color(0x3C, 0x3C, 0x3C);
    public static final Color PRIMARY_COLOR = new Color(0x4A, 0x9F, 0xF5);
    public static final Color PRIMARY_COLOR_DARKER = new Color(0x2E, 0x7D, 0xC3);
    public static final Color SUCCESS_COLOR = new Color(0x4C, 0xAF, 0x50);
    public static final Color DANGER_COLOR = new Color(0xF4, 0x43, 0x36);
    public static final Color WARNING_COLOR = new Color(0xFF, 0x98, 0x00);
    public static final Color TEXT_COLOR = new Color(0xE0, 0xE0, 0xE0);
    public static final Color BORDER_COLOR = new Color(0x55, 0x55, 0x55);

    public static final Color LIGHT_SQUARE = new Color(0xF0, 0xD9, 0xB5);
    public static final Color DARK_SQUARE = new Color(0xB5, 0x88, 0x63);
    public static final Color HIGHLIGHT_COLOR = new Color(0xFF, 0xD7, 0x00);
    public static final Color SELECTED_COLOR = new Color(0x90, 0xEE, 0x90);

    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font PIECE_FONT = new Font("Serif", Font.BOLD, 36);

    public static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(20, 20, 20, 20);
    public static final Border FIELD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
    );

    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(PRIMARY_COLOR_DARKER);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorderPainted(false);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR_DARKER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR);
            }
        });

        return button;
    }

    public static JTextField createStyledTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setBorder(FIELD_BORDER);
        field.setFont(LABEL_FONT);
        field.setBackground(Color.WHITE);
        field.setForeground(Color.BLACK);
        return field;
    }

    public static JPasswordField createStyledPasswordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setBorder(FIELD_BORDER);
        field.setFont(LABEL_FONT);
        field.setBackground(Color.WHITE);
        field.setForeground(Color.BLACK);
        return field;
    }

    public static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }
}