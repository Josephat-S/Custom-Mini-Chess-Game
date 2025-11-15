
# 🧩 Mini Chess Game

A custom mini chess game built with Java Swing, featuring local gameplay, AI opponent, LAN multiplayer, and a competitive leaderboard system.

## 👥 Team Members
- Josephat Sangwa

- Josiane Nikuze

- Ezechiel Ukwishaka

- Augustin Bizumuremyi

- Clementine Nabayo

## 📋 Features

### 🎮 Game Modes
- *Local Game*: Play against another human player or an AI opponent on the same machine
- *LAN Multiplayer*: Host or join games over local network
- *Save/Load*: Save your progress and resume games later

### 🏆 Leaderboard System
- View global player rankings based on scores
- Real-time score tracking from database
- Automatic score updates when games are won
- Rank, player name, and score display

### 🎯 Gameplay Features
- 5×5 custom chess board
- Two piece types: Leader (♔) and Soldier (♙)
- Turn-based movement system
- Win condition tracking
- Move history recording
- Real-time board synchronization in LAN games

### 🔐 User Authentication
- Secure login and registration system
- User session management
- Player profile tracking

### 🌐 Network Features
- Server-client architecture for LAN games
- Automatic firewall rule management (Windows)
- IPv4 address detection
- Move synchronization across network

### 🤖 AI Opponent
- Configurable search depth
- Strategic move evaluation
- Minimax algorithm implementation

## 🛠 Technical Stack

### Technologies
- *Language*: Java 17+
- *UI Framework*: Swing
- *Database*: PostgreSQL
- *Network*: Java Socket Programming

### Key Components

#### GUI Layer (src/mini/chess/game/GUI/)
- GameUI.java - Main application frame with CardLayout navigation
- GameBoardPanel.java - Interactive chess board display
- LeaderboardPanel.java - Ranking and score display
- LanHostPanel.java - LAN game hosting interface
- LanJoinPanel.java - LAN game joining interface
- UIConstants.java - Centralized styling constants

#### Game Logic (src/mini/chess/game/Models/)
- Board.java - Board state management
- Piece.java - Abstract piece class
- Leader.java - Leader piece (moves 1 square in any direction)
- Soldier.java - Soldier piece (moves 1 square forward/sideways)
- Move.java - Move representation
- AIPlayer.java - AI move calculation

#### Database Layer (src/mini/chess/game/db/)
- DBConnection.java - Database connection pool
- AuthManager.java - User authentication
- GameDataManager.java - Game state persistence

#### Network Layer (src/mini/chess/game/Network/)
- Server.java - LAN game server
- Client.java - LAN game client

#### Utilities (src/mini/chess/game/utils/)
- GameDataManager.java - Game data operations
- NetworkInfo.java - Network interface detection
- FirewallRuleManager.java - Windows firewall automation

## 🗄 Database Schema

### Tables
- *users*: User accounts (user_id, username, password_hash)
- *players*: Player profiles (player_id, user_id, score)
- *games*: Game records (game_id, type, status, start_time, end_time)
- *players_games*: Game participation (game_id, player_one_id, player_two_id, winner_id)
- *moves*: Move history (game_id, player_id, move_number, from_cell, to_cell)
- *gamestate*: Board snapshots (game_id, player_turn, board_data, last_move)

### Views
- *leaderboards*: Player rankings with rank, username, and score

```sql
CREATE OR REPLACE VIEW leaderboards AS
SELECT 
    p.player_id,
    u.username,
    p.score,
    ROW_NUMBER() OVER (ORDER BY p.score DESC, p.player_id ASC) AS rank_no
FROM players p
JOIN users u ON p.user_id = u.user_id
WHERE p.score > 0
ORDER BY p.score DESC;