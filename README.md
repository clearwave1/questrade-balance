# questrade-balance

A small Quarkus backend that pulls your real Questrade positions/balances and recomputes
portfolio equity and %P&L **excluding options market value**, since Questrade Edge's own
totals fold options mark-to-market into your equity and % P&L even when the option itself
will never affect your cash (e.g. covered calls you intend to let expire, or spreads you're
not planning to close early).

## How the correction works

For each account:
1. Pull `/v1/positions` and `/v1/symbols` to tag each position as `Option` or not.
2. `correctedMarketValue` = sum of `currentMarketValue` for **non**-option positions only.
3. `correctedEquity` = cash + `correctedMarketValue`.
4. `correctedOpenPnl` = sum of `openPnl` for non-option positions.
5. `correctedPercentPnl` = `correctedOpenPnl / correctedCostBasis * 100`, where cost basis is
   also summed only over non-option positions - this is the part that fixes the % distortion,
   since Edge's % includes option premium/cost in the denominator too.

Raw Questrade numbers (`rawMarketValue`, `rawTotalEquity`) are included in the response
alongside the corrected ones so you can see the delta.

## One-time setup: getting a refresh token per person

Questrade's API auth is per-login, and authorized-trader access to someone else's account
only shows up when you authenticate *as that person*. So you (and your wife, separately)
each need to generate a manual refresh token:

1. Log into `https://login.questrade.com` as that person.
2. Go to **API Access** (App Hub) in account settings.
3. Generate a new manual token. It's a long opaque string, **valid for one use, once,
   within a short window** (currently ~7 minutes) if you don't use it - so generate it right
   before registering the connection below, don't save it for later.

## Running locally

```bash
# Postgres
docker run --name questrade-db -e POSTGRES_USER=questrade -e POSTGRES_PASSWORD=questrade \
  -e POSTGRES_DB=questrade_balance -p 5432:5432 -d postgres:16

# Dev mode
mvn quarkus:dev
```

## Registering a connection

```bash
curl -X POST http://localhost:8080/connections \
  -H "Content-Type: application/json" \
  -d '{"label": "don", "refreshToken": "PASTE_THE_APP_HUB_TOKEN_HERE"}'

curl -X POST http://localhost:8080/connections \
  -H "Content-Type: application/json" \
  -d '{"label": "wife", "refreshToken": "PASTE_HER_APP_HUB_TOKEN_HERE"}'
```

This immediately exchanges the token (confirming it works, capturing `api_server`) and
stores the rotated refresh token in Postgres. From then on the app refreshes it
automatically as needed - you never touch it again unless the connection breaks.

## Getting the corrected portfolio

```bash
curl http://localhost:8080/portfolio | jq
```

Returns per-connection, per-account breakdowns plus grand totals across every account you
have access to.

## The one gotcha to know about

Questrade refresh tokens are **single-use and rotate on every exchange**. This app persists
the new one to Postgres immediately after every refresh, inside the same transaction as the
exchange. If the app crashes or the DB write fails in that narrow window (extremely rare,
but possible), the connection is dead and you'll need to generate a fresh manual token from
App Hub and re-`POST /connections` (delete the old one first, or use a different label).
There's no way around this - it's how Questrade's OAuth flow is designed, not a limitation
of this app.

## What's not built yet (natural next steps)

- Scheduling (`@Scheduled`) to poll and cache portfolio snapshots instead of hitting
  Questrade live on every request - useful if you want historical tracking.
- A minimal Angular/HTML front end - right now this is a JSON API only.
- Rate-limit handling (Questrade allows ~20,000 requests/hour per account, well above what
  this app needs, but worth knowing if you add scheduling with a short interval).
