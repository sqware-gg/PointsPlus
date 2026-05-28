# PointsPlus

**Get the plugin jar, setup help, and updates in the SQWARE Discord: [discord.sqware.gg](https://discord.sqware.gg).**

PointsPlus is a Paper points economy plugin for Minecraft servers. It is a PlayerPoints-style replacement with UUID storage, payments, leaderboards, PlaceholderAPI support, a Java API, and cancellable balance-change events.

Use it when you want a separate points currency that does not replace Vault, Essentials, or your main money economy.

## Features

- Balances stored in `plugins/PointsPlus/players.yml`.
- Player commands for balances, payments, and leaderboards.
- Admin commands for give, giveall, take, set, reset, save, reload, import, and export.
- PlayerPoints-style aliases: `/points`, `/playerpoints`, `/p`, `/pp`, and `/pointsplus`.
- UUID-keyed accounts with last-known names.
- Configurable currency name, limits, payments, and messages.
- Cancellable `PointsChangeEvent`.
- Static `PointsPlusApi` for integrations.
- `%pointsplus_*%` PlaceholderAPI placeholders.
- Optional `%playerpoints_*%` compatibility placeholders.
- Config-safe updates through `config-new.yml`.

## Requirements

- Paper `26.1.2+`
- Java `25+`
- Optional: PlaceholderAPI
- Maven wrapper included

## Player Commands

```text
/points
/points me
/points pay <player> <amount>
/points look <player>
/points lead [page]
```

Amount parsing accepts full numbers and compact suffixes: `100`, `1,000`, `1k`, `1.5m`.

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

When compatibility placeholders are enabled, the same parameters also work under `%playerpoints_*%`.

## Java API

```java
long balance = PointsPlusApi.balanceOrZero(player.getUniqueId());
boolean paid = PointsPlusApi.pay(sender, target, 100);
```

```java
@EventHandler
public void onPointsChange(PointsChangeEvent event) {
    if (event.newBalance() > 1_000_000L) {
        event.setCancelled(true);
    }
}
```

## Build

Use JDK 25:

```powershell
.\mvnw.cmd package
```

The jar is written to `target/PointsPlus-0.1.0.jar`.

## License

PointsPlus is licensed under the Apache License, Version 2.0.
