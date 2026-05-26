# PointsPlus

PointsPlus is a Paper plugin for standalone point-based economies. It is intended as a cleaner PlayerPoints-style replacement with UUID storage, familiar commands, PlaceholderAPI support, a small Java API, and cancellable balance-change events.

## Compatibility

- Server software: Paper
- API target: Paper `26.1.2.build.65-stable`
- Java: `25+`
- Build tool: Maven wrapper
- Optional hooks: PlaceholderAPI

## Features

- Stores balances in `plugins/PointsPlus/players.yml`.
- Player commands for balance checks, payments, and leaderboards.
- Admin commands for give, giveall, take, set, reset, save, and reload.
- Basic YAML import/export through `storage.yml`.
- PlayerPoints-style aliases: `/points`, `/playerpoints`, `/p`, `/pp`, and `/pointsplus`.
- UUID-keyed accounts with last-known names.
- Configurable point names, limits, payments, and messages.
- Cancellable `PointsChangeEvent` for integrations.
- Static `PointsPlusApi` for direct plugin integration.
- Optional `%pointsplus_*%` PlaceholderAPI placeholders.
- Optional `%playerpoints_*%` compatibility placeholders.
- Separate from Vault and Essentials economy by design, so `/eco` and `/balance` stay money-only.
- Config-safe updates through `config-new.yml`.

## Installation

1. Build or download `PointsPlus-0.1.0.jar`.
2. Stop the Paper server.
3. Put the jar in the server `plugins` folder.
4. Start the server once to generate `plugins/PointsPlus/config.yml`.
5. Review account creation, payment limits, PlaceholderAPI, messages, and permissions.
6. Restart the server, or run `/points reload`.

## Player Commands

```text
/points
/points me
/points pay <player> <amount>
/points look <player>
/points lead [page]
```

Amount parsing accepts full numbers and compact suffixes:

```text
100
1,000
1k
1.5m
```

## Admin Commands

```text
/points give <player> <amount>
/points giveall <amount>
/points take <player> <amount>
/points set <player> <amount>
/points reset <player>
/points broadcast <player>
/points export
/points import
/points save
/points reload
```

## Permissions

```text
pointsplus.me        - view own points, default true
pointsplus.pay       - send points, default true
pointsplus.look      - view another player's points, default true
pointsplus.lead      - view leaderboards, default true
pointsplus.reload    - reload config, default op
pointsplus.save      - save data, default op
pointsplus.give      - give points, default op
pointsplus.giveall   - give online players points, default op
pointsplus.take      - take points, default op
pointsplus.set       - set balances, default op
pointsplus.reset     - reset balances, default op
pointsplus.broadcast - broadcast a balance, default op
pointsplus.export    - export storage.yml, default op
pointsplus.import    - import storage.yml, default op
pointsplus.admin     - all admin commands, default op
```

## PlaceholderAPI

If PlaceholderAPI is installed and `placeholders.enabled` is true, these placeholders are registered under `%pointsplus_*%`. If compatibility is enabled, the same params also work under `%playerpoints_*%`.

```text
%pointsplus_points%
%pointsplus_points_formatted%
%pointsplus_points_shorthand%
%pointsplus_points_for_<player>%
%pointsplus_points_formatted_for_<player>%
%pointsplus_points_shorthand_for_<player>%
%pointsplus_leaderboard_<#>%
%pointsplus_leaderboard_<#>_amount%
%pointsplus_leaderboard_<#>_amount_formatted%
%pointsplus_leaderboard_<#>_amount_shorthand%
%pointsplus_leaderboard_position%
%pointsplus_leaderboard_position_formatted%
```

## Java API

```java
long balance = PointsPlusApi.balanceOrZero(player.getUniqueId());
boolean paid = PointsPlusApi.pay(sender, target, 100);
```

Listen for balance changes:

```java
@EventHandler
public void onPointsChange(PointsChangeEvent event) {
    if (event.newBalance() > 1_000_000L) {
        event.setCancelled(true);
    }
}
```

## Data

Player data is stored in:

```text
plugins/PointsPlus/players.yml
```

Each account stores UUID, last known name, balance, creation time, and update time.

`/points export` writes `plugins/PointsPlus/storage.yml`. `/points import` first reads that file, then falls back to `plugins/PlayerPoints/storage.yml` if it exists.

## Build From Source

Use JDK 25:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot'
.\mvnw.cmd package
```

The compiled jar is written to:

```text
target/PointsPlus-0.1.0.jar
```

## Updating

PointsPlus does not overwrite your existing `config.yml`. If the bundled config changes, the plugin writes `plugins/PointsPlus/config-new.yml` so you can compare and copy new options.

Player data is stored separately in `players.yml`.

## License

PointsPlus is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
