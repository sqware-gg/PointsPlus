package dev.pointsplus.listener;

import dev.pointsplus.points.PointsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerAccountListener implements Listener {
    private final PointsService pointsService;

    public PlayerAccountListener(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        pointsService.touch(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pointsService.touch(event.getPlayer());
    }
}
