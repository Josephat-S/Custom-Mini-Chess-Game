// java
package mini.chess.game.utils;

import mini.chess.game.db.DBConnection;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class LogManager {

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LogManager-Worker");
        t.setDaemon(true);
        return t;
    });

    private LogManager() { /* no-op */ }

    public static void logAction(int userId, String action) {
        if (action == null || userId <= 0) return;
        EXEC.submit(() -> writeLog(userId, action));
    }

    private static void writeLog(int userId, String action) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;
            boolean prevAuto = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                String sql = "INSERT INTO user_logs (user_id, log_time, action) VALUES (?, CURRENT_TIMESTAMP, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, action);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                try { if (conn != null) conn.rollback(); } catch (Exception ignore) {}
            } finally {
                try { conn.setAutoCommit(prevAuto); } catch (Exception ignore) {}
            }
        } catch (Exception ignored) { }
    }

    public static void shutdown() {
        EXEC.shutdown();
        try {
            if (!EXEC.awaitTermination(2, TimeUnit.SECONDS)) EXEC.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            EXEC.shutdownNow();
        }
    }
}
