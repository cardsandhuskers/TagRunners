package io.github.cardsandhuskers.tag.listeners;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.handlers.PlayerDeathHandler;
import io.github.cardsandhuskers.teams.objects.Team;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.HashMap;

import static io.github.cardsandhuskers.tag.Tag.handler;

public class PlayerAttackListener implements Listener {
    private HashMap<Team, Player> currentHunters;
    private ArrayList<Player> alivePlayers;
    Tag plugin;
    PlayerDeathHandler deathHandler;
    public PlayerAttackListener(Tag plugin, HashMap<Team, Player> currentHunters, ArrayList<Player> alivePlayers, PlayerDeathHandler deathHandler) {
        this.currentHunters = currentHunters;
        this.alivePlayers = alivePlayers;
        this.plugin = plugin;
        this.deathHandler = deathHandler;
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent e) {
        e.setCancelled(true);
        if(Tag.gameState != Tag.GameState.ROUND_ACTIVE) {
            return;
        }
        Player attacker;
        Player attacked;

        if(e.getEntity().getType() == EntityType.PLAYER) {
            attacked = (Player) e.getEntity();
            if(e.getDamager().getType() == EntityType.PLAYER) {
                attacker = (Player) e.getDamager();
                if(handler.getPlayerTeam(attacker) == null || handler.getPlayerTeam(attacked) == null) return;
                if(currentHunters.get(handler.getPlayerTeam(attacker)).equals(attacker)) {
                    deathHandler.onPlayerDeath(attacked, attacker);
                }
            }
        }
    }



}
