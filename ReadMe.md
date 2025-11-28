
# 🧩 Mini Chess Game

A feature-rich custom mini chess game built with Java Swing, featuring local gameplay, AI opponent, LAN multiplayer with automatic firewall configuration, competitive leaderboard system, admin dashboard, and automatic cloud backup functionality.

## 👥 Team Members
- **Josephat Sangwa**
- **Josiane Nikuze**
- **Ezechiel Ukwishaka**
- **Augustin Bizumuremyi**
- **Clementine Nabayo**

---

## 📋 Features

### 🎮 Game Modes
- **Local Game**: Play against another human player or an AI opponent on the same machine
- **LAN Multiplayer**: Host or join games over local network with automatic firewall configuration
- **Save/Load**: Save your progress and resume games later
- **Offline Play**: Play on the same WLAN without internet connection

### 🏆 Leaderboard System
- View global player rankings based on scores
- Real-time score tracking from database
- Automatic score updates when games are won
- Dynamic ranking with rank, player name, and score display

### 🎯 Gameplay Features
- **5×5 custom chess board**
- **Two piece types**:
  - Leader (♔): Moves 1 square in any direction
  - Soldier (♙): Moves 1 square forward/sideways
- Turn-based movement system
- Win condition tracking (checkmate/draw detection)
- Complete move history recording
- Real-time board synchronization in LAN games

### 🔐 User Authentication & Security
- **Secure login and registration** with SHA-256 password hashing
- User session management
- Player profile tracking
- **Account locking/unlocking** (admin feature)
- Comprehensive user activity logging

### 🛡️ Admin Dashboard
Accessible only to users with admin privileges:
- **System Logs**: View all user actions (login, register, logout) with timestamps
- **Game History**: Monitor all games with player details, winners, and timestamps
- **User Management**:
  - Edit usernames
  - Lock/unlock user accounts
  - Delete users
  - View user roles and status
- Refresh functionality for real-time data updates

### ☁️ Automatic Cloud Backup System
- **Triggered on admin login** when internet is available
- Generates complete SQL database backups
- Saves to user-configured cloud-synced folder (Google Drive, OneDrive, etc.)
- **Backup includes**:
  - All user accounts
  - Player profiles and scores
  - Complete game history
  - Move records
  - System logs
- One-time cloud folder configuration with persistent settings
- Non-intrusive success notifications

### 🌐 Network Features
- **Server-client architecture** for LAN games
- **Automatic Windows Firewall rule management**:
  - Creates inbound rules when hosting
  - Removes rules when done
  - Administrator privilege detection
- IPv4 address detection and display
- Move synchronization across network
- Support for **offline LAN play** (no internet required)
- Configurable port selection

### 🤖 AI Opponent
- Configurable search depth for difficulty levels
- Strategic move evaluation using minimax algorithm
- Intelligent piece positioning and threat assessment

---

## 🛠 Technical Stack

### Technologies
- **Language**: Java 17+
- **UI Framework**: Swing (custom-styled components)
- **Database**: MySQL with connection pooling
- **Network**: Java Socket Programming
- **Security**: SHA-256 password hashing
- **File I/O**: Properties files for configuration

### Key Components

#### 🎨 GUI Layer (`src/mini/chess/game/GUI/`)
- **GameUI.java** - Main application frame with CardLayout navigation and backup triggers
- **GameBoardPanel.java** - Interactive 5×5 chess board with drag-and-drop support
- **LeaderboardPanel.java** - Dynamic ranking and score display
- **LanHostPanel.java** - LAN game hosting with firewall management
- **LanJoinPanel.java** - LAN game joining interface
- **AdminDashboardPanel.java** - Comprehensive admin control panel
- **UIConstants.java** - Centralized styling constants and theme

#### 🎲 Game Logic (`src/mini/chess/game/Models/`)
- **Board.java** - Board state management, move validation, win detection
- **Piece.java** - Abstract piece class
- **Leader.java** - Leader piece movement rules
- **Soldier.java** - Soldier piece movement rules
- **Move.java** - Move representation
- **AIPlayer.java** - Minimax algorithm implementation
- **Game.java** - Game state coordination
- **Player.java** - Player model

#### 💾 Database Layer (`src/mini/chess/game/db/`)
- **DBConnection.java** - MySQL connection pool management
- **AuthManager.java** - User authentication with SHA-256 hashing
- **GameDataManager.java** - Game state persistence and retrieval
- **AdminDAO.java** - Admin operations (logs, games, user management)

#### 🌐 Network Layer (`src/mini/chess/game/Network/`)
- **Server.java** - LAN game server with client handling
- **Client.java** - LAN game client connection
- **LANHandler.java** - Network protocol handler

#### 🔧 Utilities (`src/mini/chess/game/utils/`)
- **GameDataManager.java** - Game data operations and board serialization
- **NetworkInfo.java** - Network interface detection (IPv4 address)
- **FirewallRuleManager.java** - Windows Defender Firewall automation (netsh)
- **BackupManager.java** - Automatic SQL backup generation and cloud sync
- **LogManager.java** - User activity logging system

#### 🚀 Application Entry (`src/mini/chess/game/app/`)
- **Main.java** - Console-based application (CLI mode)
- **GameUI.java** - GUI application entry point

---

## 🗄 Database Schema

### Tables

#### `users`
- `user_id` (PK, AUTO_INCREMENT)
- `username` (UNIQUE, VARCHAR)
- `password_hash` (VARCHAR) - SHA-256 hashed
- `is_locked` (TINYINT) - Account lock status

#### `admins`
- `admin_id` (PK, AUTO_INCREMENT)
- `user_id` (FK → users.user_id)

#### `players`
- `player_id` (PK, AUTO_INCREMENT)
- `user_id` (FK → users.user_id)
- `score` (INT) - Competitive score

#### `games`
- `game_id` (PK, AUTO_INCREMENT)
- `type` (ENUM: 'local', 'lan', 'ai')
- `status` (ENUM: 'active', 'completed', 'abandoned')
- `start_time` (TIMESTAMP)
- `end_time` (TIMESTAMP)

#### `players_games`
- `game_id` (FK → games.game_id)
- `player_one_id` (FK → players.player_id)
- `player_two_id` (FK → players.player_id, NULLABLE)
- `winner_id` (FK → players.player_id, NULLABLE)

#### `moves`
- `move_id` (PK, AUTO_INCREMENT)
- `game_id` (FK → games.game_id)
- `player_id` (FK → players.player_id)
- `move_number` (INT)
- `from_cell` (VARCHAR) - Format: "row_col"
- `to_cell` (VARCHAR) - Format: "row_col"
- `timestamp` (TIMESTAMP)

#### `gamestate`
- `state_id` (PK, AUTO_INCREMENT)
- `game_id` (FK → games.game_id)
- `player_turn` (VARCHAR)
- `board_data` (TEXT) - Serialized board state (JSON)
- `last_move` (VARCHAR)
- `updated_at` (TIMESTAMP)

#### `user_logs`
- `log_id` (PK, AUTO_INCREMENT)
- `user_id` (FK → users.user_id)
- `action` (VARCHAR) - e.g., 'LOGIN', 'REGISTER', 'LOGOUT'
- `log_time` (TIMESTAMP)

### Views

#### `leaderboards`
Provides ranked player scores:

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
```

---

## 🚀 Getting Started

### System Requirements

- **Java Development Kit (JDK)**: 17 or higher
- **MySQL Server**: 8.0 or higher
- **MySQL Connector/J**: JDBC driver for MySQL (required in classpath)
- **Operating System**: 
  - Windows (full feature support including firewall management)
  - Linux/macOS (limited firewall features)
- **Memory**: Minimum 512 MB RAM
- **Network**: For LAN multiplayer functionality
- **Administrator Privileges**: Required for automatic firewall configuration on Windows

### Database Setup

1. **Create the database**:
   ```sql
   CREATE DATABASE mini_chess;
   USE mini_chess;
   ```

2. **Execute the SQL Script**:
   Run the `resources/mini_chess.sql` script to create all tables and views.
   ```bash
   mysql -u root -p mini_chess < resources/mini_chess.sql
   ```
   *(Or copy-paste the SQL commands from the schema section above if the file is missing)*

3. **Configure Database Connection**:
   Create or edit the `db.properties` file in the project root directory:
   ```properties
   # Database Configuration
   db.url=jdbc:mysql://localhost:3306/mini_chess?serverTimezone=UTC&useSSL=false
   db.user=your_mysql_username
   db.password=your_mysql_password
   ```

### Running the Application

#### GUI Mode (Recommended)
1. **Compile the project**:
   ```bash
   javac -d bin -sourcepath src src/mini/chess/game/GUI/GameUI.java
   ```

2. **Run the application**:
   *Note: Ensure `mysql-connector-j-8.x.x.jar` is in your classpath. If you don't have it, download it from MySQL website.*
   
   **Windows (Command Prompt)**:
   ```bash
   java -cp "bin;path/to/mysql-connector-j-8.x.x.jar" mini.chess.game.GUI.GameUI
   ```
   
   **Linux/Mac**:
   ```bash
   java -cp "bin:path/to/mysql-connector-j-8.x.x.jar" mini.chess.game.GUI.GameUI
   ```

#### Console Mode
```bash
# Compile
javac -d bin -sourcepath src src/mini/chess/game/app/Main.java

# Run
java -cp "bin;path/to/mysql-connector-j-8.x.x.jar" mini.chess.game.app.Main
```

#### For LAN Hosting with Firewall Management (Windows)
Run as Administrator to enable automatic firewall rule creation:
```bash
# Right-click Command Prompt → Run as Administrator
java -cp "bin;path/to/mysql-connector-j-8.x.x.jar" mini.chess.game.GUI.GameUI
```

### Running Unit Tests
To verify the game logic, you can run the included unit test suite:
```bash
# Compile tests
javac -d bin -sourcepath src src/mini/chess/game/tests/TestRunner.java

# Run tests
java -cp bin mini.chess.game.tests.TestRunner
```

---

## 📖 How to Use

### Creating an Admin Account

1. Register a normal user account through the application
2. Manually insert an admin record in the database:
   ```sql
   INSERT INTO admins (user_id) 
   SELECT user_id FROM users WHERE username = 'your_username';
   ```
3. Login to access the admin dashboard

### Playing Locally

1. **Login/Register** to your account
2. Select **"New Local Game"**
3. Choose opponent type:
   - **Human**: Two-player local game
   - **AI**: Play against computer (select difficulty level)
4. Make moves by entering: `fromRow fromCol toRow toCol` (0-indexed)

### LAN Multiplayer

**Host:**
1. Select **"Host LAN Game"**
2. Enter a port number (e.g., 9000)
3. The application will:
   - Display your IPv4 address
   - Automatically create firewall rule (if admin)
   - Wait for client connection
4. Share your IP and port with the other player

**Join:**
1. Select **"Join LAN Game"**
2. Enter host's IP address and port
3. Start playing when connected

### Using the Leaderboard

- Select **"View Leaderboard"** from main menu
- Rankings update automatically after each completed game
- Scores are based on game wins

### Admin Features

**Accessing Dashboard:**
1. Login with an admin account
2. Dashboard appears automatically

**Available Operations:**
- **System Logs**: Monitor user activities
- **Game History**: View all completed and active games
- **User Management**: Edit, lock, or delete user accounts

### Cloud Backup Configuration

**Automatic Trigger:**
- Backup triggers when an admin logs in with internet connection

**First-Time Setup:**
1. When prompted, select your cloud sync folder:
   - Google Drive folder
   - OneDrive folder
   - Any other cloud-synced directory
2. Configuration is saved in `backup_config.properties`

**Manual Folder Change:**
Delete or edit `backup_config.properties` to reconfigure

---

## 🎯 Game Rules

### Objective
Capture the opponent's **Leader** piece to win the game.

### Pieces

**Leader (♔)**
- Can move **1 square** in any direction (horizontal, vertical, or diagonal)
- Most valuable piece - losing it means game over

**Soldier (♙)**
- Can move **1 square** forward or sideways (left/right)
- Cannot move backwards or diagonally

### Special Conditions

- **Checkmate**: Leader is under threat and has no valid escape moves
- **Draw**: No valid moves available (stalemate)

---

## 🔧 Configuration Files

### `db.properties`
Database connection settings. Must be placed in the application root directory.
```properties
db.url=jdbc:mysql://localhost:3306/mini_chess?serverTimezone=UTC&useSSL=false
db.user=root
db.password=
```

### `backup_config.properties`
Stores cloud backup folder path:
```properties
cloud_backup_path=C:\\Users\\user\\OneDrive\\Mini Chess Game Backup
```

### MySQL Connection Pool
Configured in `DBConnection.java` with automatic connection management and transaction support.

---

## 🤝 Contributing

### Team Collaboration
This project was developed collaboratively by:
- **Josephat Sangwa** - Lead Developer
- **Josiane Nikuze** - Database Design
- **Ezechiel Ukwishaka** - Network Implementation
- **Augustin Bizumuremyi** - Game Logic
- **Clementine Nabayo** - UI/UX Design

---

## 📝 License

This project is an academic assignment developed for educational purposes.

---

## 🐛 Known Issues & Future Enhancements

### Current Limitations
- Firewall management only works on Windows
- Cloud backup requires manual folder selection on first use
- LAN games require both players to be on the same network

### Planned Features
- Online multiplayer with cloud hosting
- Tournament mode
- Move animations
- Sound effects
- Game replay functionality
- Mobile app version

---

## 📞 Support

For issues or questions, please contact any team member or create an issue in the project repository.

**Happy Gaming! ♟️**
