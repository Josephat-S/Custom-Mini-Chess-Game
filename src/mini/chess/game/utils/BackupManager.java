package mini.chess.game.utils;

import mini.chess.game.db.DBConnection;

import java.awt.Component;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class BackupManager {

    private static final String CONFIG_FILE = "backup_config.properties";
    private static final String KEY_CLOUD_PATH = "cloud_backup_path";

    public static boolean isInternetAvailable() {
        try {
            URL url = new URL("http://www.google.com");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000); // 3 seconds timeout
            conn.connect();
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static void performBackup(Component parent) {
        new Thread(() -> {
            try {
                File backupFile = generateSQLBackup();
                if (backupFile != null) {
                    uploadToCloud(backupFile, parent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static File generateSQLBackup() throws IOException, SQLException {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupDir = new File("backups");
        if (!backupDir.exists()) backupDir.mkdirs();
        
        File file = new File(backupDir, "mini_chess_backup_" + timeStamp + ".sql");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file));
             Connection conn = DBConnection.getConnection()) {
            
            writer.println("-- Mini Chess Backup " + timeStamp);
            writer.println("SET FOREIGN_KEY_CHECKS=0;");
            
            exportTable(conn, writer, "users");
            exportTable(conn, writer, "admins");
            exportTable(conn, writer, "players");
            exportTable(conn, writer, "games");
            exportTable(conn, writer, "players_games");
            exportTable(conn, writer, "moves");
            exportTable(conn, writer, "gamestate");
            exportTable(conn, writer, "user_logs");
            
            writer.println("SET FOREIGN_KEY_CHECKS=1;");
        }
        return file;
    }

    private static void exportTable(Connection conn, PrintWriter writer, String tableName) throws SQLException {
        String query = "SELECT * FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ").append(tableName).append(" VALUES (");
                
                for (int i = 1; i <= colCount; i++) {
                    Object val = rs.getObject(i);
                    if (val == null) {
                        sb.append("NULL");
                    } else if (val instanceof Number) {
                        sb.append(val);
                    } else {
                        String str = val.toString().replace("'", "''").replace("\\", "\\\\");
                        sb.append("'").append(str).append("'");
                    }
                    if (i < colCount) sb.append(", ");
                }
                sb.append(");");
                writer.println(sb.toString());
            }
        }
    }

    private static void uploadToCloud(File sourceFile, Component parent) {
        String cloudPath = loadCloudPath();
        
        if (cloudPath == null || !new File(cloudPath).exists()) {
            // Prompt user to select cloud folder
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(parent, 
                    "Internet detected! Would you like to configure a Cloud Backup folder (Google Drive/OneDrive)?", 
                    "Cloud Backup Setup", JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setDialogTitle("Select Cloud Sync Folder (e.g., Google Drive)");
                    
                    if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                        File selectedDir = chooser.getSelectedFile();
                        saveCloudPath(selectedDir.getAbsolutePath());
                        copyToCloud(sourceFile, selectedDir);
                    }
                }
            });
        } else {
            copyToCloud(sourceFile, new File(cloudPath));
        }
    }

    private static void copyToCloud(File source, File destDir) {
        try {
            File dest = new File(destDir, source.getName());
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup copied to cloud: " + dest.getAbsolutePath());
            
            // Show a non-intrusive notification
            SwingUtilities.invokeLater(() -> 
               JOptionPane.showMessageDialog(null, "Cloud Backup Complete!\nSaved to: " + dest.getAbsolutePath(), "Backup Success", JOptionPane.INFORMATION_MESSAGE));
            
        } catch (IOException e) {
            System.err.println("Failed to copy backup to cloud: " + e.getMessage());
        }
    }

    private static String loadCloudPath() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            return props.getProperty(KEY_CLOUD_PATH);
        } catch (IOException e) {
            return null;
        }
    }

    private static void saveCloudPath(String path) {
        Properties props = new Properties();
        props.setProperty(KEY_CLOUD_PATH, path);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Mini Chess Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
