package dev.pointsplus.points;

import dev.pointsplus.api.PointBalance;
import dev.pointsplus.api.PointTransactionType;
import dev.pointsplus.config.PointsPlusConfig;
import dev.pointsplus.event.PointsChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PointsService {
    private final JavaPlugin plugin;
    private final PointStore store;
    private PointsPlusConfig config;
    private BukkitTask saveTask;

    public PointsService(JavaPlugin plugin, PointsPlusConfig config, PointStore store) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
    }

    public synchronized void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            touch(player);
        }
        scheduleSaveTask();
    }

    public synchronized void stop() {
        cancelSaveTask();
        store.save();
    }

    public synchronized void reload(PointsPlusConfig config) {
        this.config = config;
        cancelSaveTask();
        scheduleSaveTask();
    }

    public synchronized void touch(Player player) {
        if (player == null) {
            return;
        }
        PointAccount account = store.account(player.getUniqueId());
        account.name(player.getName());
        account.touch(now());
    }

    public synchronized OptionalLong balance(UUID uuid) {
        return store.existing(uuid).map(account -> OptionalLong.of(account.balance())).orElseGet(OptionalLong::empty);
    }

    public synchronized long balanceOrZero(UUID uuid) {
        return balance(uuid).orElse(0L);
    }

    public synchronized Optional<PointBalance> snapshot(UUID uuid) {
        return store.existing(uuid).map(PointAccount::snapshot);
    }

    public synchronized Optional<PointBalance> findByName(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            touch(online);
            return Optional.of(store.account(online.getUniqueId()).snapshot());
        }
        return store.findByName(name).map(PointAccount::snapshot);
    }

    public synchronized Optional<AccountRef> resolve(String input, boolean create) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            touch(online);
            return Optional.of(new AccountRef(online.getUniqueId(), online.getName()));
        }

        Optional<PointAccount> stored = store.findByName(input);
        if (stored.isPresent()) {
            PointAccount account = stored.get();
            return Optional.of(new AccountRef(account.uuid(), account.name()));
        }

        if (!create) {
            return Optional.empty();
        }

        OfflinePlayer offline = offlinePlayer(input);
        UUID uuid = offline.getUniqueId();
        String name = offline.getName() == null || offline.getName().isBlank() ? input : offline.getName();
        PointAccount account = store.account(uuid);
        account.name(name);
        account.touch(now());
        return Optional.of(new AccountRef(uuid, account.name()));
    }

    public synchronized List<PointBalance> top() {
        return store.snapshots().stream()
                .sorted(Comparator.comparingLong(PointBalance::balance).reversed()
                        .thenComparing(PointBalance::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized List<PointBalance> top(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return top().stream().limit(limit).toList();
    }

    public synchronized int rank(UUID uuid) {
        List<PointBalance> ranking = top();
        for (int index = 0; index < ranking.size(); index++) {
            if (ranking.get(index).uuid().equals(uuid)) {
                return index + 1;
            }
        }
        return -1;
    }

    public synchronized boolean give(UUID uuid, String name, long amount, String actor) {
        return add(uuid, name, amount, PointTransactionType.GIVE, actor, true);
    }

    public synchronized boolean take(UUID uuid, String name, long amount, String actor) {
        return add(uuid, name, negate(amount), PointTransactionType.TAKE, actor, true);
    }

    public synchronized boolean set(UUID uuid, String name, long balance, String actor) {
        return setBalance(uuid, name, balance, PointTransactionType.SET, actor, true);
    }

    public synchronized boolean reset(UUID uuid, String name, String actor) {
        return setBalance(uuid, name, 0L, PointTransactionType.RESET, actor, true);
    }

    public synchronized boolean pay(UUID sourceId, String sourceName, UUID targetId, String targetName, long amount) {
        if (amount <= 0L || sourceId.equals(targetId)) {
            return false;
        }

        PointAccount source = store.account(sourceId);
        source.name(sourceName);
        source.touch(now());
        PointAccount target = store.account(targetId);
        target.name(targetName);
        target.touch(now());

        long oldSource = source.balance();
        long oldTarget = target.balance();
        OptionalLong newSource = safeAdd(oldSource, negate(amount));
        OptionalLong newTarget = safeAdd(oldTarget, amount);
        if (newSource.isEmpty() || newTarget.isEmpty()
                || !canSetBalance(newSource.getAsLong()) || !canSetBalance(newTarget.getAsLong())) {
            return false;
        }

        if (!callChangeEvent(source, oldSource, newSource.getAsLong(), PointTransactionType.PAY_SENT, sourceName)) {
            return false;
        }
        if (!callChangeEvent(target, oldTarget, newTarget.getAsLong(), PointTransactionType.PAY_RECEIVED, sourceName)) {
            return false;
        }

        source.balance(newSource.getAsLong());
        target.balance(newTarget.getAsLong());
        long now = now();
        source.touch(now);
        target.touch(now);
        saveIfNeeded();
        return true;
    }

    public synchronized int giveAllOnline(long amount, String actor) {
        if (amount <= 0L) {
            return 0;
        }

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (add(player.getUniqueId(), player.getName(), amount, PointTransactionType.GIVE_ALL, actor, false)) {
                count++;
            }
        }
        if (count > 0) {
            saveIfNeeded();
        }
        return count;
    }

    public synchronized void save() {
        store.save();
    }

    public synchronized int exportStorage(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<PointBalance> balances = store.snapshots();
        for (PointBalance balance : balances) {
            String path = "players." + balance.uuid();
            yaml.set(path + ".name", balance.name());
            yaml.set(path + ".balance", balance.balance());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
            return balances.size();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not export storage.yml: " + e.getMessage());
            return -1;
        }
    }

    public synchronized int importStorage(File file, String actor) {
        if (file == null || !file.exists()) {
            return -1;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int imported = 0;
        imported += importSection(yaml.getConfigurationSection("players"), actor);
        imported += importSection(yaml.getConfigurationSection("data"), actor);
        imported += importSection(yaml.getConfigurationSection("balances"), actor);
        if (imported == 0) {
            imported = importSection(yaml, actor);
        }
        if (imported > 0) {
            store.save();
        }
        return imported;
    }

    public int accountCount() {
        return store.size();
    }

    public PointsPlusConfig config() {
        return config;
    }

    private boolean add(UUID uuid, String name, long delta, PointTransactionType type, String actor, boolean save) {
        if (uuid == null || delta == 0L) {
            return false;
        }
        PointAccount account = store.account(uuid);
        account.name(name);
        long oldBalance = account.balance();
        OptionalLong newBalance = safeAdd(oldBalance, delta);
        if (newBalance.isEmpty() || !canSetBalance(newBalance.getAsLong())) {
            return false;
        }
        if (!callChangeEvent(account, oldBalance, newBalance.getAsLong(), type, actor)) {
            return false;
        }
        account.balance(newBalance.getAsLong());
        account.touch(now());
        if (save) {
            saveIfNeeded();
        }
        return true;
    }

    private boolean setBalance(UUID uuid, String name, long balance, PointTransactionType type, String actor, boolean save) {
        if (uuid == null || !canSetBalance(balance)) {
            return false;
        }
        PointAccount account = store.account(uuid);
        account.name(name);
        long oldBalance = account.balance();
        if (!callChangeEvent(account, oldBalance, balance, type, actor)) {
            return false;
        }
        account.balance(balance);
        account.touch(now());
        if (save) {
            saveIfNeeded();
        }
        return true;
    }

    private int importSection(ConfigurationSection section, String actor) {
        if (section == null) {
            return 0;
        }

        int imported = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            UUID uuid = uuidFrom(key);
            String name = key;
            Long balance = null;

            if (child != null) {
                if (uuid == null) {
                    uuid = uuidFrom(child.getString("uuid", ""));
                }
                name = child.getString("name", key);
                balance = firstLong(child, "balance", "points", "amount");
            } else if (section.isLong(key) || section.isInt(key)) {
                balance = section.getLong(key);
            }

            if (uuid == null || balance == null || !canSetBalance(balance)) {
                continue;
            }
            if (setBalance(uuid, name, balance, PointTransactionType.IMPORT, actor, false)) {
                imported++;
            }
        }
        return imported;
    }

    private Long firstLong(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (section.isLong(key) || section.isInt(key)) {
                return section.getLong(key);
            }
        }
        return null;
    }

    private UUID uuidFrom(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean canSetBalance(long balance) {
        return (config.allowNegativeBalances() || balance >= 0L) && balance <= config.maxBalance();
    }

    private boolean callChangeEvent(PointAccount account, long oldBalance, long newBalance,
                                    PointTransactionType type, String actor) {
        PointsChangeEvent event = new PointsChangeEvent(
                account.uuid(),
                account.name(),
                oldBalance,
                newBalance,
                newBalance - oldBalance,
                type,
                actor == null ? "" : actor
        );
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private OptionalLong safeAdd(long left, long right) {
        try {
            return OptionalLong.of(Math.addExact(left, right));
        } catch (ArithmeticException ignored) {
            return OptionalLong.empty();
        }
    }

    private long negate(long amount) {
        if (amount == Long.MIN_VALUE) {
            return Long.MAX_VALUE;
        }
        return -amount;
    }

    private void scheduleSaveTask() {
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, config.saveIntervalTicks(), config.saveIntervalTicks());
    }

    private void cancelSaveTask() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    private void saveIfNeeded() {
        if (config.saveOnMutation()) {
            store.save();
        }
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer offlinePlayer(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
