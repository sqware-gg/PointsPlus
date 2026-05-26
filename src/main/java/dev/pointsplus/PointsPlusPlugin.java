package dev.pointsplus;

import dev.pointsplus.api.PointsPlusApi;
import dev.pointsplus.command.PointsCommand;
import dev.pointsplus.config.ConfigReferenceWriter;
import dev.pointsplus.config.PointsPlusConfig;
import dev.pointsplus.listener.PlayerAccountListener;
import dev.pointsplus.points.PointStore;
import dev.pointsplus.points.PointsService;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PointsPlusPlugin extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 31602;

    private PointsPlusConfig pointsConfig;
    private PointStore pointStore;
    private PointsService pointsService;
    private final List<Object> placeholderExpansions = new ArrayList<>();

    @Override
    public void onEnable() {
        new Metrics(this, BSTATS_PLUGIN_ID);
        ConfigReferenceWriter.saveDefaultAndReferenceIfNeeded(this);

        pointsConfig = new PointsPlusConfig(this);
        pointStore = new PointStore(this);
        pointsService = new PointsService(this, pointsConfig, pointStore);
        PointsPlusApi.register(pointsService);

        getServer().getPluginManager().registerEvents(new PlayerAccountListener(pointsService), this);
        registerCommand();

        pointsService.start();
        registerPlaceholderApiExpansions();
    }

    @Override
    public void onDisable() {
        unregisterPlaceholderApiExpansions();
        PointsPlusApi.unregister();
        if (pointsService != null) {
            pointsService.stop();
        }
    }

    public void reloadPlugin() {
        pointsConfig.reload();
        pointsService.reload(pointsConfig);
        unregisterPlaceholderApiExpansions();
        registerPlaceholderApiExpansions();
    }

    private void registerCommand() {
        PointsCommand pointsCommand = new PointsCommand(this, pointsService);
        PluginCommand points = getCommand("points");
        if (points != null) {
            points.setExecutor(pointsCommand);
            points.setTabCompleter(pointsCommand);
        }
    }

    private void registerPlaceholderApiExpansions() {
        if (!pointsConfig.placeholdersEnabled() || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        registerPlaceholderApiExpansion("pointsplus");
        if (pointsConfig.playerPointsPlaceholdersEnabled()) {
            registerPlaceholderApiExpansion("playerpoints");
        }
    }

    private void registerPlaceholderApiExpansion(String identifier) {
        try {
            Class<?> expansionClass = Class.forName("dev.pointsplus.hook.PlaceholderApiExpansion");
            Object expansion = expansionClass
                    .getConstructor(JavaPlugin.class, PointsService.class, String.class)
                    .newInstance(this, pointsService, identifier);
            expansionClass.getMethod("register").invoke(expansion);
            placeholderExpansions.add(expansion);
            getLogger().info("Registered PlaceholderAPI placeholders for %" + identifier + "_*%.");
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException e) {
            getLogger().warning("Could not register PlaceholderAPI placeholders: " + e.getMessage());
        }
    }

    private void unregisterPlaceholderApiExpansions() {
        for (Object expansion : placeholderExpansions) {
            try {
                expansion.getClass().getMethod("unregister").invoke(expansion);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                getLogger().warning("Could not unregister PlaceholderAPI placeholders: " + e.getMessage());
            }
        }
        placeholderExpansions.clear();
    }

}
