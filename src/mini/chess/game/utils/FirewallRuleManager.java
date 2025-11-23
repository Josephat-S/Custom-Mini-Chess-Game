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

    /**
     * Adds a firewall rule for the specified port.
     * @param port The port to allow
     * @return true if rule was added successfully or already exists, false otherwise
     */
    public static boolean addFirewallRule(int port) {
        if (!isWindows()) return true; // Non-Windows systems don't need this
        String ruleName = RULE_PREFIX + port;
        if (ruleExists(ruleName)) return true;
        
        // Use remoteip=any instead of localsubnet for broader compatibility
        String cmd = "netsh advfirewall firewall add rule name=\"" + ruleName + "\" dir=in action=allow protocol=TCP localport=" + port + " remoteip=any enable=yes";
        boolean success = run(cmd);
        
        // Verify the rule was created
        return success && ruleExists(ruleName);
    }

    /**
     * Removes the firewall rule for the specified port.
     * @param port The port to remove the rule for
     * @return true if rule was removed successfully or didn't exist, false otherwise
     */
    public static boolean removeFirewallRule(int port) {
        if (!isWindows()) return true;
        String ruleName = RULE_PREFIX + port;
        if (!ruleExists(ruleName)) return true;
        String cmd = "netsh advfirewall firewall delete rule name=\"" + ruleName + "\"";
        boolean success = run(cmd);
        
        // Verify the rule was removed
        return success && !ruleExists(ruleName);
    }
    
    /**
     * Checks if the current process is running with Administrator privileges.
     * @return true if running as admin, false otherwise
     */
    public static boolean isRunningAsAdmin() {
        if (!isWindows()) return true;
        
        try {
            // Try to execute a command that requires admin privileges
            String cmd = "net session";
            Process p = new ProcessBuilder("cmd.exe", "/c", cmd)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
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

    private static boolean run(String cmd) {
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", cmd)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
