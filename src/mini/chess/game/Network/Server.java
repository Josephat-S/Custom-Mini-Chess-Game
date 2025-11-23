// java
package mini.chess.game.Network;

import mini.chess.game.Models.Board;
import mini.chess.game.utils.GameDataManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;

/*
Simple LAN server:
- Accepts one client.
- Text protocol lines:
  READY <username>
  MOVE fromRow fromCol toRow toCol
  SAVE <userId> <currentTurn>   (client requests host to save)
  SYNC <boardData>              (optional full board sync)
  QUIT
Host can read client messages via getLastMessage().
*/
public class Server implements Closeable {
    private final ServerSocket serverSocket;
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private final AtomicReference<String> lastLine = new AtomicReference<>(null);
    private static final int ACCEPT_TIMEOUT_MS = 30000; // 30 seconds

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket();
        // Enable SO_REUSEADDR to prevent "address already in use" errors
        serverSocket.setReuseAddress(true);
        // Bind to all interfaces (0.0.0.0) to ensure LAN accessibility
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        // Set accept timeout to prevent indefinite blocking
        serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MS);
    }

    // blocking accept
    public void accept() throws IOException {
        client = serverSocket.accept();
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true);
        // start reader thread
        new Thread(this::readerLoop, "lan-server-reader").start();
    }

    private void readerLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                lastLine.set(line.trim());
                // auto-handle SAVE requests from client: "SAVE <userId> <turn> <boardData...>"
                if (line.startsWith("SAVE ")) {
                    String[] parts = line.split(" ", 4);
                    try {
                        int userId = Integer.parseInt(parts[1]);
                        int turn = Integer.parseInt(parts[2]);
                        String boardData = parts.length >= 4 ? parts[3] : "";
                        // let GameDataManager handle board string deserialization internally by providing boardData
                        GameDataManager.saveUnfinishedGameWithBoardData(userId, boardData, turn);
                        send("SAVED_OK");
                    } catch (Exception e) {
                        send("SAVED_ERR " + e.getMessage());
                    }
                }
            }
        } catch (IOException ignored) { }
    }

    public String pollMessage() {
        return lastLine.getAndSet(null);
    }

    public void send(String msg) {
        if (out != null) out.println(msg);
    }

    @Override
    public void close() throws IOException {
        try { if (client != null) client.close(); } finally { serverSocket.close(); }
    }
}
