package dev.pointsplus.points;

import dev.pointsplus.api.PointBalance;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PointStore {
    private final JavaPlugin plugin;
    private final File file;
    private final ConcurrentMap<UUID, PointAccount> accounts = new ConcurrentHashMap<>();

    public PointStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        reload();
    }

    public void reload() {
        accounts.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = players.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                PointAccount account = new PointAccount(uuid);
                account.load(
                        section.getString("name", ""),
                        section.getLong("balance", 0L),
                        section.getLong("created-at", 0L),
                        section.getLong("updated-at", 0L)
                );
                accounts.put(uuid, account);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().fine("Ignoring invalid UUID in players.yml: " + key);
            }
        }
    }

    public PointAccount account(UUID uuid) {
        return accounts.computeIfAbsent(uuid, PointAccount::new);
    }

    public Optional<PointAccount> existing(UUID uuid) {
        return Optional.ofNullable(accounts.get(uuid));
    }

    public Optional<PointAccount> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return accounts.values().stream()
                .filter(account -> account.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public List<PointAccount> accounts() {
        return new ArrayList<>(accounts.values()).stream()
                .sorted(Comparator.comparing(PointAccount::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<PointBalance> snapshots() {
        return accounts().stream()
                .map(PointAccount::snapshot)
                .toList();
    }

    public int size() {
        return accounts.size();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PointAccount account : accounts.values()) {
            String path = "players." + account.uuid();
            yaml.set(path + ".name", account.name());
            yaml.set(path + ".balance", account.balance());
            yaml.set(path + ".created-at", account.createdAtMillis());
            yaml.set(path + ".updated-at", account.updatedAtMillis());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save players.yml: " + e.getMessage());
        }
    }
}
