-- sql
-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Nov 15, 2025 at 03:50 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
 /*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
 /*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
 /*!40101 SET NAMES utf8mb4 */;

--
-- Database: `mini_chess`
--

-- --------------------------------------------------------

--
-- Table structure for table `admins`
--

CREATE TABLE `admins` (
  `admin_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `games`
--

CREATE TABLE `games` (
  `game_id` int(11) NOT NULL,
  `type` enum('single_mode','lan') NOT NULL DEFAULT 'single_mode',
  `start_time` timestamp NULL DEFAULT NULL,
  `end_time` timestamp NULL DEFAULT NULL,
  `status` enum('ongoing','paused','done') NOT NULL DEFAULT 'ongoing',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `gamestate`
--

CREATE TABLE `gamestate` (
  `id` int(11) NOT NULL,
  `game_id` int(11) NOT NULL,
  `player_turn` tinyint(4) NOT NULL CHECK (`player_turn` in (1,2)),
  `board_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`board_data`)),
  `last_move` varchar(50) DEFAULT NULL,
  `saved_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `moves`
--

CREATE TABLE `moves` (
  `move_id` int(11) NOT NULL,
  `game_id` int(11) NOT NULL,
  `player_id` int(11) NOT NULL,
  `move_number` int(11) DEFAULT NULL,
  `from_cell` varchar(10) DEFAULT NULL,
  `to_cell` varchar(10) DEFAULT NULL,
  `move_time` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `players`
--

CREATE TABLE `players` (
  `player_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `score` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `players`
--

INSERT INTO `players` (`player_id`, `user_id`, `score`) VALUES
(1, 1, 10),
(2, 2, 5);

-- --------------------------------------------------------

--
-- Table structure for table `players_games`
--

CREATE TABLE `players_games` (
  `pg_id` int(11) NOT NULL,
  `game_id` int(11) NOT NULL,
  `player_one_id` int(11) NOT NULL,
  `player_two_id` int(11) DEFAULT NULL,
  `winner_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `player_status`
--

CREATE TABLE `player_status` (
  `player_id` int(11) NOT NULL,
  `wins` int(11) DEFAULT 0,
  `losses` int(11) DEFAULT 0,
  `draws` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `date_registered` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `password_hash`, `date_registered`) VALUES
(1, 'josephat', '6cf0ea55e5fd5e692e007b16339a83f4319370cdb8b6193c1630820119cbba50', '2025-11-08 13:46:50'),
(2, 'sangwa', '6cf0ea55e5fd5e692e007b16339a83f4319370cdb8b6193c1630820119cbba50', '2025-11-08 14:02:11');

-- --------------------------------------------------------

--
-- Table structure for table `user_logs`
--

CREATE TABLE `user_logs` (
  `log_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `log_time` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Views (drop any old stand-ins or views and recreate cleanly)
-- --------------------------------------------------------

DROP VIEW IF EXISTS `leaderboard`;
DROP VIEW IF EXISTS `leaderboards`;
DROP TABLE IF EXISTS `leaderboard`;
DROP TABLE IF EXISTS `leaderboards`;

CREATE OR REPLACE SQL SECURITY INVOKER VIEW `leaderboard` AS
SELECT
  DENSE_RANK() OVER (
    ORDER BY
      p.score DESC,
      COALESCE(ps.wins, 0) DESC,
      COALESCE(ps.losses, 0) ASC,
      COALESCE(ps.draws, 0) DESC,
      u.username ASC,
      p.player_id ASC
  ) AS rank_no,
  u.username AS username,
  p.score AS score,
  COALESCE(ps.wins, 0) AS wins,
  COALESCE(ps.losses, 0) AS losses,
  COALESCE(ps.draws, 0) AS draws,
  p.player_id AS player_id
FROM players p
JOIN users u ON u.user_id = p.user_id
LEFT JOIN player_status ps ON ps.player_id = p.player_id;

CREATE OR REPLACE SQL SECURITY INVOKER VIEW `leaderboards` AS
SELECT
  p.player_id,
  u.username,
  p.score,
  RANK() OVER (
    ORDER BY
      p.score DESC,
      u.username ASC,
      p.player_id ASC
  ) AS rank_no
FROM players p
JOIN users u ON u.user_id = p.user_id;

-- --------------------------------------------------------
-- Indexes for dumped tables
-- --------------------------------------------------------

--
-- Indexes for table `admins`
--
ALTER TABLE `admins`
  ADD PRIMARY KEY (`admin_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `games`
--
ALTER TABLE `games`
  ADD PRIMARY KEY (`game_id`),
  ADD KEY `status` (`status`);

--
-- Indexes for table `gamestate`
--
ALTER TABLE `gamestate`
  ADD PRIMARY KEY (`id`),
  ADD KEY `game_id` (`game_id`);

--
-- Indexes for table `moves`
--
ALTER TABLE `moves`
  ADD PRIMARY KEY (`move_id`),
  ADD KEY `game_id` (`game_id`),
  ADD KEY `player_id` (`player_id`);

--
-- Indexes for table `players`
--
ALTER TABLE `players`
  ADD PRIMARY KEY (`player_id`),
  ADD KEY `idx_players_user` (`user_id`),
  ADD KEY `idx_players_score` (`score`);

--
-- Indexes for table `players_games`
--
ALTER TABLE `players_games`
  ADD PRIMARY KEY (`pg_id`),
  ADD KEY `player_one_id` (`player_one_id`),
  ADD KEY `player_two_id` (`player_two_id`),
  ADD KEY `winner_id` (`winner_id`),
  ADD KEY `game_id` (`game_id`);

--
-- Indexes for table `player_status`
--
ALTER TABLE `player_status`
  ADD PRIMARY KEY (`player_id`),
  ADD KEY `idx_player_status_wld` (`wins`, `losses`, `draws`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- Indexes for table `user_logs`
--
ALTER TABLE `user_logs`
  ADD PRIMARY KEY (`log_id`),
  ADD KEY `user_id` (`user_id`);

-- --------------------------------------------------------
-- AUTO_INCREMENT for dumped tables
-- --------------------------------------------------------

ALTER TABLE `admins`
  MODIFY `admin_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `games`
  MODIFY `game_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `gamestate`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `moves`
  MODIFY `move_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `players`
  MODIFY `player_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `players_games`
  MODIFY `pg_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `user_logs`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT;

-- --------------------------------------------------------
-- Constraints for dumped tables
-- --------------------------------------------------------

ALTER TABLE `admins`
  ADD CONSTRAINT `admins_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

ALTER TABLE `gamestate`
  ADD CONSTRAINT `gamestate_ibfk_1` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`) ON DELETE CASCADE;

ALTER TABLE `moves`
  ADD CONSTRAINT `moves_ibfk_1` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `moves_ibfk_2` FOREIGN KEY (`player_id`) REFERENCES `players` (`player_id`) ON DELETE CASCADE;

ALTER TABLE `players`
  ADD CONSTRAINT `players_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `players_games`
  ADD CONSTRAINT `players_games_ibfk_1` FOREIGN KEY (`game_id`) REFERENCES `games` (`game_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `players_games_ibfk_2` FOREIGN KEY (`player_one_id`) REFERENCES `players` (`player_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `players_games_ibfk_3` FOREIGN KEY (`player_two_id`) REFERENCES `players` (`player_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `players_games_ibfk_4` FOREIGN KEY (`winner_id`) REFERENCES `players` (`player_id`) ON DELETE SET NULL;

ALTER TABLE `player_status`
  ADD CONSTRAINT `player_status_ibfk_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`player_id`) ON DELETE CASCADE;

ALTER TABLE `user_logs`
  ADD CONSTRAINT `user_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
 /*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
 /*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
