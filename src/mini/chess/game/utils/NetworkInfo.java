package mini.chess.game.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Retrieves a likely LAN IPv4 address (skips loopback, virtual, APIPA).
 */
public class NetworkInfo {

    public static String getLocalIPv4() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress ia = addrs.nextElement();
                    if (!ia.isLoopbackAddress() && ia instanceof java.net.Inet4Address) {
                        String host = ia.getHostAddress();
                        if (host.startsWith("169.254.")) continue;
                        return host;
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
