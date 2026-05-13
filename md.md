# Lottery Analysis

I checked the plugin's actual lottery logic. The real behavior is in [LotteryGame.java](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\java\com\casinocore\games\impl\LotteryGame.java:54) and [LotteryNumberPrompt.java](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\java\com\casinocore\games\impl\LotteryNumberPrompt.java:27), not the `chances` block in config.

## How the lottery works

- You choose a number from `1-100` ([LotteryNumberPrompt.java](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\java\com\casinocore\games\impl\LotteryNumberPrompt.java:27), [LotteryNumberPrompt.java](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\java\com\casinocore\games\impl\LotteryNumberPrompt.java:62))
- The plugin draws a random winning number from `1-100` ([LotteryGame.java](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\java\com\casinocore\games\impl\LotteryGame.java:54))
- Payouts are:
  - exact match: `16x`
  - within `5`: `2.5x`
  - within `10`: `1.6x`
  ([config.yml](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\resources\config.yml:126), [config.yml](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\resources\config.yml:130))

## Payout for a 500 bet

- Exact match: `500 x 16 = 8000`
- Close match (`1-5` away): `500 x 2.5 = 1250`
- Near match (`6-10` away): `500 x 1.6 = 800`
- Otherwise: `0`

## Net profit after paying the 500 bet

- Exact match: `+7500`
- Close match: `+750`
- Near match: `+300`
- Loss: `-500`

## Win chances

Your chance depends on which number you pick.

### Best picks: middle numbers, roughly `11-90`

- exact: `1%`
- close: `10%`
- near: `10%`
- any win: `21%`

### Worst picks: edge numbers like `1` or `100`

- exact: `1%`
- close: `5%`
- near: `5%`
- any win: `11%`

### Average across all possible picks

- exact: `1%`
- close: about `9.7%`
- near: about `9.2%`
- any payout: about `19.9%`

## Important discrepancy

The config claims `roll-range: 1-250` and lottery chances `0.5% / 5% / 10%` ([config.yml](C:\Users\Gabri\Desktop\plugin\CasinoCore\src\main\resources\config.yml:119)), but the code ignores that and actually uses `1-100`. So the plugin's real odds are better than the config text suggests, but still unfavorable overall.

## Expected return on a 500 bet

- middle pick: about `285` back on average
- average over all picks: about `274.85` back on average

So in practice, a `500` lottery bet in this plugin is usually a losing bet long term.
