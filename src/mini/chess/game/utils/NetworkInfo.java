package mini.chess.game.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Retrieves a likely LAN IPv4 address (skips loopback, virtual, APIPA).
 * Prefers Wi\-Fi / wireless interfaces when available.
 */
public class NetworkInfo {

    public static String getLocalIPv4() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            List<InetAddress> wifiCandidates = new ArrayList<>();
            List<InetAddress> otherCandidates = new ArrayList<>();

            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                try {
                    if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                } catch (SocketException e) {
                    continue;
                }

                String name = ni.getName() == null ? "" : ni.getName().toLowerCase();
                String display = ni.getDisplayName() == null ? "" : ni.getDisplayName().toLowerCase();
                boolean isWifi = name.contains("wlan") || name.contains("wifi") || name.contains("wi-fi")
                        || name.startsWith("wl") || display.contains("wireless") || display.contains("wifi")
                        || display.contains("wi-fi");

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress ia = addrs.nextElement();
                    if (ia instanceof Inet4Address && !ia.isLoopbackAddress()) {
                        String host = ia.getHostAddress();
                        if (host.startsWith("169.254.")) continue; // skip APIPA
                        if (isWifi) wifiCandidates.add(ia);
                        else otherCandidates.add(ia);
                    }
                }
            }

            if (!wifiCandidates.isEmpty()) return wifiCandidates.get(0).getHostAddress();
            if (!otherCandidates.isEmpty()) return otherCandidates.get(0).getHostAddress();
        } catch (Exception ignored) {}

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
