## 🧩 Mini-Chess Game

The Mini-Chess Game project is a simplified, educational chess system designed to demonstrate the integration of game logic, database design, and user interaction. The project is developed by a student team as part of a software development course, focusing on collaboration, planning, and database management.

## 🏗️ Current Progress

- Requirements and system scope defined
- mini.chess.game.db design and ER diagram completed
- GitHub repository prepared for documentation and future implementation
- Console game playable (human vs human, human vs AI)
- Basic AI upgraded to strategic alpha-beta search with difficulty levels
- Simulation runner added for bulk AI-vs-AI testing

## 👥 Team Members

Josephat Sangwa

Josiane Nikuze

Ezechiel Ukwishaka

Augustin Bizumuremyi

Clementine Nabayo

## ⚙️ Technologies (Planned)

Oracle mini.chess.game.db – for data storage and management

Java – for backend game logic

GitHub – for version control and collaboration

## Game Rules:

This project implements basic check, checkmate, and draw detection in addition to leader capture. The game ends when a Leader is captured, checkmate occurs (opponent has no legal moves while in check), or a draw situation is reached (opponent has no legal moves and is not in check).

## Board Setup
- The game is played on a 5x5 grid.
- Each player controls two types of pieces:
  - **Leader (L)**
  - **Soldier (S)**
- Initial positions:
  - **Player1:** Leader at (4,2), Soldiers at (3,1) and (3,3)
  - **Player2:** Leader at (0,2), Soldiers at (1,1) and (1,3)

## Piece Movement

### Leader (L)
- Moves one square in any direction (orthogonal or diagonal).
- Symbol: `L`
- Example: From (4,2), valid moves are (3,1), (3,2), (3,3), (4,1), (4,3), (5,1), (5,2), (5,3) [assuming within board bounds].

### Soldier (S)
- Moves forward one square (no diagonal or sideways movement).
- Symbol: `S`
- Direction:
  - **Player1 Soldiers:** Move upward (from higher row to lower row).
  - **Player2 Soldiers:** Move downward (from lower row to higher row).
- Example: A Player1 Soldier at (3,1) can only move to (2,1).

## Move Rules
- Players alternate turns.
- On your turn, enter move as: `fromRow fromCol toRow toCol` (e.g., `3 1 2 1`).
- You may only move your own pieces.
- A move is only valid if:
  - The destination square is within move rules for that piece.
  - The destination square is empty or contains an opponent’s piece (which will be captured).
  - You cannot move onto a square occupied by your own piece.

## Captures
- If a piece moves to a square occupied by an opponent’s piece, the opponent’s piece is captured and removed from the board.
- Captures are announced in the console.

## Win Condition
- The game ends immediately when a player's **Leader** is captured.
- The player who captures the opponent’s Leader wins.
- If both Leaders remain, the game continues.

## User Interface
- The board is displayed as a grid with `[L]` for Leader, `[S]` for Soldier, and `[ ]` for empty.
- Players enter moves via console input.

## Example Game
Initial Board:
```
[ ] [ ] [L] [ ] [ ]
[ ] [S] [ ] [S] [ ]
[ ] [ ] [ ] [ ] [ ]
[ ] [S] [ ] [S] [ ]
[ ] [ ] [L] [ ] [ ]
```
(Player1 at bottom, Player2 at top)

## Other Notes
- No special moves (castling, pawn promotion, en passant, etc.).
- Simplified rules tailored to 5x5 mini-chess.
- The engine detects Check, Checkmate, and basic Draw (no legal moves) conditions.

## References
- [Board initialization code](https://github.com/Josephat-S/Custom-Mini-Chess-Game/blob/main/src/Models/Board.java)
- [Piece movement rules](https://github.com/Josephat-S/Custom-Mini-Chess-Game/blob/main/src/Models/Leader.java), [Soldier movement](https://github.com/Josephat-S/Custom-Mini-Chess-Game/blob/main/src/Models/Soldier.java)
- [Win condition](https://github.com/Josephat-S/Custom-Mini-Chess-Game/blob/main/src/Models/Board.java#L61-L78)



## 🤖 AI Logic Flow

The AI uses a minimax search with alpha–beta pruning on the 5x5 board.
- Move generation: `Board.getAllLegalMoves(player)` returns only moves that don’t leave your Leader in check.
- Search: Alternates turns down to a fixed depth (configurable). Alpha–beta cuts branches that can’t affect the final decision.
- Evaluation: At leaves (or when no moves), the position is scored by:
  - Material: `Leader = 100`, `Soldier = 3`
  - Mobility: difference in the number of legal moves
  - Checks: small bonus when opponent is in check, penalty if self is in check
  - Terminal leader capture is treated as a huge win/loss
- Tie-breaking: When multiple moves share top score, one is chosen at random for variety.

Key classes/methods:
- `mini.chess.game.Models.AIPlayer`
  - `setSearchDepth(int)` – set difficulty (0 = random, 1..3 = deeper search)
  - `chooseBestMove(Board, String)` / `makeBestMove(Board, String)`
  - `chooseRandomMove(Board, String)` / `makeRandomMove(Board, String)`
- `mini.chess.game.Models.Board`
  - `getAllLegalMoves(String)` – legal moves that keep Leader safe
  - `isLeaderInCheck(String)` – used in evaluation and status logic
  - Internal helpers for search: silent `applyMoveSilently` / `undoMoveSilently`

## 🎮 Playing vs AI (Console)
- Run `mini.chess.game.app.Main`.
- Choose game mode "Play vs Computer (AI)" and pick sides.
- Choose difficulty:
  - 1) Random (no lookahead)
  - 2) Depth 1
  - 3) Depth 2 (default)
  - 4) Depth 3

## 📊 Simulation Runner (Batch AI vs AI)
Run many games automatically to evaluate performance across settings.

Usage:
```
java mini.chess.game.app.SimulationRunner [games] [p1Depth] [p2Depth] [seed]
```
Examples:
```
# 50 games, both AIs at depth 2 (default)
java mini.chess.game.app.SimulationRunner

# 200 games, P1 depth=3 vs P2 depth=1, with fixed seed for reproducibility
java mini.chess.game.app.SimulationRunner 200 3 1 12345
```
The runner prints totals: Player1 wins, Player2 wins, and Draws.
