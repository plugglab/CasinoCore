# Changelog

All notable changes to `CasinoCore` are documented in this file.

## [1.1.0] - 2026-04-23

Expanded the casino lineup, upgraded the hub GUI, and added richer player stats/placeholders.

### Hub And UX

- Reworked the `Casino Hub` layout into a fuller dashboard with wallet stats, session stats, and a larger game grid.
- Added quick bet controls for `-100`, `-10`, `min`, `max`, `+10`, and `+100`.
- Added custom bet entry through chat prompt flow so players can pick any supported amount without extra dependencies.
- Added menu refresh support and better visual separation between game cards and betting controls.

### Games

- Added `Horse Race` with horse selection, weighted race outcomes, configurable per-horse multipliers, and race animation.
- Added `Lucky Wheel` with weighted wheel segments and configurable multipliers.
- Registered both new games in the main game manager, permissions tree, and hub menu.

### Stats And Placeholders

- Expanded persistent player stats to also track `losses` and `games played`.
- Added PlaceholderAPI placeholders for:
  - `%casino_balance_raw%`
  - `%casino_losses%`
  - `%casino_games_played%`
  - `%casino_daily_ready%`
  - `%casino_next_daily%`
  - `%casino_available_games%`
  - `%casino_economy%`

### Configuration

- Added config sections for `horserace` and `wheel`, including limits, weights, multipliers, cooldowns, and documented house edge values.
- Added cooldown entries and permissions for the new games.

### Verification

- Verified the plugin compiles successfully with `mvn -q -DskipTests package`.

## [1.0.0] - 2026-04-22

Initial public plugin release with the current feature set, UX systems, balancing pass, and stability fixes.

### Core

- Added Paper `1.21.1` plugin bootstrap with Vault and PlaceholderAPI soft dependencies.
- Added centralized plugin managers for config, economy, messages, cooldowns, anti-abuse, player stats, UX, and game registration.
- Added `/casino` command suite for hub access, help, reload, balance view, and daily reward claim.
- Added `/play` command suite for direct game access and per-game subcommands.
- Added permission tree for general use, play access, reload access, admin access, and per-game access.

### Games

- Added `Coin Flip` with offer creation, player join flow, cancellation, expiry handling, refund safety, and disconnect cleanup.
- Added `Dice` with low, medium, and high risk profiles, delayed result reveal, sound feedback, and action bar updates.
- Added `Lottery` with exact, close, and near-match outcomes and configurable payout bands.
- Added `Blackjack` with GUI gameplay, dealer logic, hit/stand flow, blackjack and push handling, and round result summaries.
- Added `Roulette` with GUI betting, single-number and simple bets, animated wheel spin, weighted outcome support, and result resolution.
- Added `Slot Machine` with animated reel stopping, GUI-based play, reward summaries, and active-session protection.

### UX And Polish

- Added shared UX presentation manager for boss bar animations, title effects, sound layering, and cleaner formatted result messages.
- Added win streak tracking and best streak tracking.
- Added daily reward support with `/casino daily` and free-spin-value style crediting.
- Added richer wallet/status output in `/casino balance`, including wins, streak, and reward readiness.
- Added big win broadcast support with configurable threshold and message format.
- Added PlaceholderAPI support for balance, wins, streak, and best streak placeholders.

### Economy And Balancing

- Added centralized Vault-backed withdraw, deposit, balance, and formatting support with per-player synchronization.
- Added configurable min/max bet limits globally and per game.
- Added documented house-edge configuration and per-game payout tables.
- Tuned payouts to keep simulated house edge within the requested `5%` to `15%` range across all implemented games.
- Added standalone simulation harness for `10,000`-play economy verification per game.

### Safety And Abuse Prevention

- Added global and per-game cooldown support.
- Added anti-abuse rate limiting for commands, game starts, click spam, bet window totals, and max bet percent of balance.
- Added session guards for games that must prevent duplicate active play states.
- Added refund handling for critical payout failure and interrupted multiplayer game flows.

### Stability And Bug Fixes

- Fixed slot result handling so final reel outcomes are read from actual generated results instead of placeholder values.
- Fixed `/casino reload` so reload runs safely and no longer tears down game registration incorrectly.
- Fixed `GameManager.reloadGames()` to refresh game enable state from config instead of clearing active registrations.
- Fixed coinflip payout resolution to use configured multiplier instead of a hardcoded payout value.
- Fixed lottery to use configured payout and range values instead of stale hardcoded constants.
- Fixed blackjack push handling so pushes are no longer treated as wins.
- Reduced excessive disk writes in player stat tracking by removing per-event file saves and keeping persistence on shutdown.
- Tightened anti-abuse rollback logic around timestamp queues to avoid unsafe mutation paths.

### Performance

- Added concurrent maps for manager state that is shared across active sessions and player tracking.
- Reduced unnecessary persistence writes in player statistics.
- Kept animation and GUI work on Bukkit scheduler paths appropriate for main-thread game interaction.
- Added lightweight math-only simulator for balancing work without requiring live Bukkit interaction.

### Configuration

- Added configurable sections for:
  - messages and prefix
  - broadcasts
  - economy and daily rewards
  - cooldowns
  - anti-abuse rules
  - per-game limits, chances, payouts, animation settings, and weights

### Developer Notes

- Added `CasinoGameResultEvent` and `CasinoBigWinEvent` API hooks.
- Added modular game registration structure through `GameManager`.
- Added `CasinoEconomySimulator` for repeatable local balancing checks.

### Verified Simulation Snapshot

Results from the current `10,000`-play simulation pass:

- CoinFlip: `6.4630%` house edge
- Dice: `10.8061%` house edge
- Lottery: `9.8730%` house edge
- Blackjack: `10.9860%` house edge
- Roulette: `8.2970%` house edge
- Slots: `12.3250%` house edge
