package mini.chess.game.Network;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

/*
Simple LAN client counterpart.
*/
public class Client implements Closeable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final AtomicReference<String> lastLine = new AtomicReference<>(null);

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        new Thread(this::readerLoop, "lan-client-reader").start();
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