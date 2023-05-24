package io.github.cardsandhuskers.tag.listeners;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.handlers.PlayerDeathHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import static io.github.cardsandhuskers.tag.Tag.gameState;
import static io.github.cardsandhuskers.tag.Tag.handler;

public class PlayerLeaveListener implements Listener {
    Tag plugin;
    PlayerDeathHandler deathHandler;

    public PlayerLeaveListener(Tag plugin, PlayerDeathHandler deathHandler) {
        this.plugin = plugin;
        this.deathHandler = deathHandler;
    }
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if(handler.getPlayerTeam(p) == null) return;
        if(gameState == Tag.GameState.ROUND_ACTIVE) {
            deathHandler.onPlayerDeath(p, null);
        } else if (gameState == Tag.GameState.ROUND_STARTING) {
            deathHandler.removePlayer(p);
        }
    }
}
