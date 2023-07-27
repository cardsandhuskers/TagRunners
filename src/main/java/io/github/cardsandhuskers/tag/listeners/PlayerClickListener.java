package io.github.cardsandhuskers.tag.listeners;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.handlers.GlowHandler;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import static io.github.cardsandhuskers.tag.Tag.GameState.ROUND_ACTIVE;

public class PlayerClickListener implements Listener {

    GlowHandler glowHandler;

    public PlayerClickListener(GlowHandler glowHandler) {
        this.glowHandler = glowHandler;
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if(e.getItem() != null && e.getItem().getType() == Material.ENDER_PEARL) {
            e.setCancelled(true);
            if(Tag.gameState == ROUND_ACTIVE) {
                glowHandler.triggerRunnerVision(e.getPlayer());
            }
        }
    }
}
