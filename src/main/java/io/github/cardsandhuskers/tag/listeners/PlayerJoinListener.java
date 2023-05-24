package io.github.cardsandhuskers.tag.listeners;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.teams.objects.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

import static io.github.cardsandhuskers.tag.Tag.gameState;
import static io.github.cardsandhuskers.tag.Tag.handler;

public class PlayerJoinListener implements Listener {

    private final Tag plugin;

    public PlayerJoinListener(Tag plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        try {p.teleport(Objects.requireNonNull(plugin.getConfig().getLocation("WorldSpawn")));}catch (NullPointerException ex){}

        if(handler.getPlayerTeam(p) != null && gameState == Tag.GameState.GAME_STARTING) {

        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ()->p.setGameMode(GameMode.SPECTATOR),10);
        }


    }

}
