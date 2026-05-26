package dev.pointsplus.command;

import dev.pointsplus.PointsPlusPlugin;
import dev.pointsplus.api.PointBalance;
import dev.pointsplus.points.AccountRef;
import dev.pointsplus.points.PointsService;
import dev.pointsplus.util.AmountParser;
import dev.pointsplus.util.NumberFormatter;
import dev.pointsplus.util.Text;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class PointsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> AMOUNT_SUGGESTIONS = List.of("100", "1k", "10k", "100k", "1m");

    private final PointsPlusPlugin plugin;
    private final PointsService pointsService;

    public PointsCommand(PointsPlusPlugin plugin, PointsService pointsService) {
        this.plugin = plugin;
        this.pointsService = pointsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                showSelf(player);
            } else {
                showStatus(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> message(sender, "usage", Map.of());
            case "me", "balance", "bal" -> {
                if (sender instanceof Player player) {
                    showSelf(player);
                } else {
                    message(sender, "players-only", Map.of());
                }
            }
            case "pay", "send" -> pay(sender, args);
            case "look", "view" -> look(sender, args);
            case "lead", "top", "leaderboard" -> leaderboard(sender, args);
            case "give" -> give(sender, args);
            case "giveall" -> giveAll(sender, args);
            case "take" -> take(sender, args);
            case "set" -> set(sender, args);
            case "reset" -> reset(sender, args);
            case "broadcast" -> broadcast(sender, args);
            case "export" -> exportData(sender);
            case "import" -> importData(sender);
            case "save" -> save(sender);
            case "reload" -> reload(sender);
            case "status" -> showStatus(sender);
            default -> message(sender, "usage", Map.of());
        }
        return true;
    }

    private void showSelf(Player player) {
        if (!has(player, "pointsplus.me")) {
            message(player, "no-permission", Map.of());
            return;
        }
        pointsService.touch(player);
        long balance = pointsService.balanceOrZero(player.getUniqueId());
        message(player, "balance-self", Map.of("balance", format(balance)));
    }

    private void pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            message(sender, "players-only", Map.of());
            return;
        }
        if (!has(player, "pointsplus.pay")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (!pointsService.config().paymentsEnabled()) {
            message(sender, "transaction-failed", Map.of());
            return;
        }
        if (args.length < 3) {
            message(sender, "usage-pay", Map.of());
            return;
        }

        OptionalLong amount = parseAmount(sender, args[2], pointsService.config().minimumPayment(), pointsService.config().maximumPayment());
        if (amount.isEmpty()) {
            return;
        }
        Optional<AccountRef> target = pointsService.resolve(args[1], pointsService.config().createPayTargets());
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        if (!pointsService.config().allowNegativeBalances() && pointsService.balanceOrZero(player.getUniqueId()) < amount.getAsLong()) {
            message(sender, "insufficient-funds", Map.of(
                    "amount", format(amount.getAsLong()),
                    "balance", format(pointsService.balanceOrZero(player.getUniqueId()))
            ));
            return;
        }
        if (!pointsService.pay(player.getUniqueId(), player.getName(), target.get().uuid(), target.get().name(), amount.getAsLong())) {
            message(sender, "transaction-failed", Map.of());
            return;
        }

        long senderBalance = pointsService.balanceOrZero(player.getUniqueId());
        long targetBalance = pointsService.balanceOrZero(target.get().uuid());
        message(sender, "paid-sender", Map.of(
                "amount", format(amount.getAsLong()),
                "target", target.get().name(),
                "balance", format(senderBalance)
        ));
        Player targetPlayer = Bukkit.getPlayer(target.get().uuid());
        if (targetPlayer != null && !targetPlayer.equals(player) && pointsService.config().notifyPaymentTarget()) {
            message(targetPlayer, "paid-target", Map.of(
                    "source", player.getName(),
                    "amount", format(amount.getAsLong()),
                    "balance", format(targetBalance)
            ));
        }
    }

    private void look(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.look")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length < 2) {
            message(sender, "usage-look", Map.of());
            return;
        }
        Optional<PointBalance> balance = pointsService.findByName(args[1]);
        if (balance.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        message(sender, "balance-other", Map.of(
                "player", balance.get().name(),
                "balance", format(balance.get().balance())
        ));
    }

    private void leaderboard(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.lead")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }

        List<PointBalance> top = pointsService.top();
        if (top.isEmpty()) {
            message(sender, "top-empty", Map.of());
            return;
        }

        int pageSize = pointsService.config().leaderboardPageSize();
        int pages = Math.max(1, (int) Math.ceil(top.size() / (double) pageSize));
        page = Math.min(page, pages);
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, top.size());

        message(sender, "top-header", Map.of(
                "page", Integer.toString(page),
                "pages", Integer.toString(pages)
        ));
        for (int index = start; index < end; index++) {
            PointBalance entry = top.get(index);
            message(sender, "top-line", Map.of(
                    "rank", Integer.toString(index + 1),
                    "player", entry.name(),
                    "balance", format(entry.balance())
            ));
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.give")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length < 3) {
            message(sender, "usage-give", Map.of());
            return;
        }
        Optional<AccountRef> target = pointsService.resolve(args[1], pointsService.config().createOnAdminWrite());
        OptionalLong amount = parseAmount(sender, args[2], 1L, pointsService.config().maxBalance());
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        if (amount.isEmpty()) {
            return;
        }
        if (!pointsService.give(target.get().uuid(), target.get().name(), amount.getAsLong(), sender.getName())) {
            message(sender, "transaction-failed", Map.of());
            return;
        }
        message(sender, "gave", Map.of(
                "player", target.get().name(),
                "amount", format(amount.getAsLong()),
                "balance", format(pointsService.balanceOrZero(target.get().uuid()))
        ));
    }

    private void giveAll(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.giveall")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length < 2) {
            message(sender, "usage-giveall", Map.of());
            return;
        }
        OptionalLong amount = parseAmount(sender, args[1], 1L, pointsService.config().maxBalance());
        if (amount.isEmpty()) {
            return;
        }
        int count = pointsService.giveAllOnline(amount.getAsLong(), sender.getName());
        message(sender, "gave-all", Map.of(
                "amount", format(amount.getAsLong()),
                "count", Integer.toString(count)
        ));
    }

    private void take(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.take")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length < 3) {
            message(sender, "usage-take", Map.of());
            return;
        }
        Optional<AccountRef> target = pointsService.resolve(args[1], false);
        OptionalLong amount = parseAmount(sender, args[2], 1L, pointsService.config().maxBalance());
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        if (amount.isEmpty()) {
            return;
        }
        if (!pointsService.take(target.get().uuid(), target.get().name(), amount.getAsLong(), sender.getName())) {
            message(sender, "transaction-failed", Map.of());
            return;
        }
        message(sender, "took", Map.of(
                "player", target.get().name(),
                "amount", format(amount.getAsLong()),
                "balance", format(pointsService.balanceOrZero(target.get().uuid()))
        ));
    }

    private void set(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.set")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length < 3) {
            message(sender, "usage-set", Map.of());
            return;
        }
        Optional<AccountRef> target = pointsService.resolve(args[1], pointsService.config().createOnAdminWrite());
        OptionalLong amount = parseNonNegativeAmount(sender, args[2], pointsService.config().maxBalance());
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        if (amount.isEmpty()) {
            return;
        }
        if (!pointsService.set(target.get().uuid(), target.get().name(), amount.getAsLong(), sender.getName())) {
            message(sender, "transaction-failed", Map.of());
            return;
        }
        message(sender, "set", Map.of(
                "player", target.get().name(),
                "balance", format(amount.getAsLong())
        ));
    }

    private void reset(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.reset")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length < 2) {
            message(sender, "usage-reset", Map.of());
            return;
        }
        Optional<AccountRef> target = pointsService.resolve(args[1], false);
        if (target.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        if (!pointsService.reset(target.get().uuid(), target.get().name(), sender.getName())) {
            message(sender, "transaction-failed", Map.of());
            return;
        }
        message(sender, "reset", Map.of("player", target.get().name()));
    }

    private void broadcast(CommandSender sender, String[] args) {
        if (!has(sender, "pointsplus.broadcast")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length < 2) {
            message(sender, "usage-broadcast", Map.of());
            return;
        }
        Optional<PointBalance> balance = pointsService.findByName(args[1]);
        if (balance.isEmpty()) {
            message(sender, "player-not-found", Map.of());
            return;
        }
        String message = Text.color(pointsService.config().prefix() + Text.render(pointsService.config().message("broadcast"), Map.of(
                "player", balance.get().name(),
                "balance", format(balance.get().balance())
        )));
        Bukkit.broadcastMessage(message);
    }

    private void save(CommandSender sender) {
        if (!has(sender, "pointsplus.save")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        pointsService.save();
        message(sender, "saved", Map.of());
    }

    private void exportData(CommandSender sender) {
        if (!has(sender, "pointsplus.export")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        File file = new File(plugin.getDataFolder(), "storage.yml");
        int count = pointsService.exportStorage(file);
        if (count < 0) {
            message(sender, "transaction-failed", Map.of());
            return;
        }
        message(sender, "exported", Map.of(
                "count", Integer.toString(count),
                "file", file.getName()
        ));
    }

    private void importData(CommandSender sender) {
        if (!has(sender, "pointsplus.import")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        File file = importFile();
        if (!file.exists()) {
            message(sender, "import-file-missing", Map.of("file", file.getName()));
            return;
        }
        int count = pointsService.importStorage(file, sender.getName());
        if (count < 0) {
            message(sender, "transaction-failed", Map.of());
            return;
        }
        message(sender, "imported", Map.of(
                "count", Integer.toString(count),
                "file", file.getName()
        ));
    }

    private void reload(CommandSender sender) {
        if (!has(sender, "pointsplus.reload")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        plugin.reloadPlugin();
        message(sender, "reloaded", Map.of());
    }

    private void showStatus(CommandSender sender) {
        if (!has(sender, "pointsplus.admin")) {
            message(sender, "usage", Map.of());
            return;
        }
        message(sender, "status", Map.of(
                "version", plugin.getPluginMeta().getVersion(),
                "accounts", Integer.toString(pointsService.accountCount()),
                "papi", installedAndEnabled("PlaceholderAPI", pointsService.config().placeholdersEnabled())
        ));
    }

    private File importFile() {
        File own = new File(plugin.getDataFolder(), "storage.yml");
        if (own.exists()) {
            return own;
        }
        File parent = plugin.getDataFolder().getParentFile();
        if (parent == null) {
            return own;
        }
        File playerPoints = new File(new File(parent, "PlayerPoints"), "storage.yml");
        return playerPoints.exists() ? playerPoints : own;
    }

    private OptionalLong parseAmount(CommandSender sender, String raw, long minimum, long maximum) {
        OptionalLong amount = AmountParser.parsePositive(raw);
        if (amount.isEmpty()) {
            message(sender, "invalid-amount", Map.of());
            return OptionalLong.empty();
        }
        if (amount.getAsLong() < minimum) {
            message(sender, "amount-too-low", Map.of("amount", format(minimum)));
            return OptionalLong.empty();
        }
        if (amount.getAsLong() > maximum) {
            message(sender, "amount-too-high", Map.of("amount", format(maximum)));
            return OptionalLong.empty();
        }
        return amount;
    }

    private OptionalLong parseNonNegativeAmount(CommandSender sender, String raw, long maximum) {
        OptionalLong amount = AmountParser.parseNonNegative(raw);
        if (amount.isEmpty()) {
            message(sender, "invalid-amount", Map.of());
            return OptionalLong.empty();
        }
        if (amount.getAsLong() > maximum) {
            message(sender, "amount-too-high", Map.of("amount", format(maximum)));
            return OptionalLong.empty();
        }
        return amount;
    }

    private String installedAndEnabled(String pluginName, boolean enabled) {
        boolean installed = Bukkit.getPluginManager().getPlugin(pluginName) != null;
        if (!installed) {
            return "not installed";
        }
        return enabled ? "enabled" : "disabled";
    }

    private String format(long amount) {
        return pointsService.config().formatBalance(amount);
    }

    private void message(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(Text.color(pointsService.config().prefix()
                + Text.render(pointsService.config().message(key), placeholders)));
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("pointsplus.admin");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            addIf(sender, commands, "me", "pointsplus.me");
            addIf(sender, commands, "pay", "pointsplus.pay");
            addIf(sender, commands, "look", "pointsplus.look");
            addIf(sender, commands, "lead", "pointsplus.lead");
            addIf(sender, commands, "give", "pointsplus.give");
            addIf(sender, commands, "giveall", "pointsplus.giveall");
            addIf(sender, commands, "take", "pointsplus.take");
            addIf(sender, commands, "set", "pointsplus.set");
            addIf(sender, commands, "reset", "pointsplus.reset");
            addIf(sender, commands, "broadcast", "pointsplus.broadcast");
            addIf(sender, commands, "export", "pointsplus.export");
            addIf(sender, commands, "import", "pointsplus.import");
            addIf(sender, commands, "save", "pointsplus.save");
            addIf(sender, commands, "reload", "pointsplus.reload");
            addIf(sender, commands, "status", "pointsplus.admin");
            return filter(commands, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (List.of("pay", "look", "give", "take", "set", "reset", "broadcast").contains(subcommand)) {
                return filter(playerNames(), args[1]);
            }
            if (subcommand.equals("giveall")) {
                return filter(AMOUNT_SUGGESTIONS, args[1]);
            }
            if (List.of("lead", "top", "leaderboard").contains(subcommand)) {
                return filter(List.of("1", "2", "3"), args[1]);
            }
        }
        if (args.length == 3 && List.of("pay", "give", "take", "set").contains(subcommand)) {
            return filter(AMOUNT_SUGGESTIONS, args[2]);
        }
        return List.of();
    }

    private void addIf(CommandSender sender, List<String> commands, String command, String permission) {
        if (has(sender, permission)) {
            commands.add(command);
        }
    }

    private List<String> playerNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        for (PointBalance balance : pointsService.top()) {
            if (balance.name() != null && !balance.name().isBlank()) {
                names.add(balance.name());
            }
        }
        return List.copyOf(names);
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }
}
