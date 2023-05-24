package io.github.cardsandhuskers.tag.listeners;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.teams.objects.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.HashMap;

import static io.github.cardsandhuskers.tag.Tag.gameState;
import static io.github.cardsandhuskers.tag.Tag.handler;

public class PlayerMoveListener implements Listener {
    HashMap<Team, Player> currentHunters;
    private HashMap<Player, Integer> hunterRounds;
    private ArrayList<Location> hunterBoxes;
    private int side1Z, side2Z;
    private Tag plugin;
    public PlayerMoveListener(HashMap<Team, Player> currentHunters, HashMap<Player, Integer> hunterRounds, Tag plugin) {
        this.plugin = plugin;
        this.currentHunters = currentHunters;
        this.hunterRounds = hunterRounds;
        hunterBoxes = new ArrayList<>();

        int arenaIndex = 1;
        while(plugin.getConfig().getLocation("ArenaHunterChambers.Arena" + arenaIndex + ".1") != null) {
            Location side1 = plugin.getConfig().getLocation("ArenaHunterChambers.Arena" + arenaIndex + ".1");
            Location side2 = plugin.getConfig().getLocation("ArenaHunterChambers.Arena" + arenaIndex + ".2");
            hunterBoxes.add(side1);
            side1Z = side1.getBlockZ();

            hunterBoxes.add(side2);
            side2Z = side2.getBlockZ();
            arenaIndex++;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if(gameState != Tag.GameState.KIT_SELECTION) return;


        Player p = e.getPlayer();
        Team playerTeam = handler.getPlayerTeam(p);
        if(playerTeam == null) return;
        Location playerLoc = p.getLocation();
        boolean inRoom = false;
        //System.out.println(Math.floor(playerLoc.getX()) + "; " +
        //                    Math.floor(playerLoc.getY()) + "; " +
        //                    Math.floor(playerLoc.getZ()));

        for(Location roomLoc:hunterBoxes) {
            if (Math.floor(playerLoc.getX()) == Math.floor(roomLoc.getX()) &&
                Math.floor(playerLoc.getY()) == Math.floor(roomLoc.getY()) &&
                Math.floor(playerLoc.getZ()) == Math.floor(roomLoc.getZ())) {
                inRoom = true;
            }
        }
        if(!inRoom) return;
        if(handler.getPlayerTeam(p) == null) return;

        if(hunterRounds.containsKey(p) && !currentHunters.containsKey(handler.getPlayerTeam(p))) {
            if(hunterRounds.get(p) > 0) {
                currentHunters.put(playerTeam, p);

                for (Player player : playerTeam.getOnlinePlayers()) {
                    player.sendMessage(playerTeam.color + ChatColor.BOLD + p.getDisplayName() + ChatColor.RESET + " is it!");
                    player.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
                }

                //LOCK IN (PLACE GLASS)
                if(Math.floor(playerLoc.getZ()) == side1Z) {
                    for(int i = 0; i < 4; i++) {
                        Location glassLoc = new Location(playerLoc.getWorld(), playerLoc.getX(), playerLoc.getY() + i, playerLoc.getZ() + 1);
                        glassLoc.getBlock().setType(Material.GLASS_PANE);
                    }
                }
                if(Math.floor(playerLoc.getZ()) == side2Z) {
                    for(int i = 0; i < 4; i++) {
                        Location glassLoc = new Location(playerLoc.getWorld(), playerLoc.getX(), playerLoc.getY() + i, playerLoc.getZ() - 1);
                        glassLoc.getBlock().setType(Material.GLASS_PANE);
                    }
                }



            } else {
                p.sendMessage("You are out of opportunities to be it!");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1,1);

                //THROW OUT (TP)
                if(Math.floor(playerLoc.getZ()) == side1Z) {
                    p.teleport(new Location(p.getWorld(), playerLoc.getX(), playerLoc.getY(), playerLoc.getZ() + 3));
                }
                if(Math.floor(playerLoc.getZ()) == side2Z) {
                    p.teleport(new Location(p.getWorld(), playerLoc.getX(), playerLoc.getY(), playerLoc.getZ() - 3));
                }

            }
        }
    }


}
