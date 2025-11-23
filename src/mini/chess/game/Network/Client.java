// java
package mini.chess.game.Network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/*
Simple LAN client counterpart with connection timeout and retry logic.
*/
public class Client implements Closeable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final AtomicReference<String> lastLine = new AtomicReference<>(null);
    
    private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 500;

    public Client(String host, int port) throws IOException {
        socket = connectWithRetry(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        new Thread(this::readerLoop, "lan-client-reader").start();
    }
    
    private Socket connectWithRetry(String host, int port) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                Socket s = new Socket();
                // Enable TCP keepalive to detect dead connections
                s.setKeepAlive(true);
                // Set SO_REUSEADDR for better connection reuse
                s.setReuseAddress(true);
                // Connect with timeout
                s.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
                return s;
            } catch (SocketTimeoutException e) {
                lastException = new IOException("Connection timeout - host may not be running or firewall is blocking", e);
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(INITIAL_RETRY_DELAY_MS * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted", ie);
                    }
                }
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("refused")) {
                    lastException = new IOException("Connection refused - host not running or port is wrong", e);
                } else if (msg.contains("unreachable")) {
                    lastException = new IOException("Host unreachable - check network connection", e);
                } else {
                    lastException = new IOException("Connection failed: " + e.getMessage(), e);
                }
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(INITIAL_RETRY_DELAY_MS * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted", ie);
                    }
                }
            }
        }
        
        throw lastException != null ? lastException : new IOException("Failed to connect after " + MAX_RETRIES + " attempts");
    }

    private void readerLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                lastLine.set(line.trim());
            }
        } catch (IOException ignored) { }
    }

    public String pollMessage() {
        return lastLine.getAndSet(null);
    }

    public void send(String msg) {
        out.println(msg);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
