package mini.chess.game.app;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import mini.chess.game.Models.AIPlayer;
import mini.chess.game.Models.Board;
import mini.chess.game.db.AuthManager;
import mini.chess.game.db.GameDataManager;
import mini.chess.game.Network.Client;
import mini.chess.game.Network.Server;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int userId = authFlow(sc);
        if (userId == -1) {
            sc.close();
            return;
        }

        boolean exitGame = false;
        while (!exitGame) {
            System.out.println("\n=== MINI CHESS GAME ===");
            System.out.println("1. New Local Game");
            System.out.println("2. New LAN Game (Host)");
            System.out.println("3. Join LAN Game");
            System.out.println("4. Load Saved Game");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            int choice = 0;
            try {
                choice = sc.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Numbers only!");
                sc.nextLine();
                continue;
            }
            switch (choice) {
                case 1 -> startNewGameLocal(sc, userId);
                case 2 -> startNewGameHost(sc, userId);
                case 3 -> joinLanGame(sc, userId);
                case 4 -> loadSavedGame(sc, userId);
                case 5 -> exitGame = true;
                default -> System.out.println("Invalid choice!");
            }
        }
        sc.close();
    }

    private static void startNewGameLocal(Scanner sc, int userId) {
        System.out.println("Local opponent: 1) Human  2) AI");
        int opp = 1;
        try {
            opp = sc.nextInt();
        } catch (InputMismatchException e) {
            sc.nextLine();
            System.out.println("Numbers only!");
            return;
        }
        int aiDepth = 2;
        if (opp == 2) {
            System.out.println("Select AI difficulty: ");
            try {
                aiDepth = sc.nextInt();
            } catch (Exception e) {
                aiDepth = 2;
            }
        }
        AIPlayer ai = new AIPlayer();
        ai.setSearchDepth(aiDepth);
        Board board = new Board();
        board.displayBoard();

        // Create game in DB for recording moves
        GameDataManager.GameCreateResult res = GameDataManager.createLanGameForHost(userId, board, 1);
        if (res.gameId == -1) {
            System.out.println("Failed to create game in DB.");
            return;
        }
        int gameId = res.gameId;
        int playerId = res.playerId;

        String currentPlayer = "Player1";
        boolean gameOver = false;
        int moveCounter = 0;
        while (!gameOver) {
            try {
                if (opp == 2 && currentPlayer.equals("Player2")) {
                    ai.makeBestMove(board, "Player2");
                    // Record AI move
                    GameDataManager.recordMoveAndUpdateState(gameId, playerId, moveCounter, "ai_move", "ai_move", board);
                    moveCounter++;
                    board.displayBoard();
                } else {
                    System.out.print(currentPlayer + " move (fromRow fromCol toRow toCol): ");
                    int fromRow = sc.nextInt();
                    int fromCol = sc.nextInt();
                    int toRow = sc.nextInt();
                    int toCol = sc.nextInt();
                    board.movePiece(fromRow, fromCol, toRow, toCol);
                    // Record human move
                    GameDataManager.recordMoveAndUpdateState(gameId, playerId, moveCounter, fromRow + "_" + fromCol, toRow + "_" + toCol, board);
                    moveCounter++;
                    board.displayBoard();
                }
                String winner = board.checkWinner();
                if (winner != null) {
                    System.out.println("GAME OVER: " + winner);
                    gameOver = true;
                    break;
                }
                String status = board.checkStatus(currentPlayer);
                if ("CHECKMATE".equals(status) || "DRAW".equals(status)) {
                    System.out.println(status);
                    gameOver = true;
                }
                if (!gameOver) currentPlayer = currentPlayer.equals("Player1") ? "Player2" : "Player1";
            } catch (Exception e) {
                System.out.println("Invalid input.");
                sc.nextLine();
            }
        }
    }

    private static void startNewGameHost(Scanner sc, int userId) {
        System.out.print("Enter port to host on (e.g. 9000): ");
        int port = sc.nextInt();
        try (Server server = new Server(port)) {
            System.out.println("Hosting... waiting for client to connect.");
            server.accept();
            System.out.println("Client connected.");

            final Object boardLock = new Object();
            final AtomicReference<Board> boardRef = new AtomicReference<>(new Board());
            boardRef.get().displayBoard();

            GameDataManager.GameCreateResult res = GameDataManager.createLanGameForHost(userId, boardRef.get(), 1);
            if (res.gameId == -1) {
                System.out.println("Failed to create game in DB.");
                return;
            }
            int gameId = res.gameId;
            int hostPlayerId = res.playerId;

            server.send("GAMEID " + gameId);

            AtomicReference<Integer> clientPlayerId = new AtomicReference<>(-1);
            AtomicReference<String> currentPlayer = new AtomicReference<>("Player1");
            AtomicInteger moveCounter = new AtomicInteger(0);
            AtomicBoolean gameOver = new AtomicBoolean(false);

            // Listener thread to process client messages
            Thread listener = new Thread(() -> {
                while (!gameOver.get()) {
                    try {
                        String incoming = server.pollMessage();
                        if (incoming != null) {
                            if (incoming.startsWith("HELLO ")) {
                                try {
                                    int clientUserId = Integer.parseInt(incoming.substring(6));
                                    int addedPlayerId = GameDataManager.addPlayerToExistingGame(gameId, clientUserId);
                                    if (addedPlayerId != -1) {
                                        clientPlayerId.set(addedPlayerId);
                                        server.send("HELLO_ACK");
                                        System.out.println("Client added to game (player id " + addedPlayerId + ").");
                                    } else {
                                        System.err.println("Failed to add client player.");
                                    }
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid HELLO message.");
                                }
                            } else if (incoming.startsWith("MOVE ")) {
                                String[] parts = incoming.substring(5).split(" ");
                                if (parts.length == 4) {
                                    try {
                                        int fromRow = Integer.parseInt(parts[0]);
                                        int fromCol = Integer.parseInt(parts[1]);
                                        int toRow = Integer.parseInt(parts[2]);
                                        int toCol = Integer.parseInt(parts[3]);
                                        if (clientPlayerId.get() != -1) {
                                            synchronized (boardLock) {
                                                boardRef.get().movePiece(fromRow, fromCol, toRow, toCol);
                                                GameDataManager.recordMoveAndUpdateState(gameId, clientPlayerId.get(), moveCounter.getAndIncrement(),
                                                        fromRow + "_" + fromCol, toRow + "_" + toCol, boardRef.get());
                                            }
                                            boardRef.get().displayBoard();
                                            server.send("SYNC " + GameDataManager.boardToStringForNetwork(boardRef.get()));
                                            // switch turn back to host
                                            currentPlayer.set("Player1");
                                            String winner = boardRef.get().checkWinner();
                                            if (winner != null) {
                                                System.out.println("GAME OVER: " + winner);
                                                gameOver.set(true);
                                            }
                                        } else {
                                            System.err.println("Client move received before client was added to DB.");
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Invalid move from client.");
                                    }
                                }
                            }
                        } else {
                            TimeUnit.MILLISECONDS.sleep(50);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Host-Listener");
            listener.setDaemon(true);
            listener.start();

            // Main thread: host input loop
            while (!gameOver.get()) {
                if ("Player1".equals(currentPlayer.get())) {
                    System.out.print("Your move (fromRow fromCol toRow toCol): ");
                    try {
                        int fromRow = sc.nextInt();
                        int fromCol = sc.nextInt();
                        int toRow = sc.nextInt();
                        int toCol = sc.nextInt();
                        synchronized (boardLock) {
                            boardRef.get().movePiece(fromRow, fromCol, toRow, toCol);
                            GameDataManager.recordMoveAndUpdateState(gameId, hostPlayerId, moveCounter.getAndIncrement(),
                                    fromRow + "_" + fromCol, toRow + "_" + toCol, boardRef.get());
                        }
                        boardRef.get().displayBoard();
                        server.send("SYNC " + GameDataManager.boardToStringForNetwork(boardRef.get()));
                        currentPlayer.set("Player2");
                        String winner = boardRef.get().checkWinner();
                        if (winner != null) {
                            System.out.println("GAME OVER: " + winner);
                            gameOver.set(true);
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                        sc.nextLine();
                        continue;
                    }
                } else {
                    // not our turn; sleep shortly to avoid busy loop
                    try {
                        TimeUnit.MILLISECONDS.sleep(80);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // wait a short moment for listener to finish
            try {
                listener.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (IOException e) {
            System.out.println("Hosting error: " + e.getMessage());
        }
    }

    private static void joinLanGame(Scanner sc, int userId) {
        sc.nextLine();
        System.out.print("Host address: ");
        String host = sc.nextLine();
        System.out.print("Host port: ");
        int port = sc.nextInt();
        try (Client client = new Client(host, port)) {
            System.out.println("Connected to host.");

            // Wait for GAMEID before sending HELLO
            String gameIdMsg = null;
            while (gameIdMsg == null) {
                String incoming = client.pollMessage();
                if (incoming != null && incoming.startsWith("GAMEID ")) {
                    gameIdMsg = incoming;
                    System.out.println("Joined game: " + incoming.substring(7));
                } else {
                    TimeUnit.MILLISECONDS.sleep(50);
                }
            }

            client.send("HELLO " + userId);

            final Object boardLock = new Object();
            final AtomicReference<Board> boardRef = new AtomicReference<>(new Board());
            boardRef.get().displayBoard();
            AtomicReference<String> currentPlayer = new AtomicReference<>("Player2");
            AtomicBoolean gameOver = new AtomicBoolean(false);

            // Listener thread to process host messages
            Thread listener = new Thread(() -> {
                while (!gameOver.get()) {
                    try {
                        String incoming = client.pollMessage();
                        if (incoming != null) {
                            if (incoming.startsWith("SYNC ")) {
                                String boardJson = incoming.substring(5);
                                Board newBoard = GameDataManager.boardFromStringForNetwork(boardJson);
                                synchronized (boardLock) {
                                    boardRef.set(newBoard);
                                }
                                boardRef.get().displayBoard();
                                currentPlayer.set("Player2");
                            } else if (incoming.startsWith("HELLO_ACK")) {
                                System.out.println("Host acknowledged join.");
                            }
                        } else {
                            TimeUnit.MILLISECONDS.sleep(50);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("Error processing host message: " + e.getMessage());
                    }
                }
            }, "Client-Listener");
            listener.setDaemon(true);
            listener.start();

            boolean localGameOver = false;
            while (!localGameOver && !gameOver.get()) {
                if ("Player2".equals(currentPlayer.get())) {
                    System.out.print("Your move (fromRow fromCol toRow toCol): ");
                    try {
                        int fromRow = sc.nextInt();
                        int fromCol = sc.nextInt();
                        int toRow = sc.nextInt();
                        int toCol = sc.nextInt();
                        synchronized (boardLock) {
                            boardRef.get().movePiece(fromRow, fromCol, toRow, toCol);
                        }
                        boardRef.get().displayBoard();
                        // Send move to host; host will record and SYNC back
                        client.send("MOVE " + fromRow + " " + fromCol + " " + toRow + " " + toCol);
                        // After sending, wait for host SYNC; disable further input until SYNC arrives
                        currentPlayer.set("Player1");
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                        sc.nextLine();
                        continue;
                    }
                } else {
                    try {
                        TimeUnit.MILLISECONDS.sleep(80);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                String winner = boardRef.get().checkWinner();
                if (winner != null) {
                    System.out.println("GAME OVER: " + winner);
                    localGameOver = true;
                    gameOver.set(true);
                    break;
                }
                String status = boardRef.get().checkStatus(currentPlayer.get());
                if ("CHECKMATE".equals(status) || "DRAW".equals(status)) {
                    System.out.println(status);
                    localGameOver = true;
                    gameOver.set(true);
                }
            }

            try {
                listener.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void loadSavedGame(Scanner sc, int userId) {
        List<Integer> saved = GameDataManager.listSavedGamesForUser(userId);
        if (saved.isEmpty()) {
            System.out.println("No saved games found.");
            return;
        }
        System.out.println("Saved games:");
        for (int i = 0; i < saved.size(); i++) System.out.println((i + 1) + ". Game ID: " + saved.get(i));
        System.out.print("Choose: ");
        int sel = sc.nextInt();
        if (sel < 1 || sel > saved.size()) {
            System.out.println("Invalid selection.");
            return;
        }
        Board loaded = GameDataManager.loadGameById(saved.get(sel - 1));
        if (loaded != null) {
            System.out.println("Loaded board:");
            loaded.displayBoard();
            startNewGameLocal(sc, userId);
        } else System.out.println("Failed to load.");
    }

    private static int authFlow(Scanner sc) {
        System.out.println("Welcome. Please login or register.");
        while (true) {
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.print("Choice: ");
            int c = 0;
            try {
                c = sc.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Numbers only");
                sc.nextLine();
                continue;
            }
            sc.nextLine();
            if (c == 1) {
                System.out.print("Username: ");
                String u = sc.nextLine();
                System.out.print("Password: ");
                String p = sc.nextLine();
                int userId = AuthManager.login(u, p);
                if (userId != -1) return userId;
                System.out.println("Login failed.");
            } else if (c == 2) {
                System.out.print("Choose username: ");
                String u = sc.nextLine();
                System.out.print("Choose password: ");
                String p = sc.nextLine();
                int userId = AuthManager.register(u, p);
                if (userId != -1) return userId;
                System.out.println("Registration failed (username may exist).");
            }
        }
    }
}
