package mini.chess.game.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Manages Windows Defender Firewall inbound rules for hosting.
 * Uses `netsh` commands; requires the process to have Administrator rights.
 * Safe no-ops on non-Windows systems.
 */
public class FirewallRuleManager {

    private static final String RULE_PREFIX = "MiniChessHostPort_";

    public static void addFirewallRule(int port) {
        if (!isWindows()) return;
        String ruleName = RULE_PREFIX + port;
        if (ruleExists(ruleName)) return;
        String cmd = "netsh advfirewall firewall add rule name=\"" + ruleName + "\" dir=in action=allow protocol=TCP localport=" + port + " remoteip=localsubnet enable=yes";
        run(cmd);
    }

    public static void removeFirewallRule(int port) {
        if (!isWindows()) return;
        String ruleName = RULE_PREFIX + port;
        if (!ruleExists(ruleName)) return;
        String cmd = "netsh advfirewall firewall delete rule name=\"" + ruleName + "\"";
        run(cmd);
    }

    private static boolean ruleExists(String ruleName) {
        String cmd = "netsh advfirewall firewall show rule name=\"" + ruleName + "\"";
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", cmd).redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("Rule Name") && line.contains(ruleName)) return true;
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return false;
    }

    private static void run(String cmd) {
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", cmd).inheritIO().start();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
