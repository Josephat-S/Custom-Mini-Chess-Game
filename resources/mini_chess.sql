-- sql
CREATE DATABASE IF NOT EXISTS mini_chess CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;
USE mini_chess;

-- users: use password_hash (bcrypt/argon2) — hashing handled in app
CREATE TABLE IF NOT EXISTS users (
  user_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  date_registered TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- players
CREATE TABLE IF NOT EXISTS players (
  player_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  score INT DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- games
CREATE TABLE IF NOT EXISTS games (
  game_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  type ENUM('single_mode','lan') NOT NULL DEFAULT 'single_mode',
  start_time TIMESTAMP NULL DEFAULT NULL,
  end_time TIMESTAMP NULL DEFAULT NULL,
  status ENUM('ongoing','paused','done') NOT NULL DEFAULT 'ongoing',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- players_games (associates players with games)
CREATE TABLE IF NOT EXISTS players_games (
  pg_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  game_id INT NOT NULL,
  player_one_id INT NOT NULL,
  player_two_id INT,
  winner_id INT NULL,
  FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE,
  FOREIGN KEY (player_one_id) REFERENCES players(player_id) ON DELETE CASCADE,
  FOREIGN KEY (player_two_id) REFERENCES players(player_id) ON DELETE SET NULL,
  FOREIGN KEY (winner_id) REFERENCES players(player_id) ON DELETE SET NULL,
  INDEX (game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- gamestate: store full board as JSON for easy save/load; if your MariaDB/MySQL version lacks JSON,
-- change column to TEXT and store serialized string.
CREATE TABLE IF NOT EXISTS gamestate (
  id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  game_id INT NOT NULL,
  player_turn TINYINT NOT NULL CHECK (player_turn IN (1,2)),
  board_data JSON NOT NULL, -- fallback: change to TEXT if JSON not supported
  last_move VARCHAR(50),
  saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE,
  INDEX (game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- moves (optional detailed move log)
CREATE TABLE IF NOT EXISTS moves (
  move_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  game_id INT NOT NULL,
  player_id INT NOT NULL,
  move_number INT DEFAULT NULL,
  from_cell VARCHAR(10) DEFAULT NULL,
  to_cell VARCHAR(10) DEFAULT NULL,
  move_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE,
  FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE,
  INDEX (game_id),
  INDEX (player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- leaderboards and player_status kept but normalized
CREATE TABLE IF NOT EXISTS leaderboards (
  player_id INT NOT NULL PRIMARY KEY,
  rank_no INT DEFAULT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS player_status (
  player_id INT NOT NULL PRIMARY KEY,
  wins INT DEFAULT 0,
  losses INT DEFAULT 0,
  FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- admin and user_logs normalized
CREATE TABLE IF NOT EXISTS admins (
  admin_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_logs (
  log_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  log_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- removed `saved_pieces` table (redundant if board_data contains full state).
-- If you need per-piece relational queries, recreate with snake_case and proper FKs.

-- Useful indexes (additional)
CREATE INDEX IF NOT EXISTS idx_games_status ON games(status);
CREATE INDEX IF NOT EXISTS idx_gamestate_game ON gamestate(game_id);
CREATE INDEX IF NOT EXISTS idx_players_user ON players(user_id);
