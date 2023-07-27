package io.github.cardsandhuskers.tag.handlers;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.objects.GameMessages;
import io.github.cardsandhuskers.teams.objects.Team;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class ArenaHandler {
    Tag plugin;
    ArrayList<Location> wallLocations;
    public ArenaHandler(Tag plugin) {
        this.plugin = plugin;
        wallLocations = new ArrayList<>();


        //make sure plugin instance is not null (error handling)
        if(plugin != null) {
            //get arenas, used to build the wool
            int counter = 1;
            while (plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 1) != null && plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 2) != null &&
                    plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 3) != null && plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 4) != null) {
                Location Arena1 = plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 1);
                Location Arena2 = plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 2);
                Location Arena3 = plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 3);
                Location Arena4 = plugin.getConfig().getLocation("ArenaWalls.Arena" + counter + "." + 4);
                wallLocations.add(Arena1);
                wallLocations.add(Arena2);
                wallLocations.add(Arena3);
                wallLocations.add(Arena4);
                counter++;
            }
        }
    }

    public void buildWalls() {
        for(Location l:wallLocations) {
            //X values
            for(int x = -1; x <=1; x++) {
                //Y values
                for(int y = 0; y <=3; y++) {
                    Location temp = new Location(l.getWorld(), l.getX() + x, l.getY() + y, l.getZ());
                    Block b = temp.getBlock();
                    b.setType(Material.BLACK_STAINED_GLASS);
                }
            }
        }
    }

    public void deleteWalls() {
        for(Location l:wallLocations) {
            //X values
            for(int x = -1; x <=1; x++) {
                //Y values
                for(int y = 0; y <=3; y++) {
                    Location temp = new Location(l.getWorld(), l.getX() + x, l.getY() + y, l.getZ());
                    Block b = temp.getBlock();
                    b.setType(Material.AIR);
                }
            }
        }
    }

    public void recolorHunterRoom(Location l, Team t) {
        for(int x = -8; x <= 8; x++) {
            for(int y = -1; y <= 8; y++) {
                for(int z = -8; z <= 8; z++) {
                    Location loc = new Location(l.getWorld(), l.getX() + x, l.getY() + y, l.getZ() + z);
                    if(isConcrete(loc.getBlock().getType())) {
                        loc.getBlock().setType(getConcrete(t.color));
                    }
                }
            }
        }
    }

    /**
     * teleport all players to their correct arenas
     * @param kit - teleport to kit rooms or not
     */
    public void teleportPlayers(boolean kit, Team[][] matchups, PlayerDeathHandler deathHandler, HashMap<Team, Player> currentHunters, HashMap<Player, Integer> hunterRounds) {
        String add = "";
        if(kit) add = "Kit";
        int teamCounter = 1;
        int arenaCounter = 1;
        while(plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + teamCounter + ".1") != null) {
            if(matchups.length < teamCounter) {
                break;
            }

            Team teamA = matchups[teamCounter-1][0];
            Team teamB = matchups[teamCounter-1][1];

            if(!kit && teamA.getTeamName().equalsIgnoreCase("DUMMYTEAM")) {
                deathHandler.unopposed = teamB;
            } else if(!kit && teamB.getTeamName().equalsIgnoreCase("DUMMYTEAM")) {
                deathHandler.unopposed = teamA;
            }

            Location aRunners = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 2);
            Location bHunter = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 1);

            arenaCounter++;

            Location bRunners = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 2);
            Location aHunter = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 1);

            for(Player p:teamA.getOnlinePlayers()) {
                if(p.equals(currentHunters.get(teamA)) && !kit) p.teleport(aHunter);
                else p.teleport(aRunners);
                p.setGameMode(GameMode.ADVENTURE);
            }

            for(Player p:teamB.getOnlinePlayers()) {
                if(p.equals(currentHunters.get(teamB)) && !kit) p.teleport(bHunter);
                else p.teleport(bRunners);
                p.setGameMode(GameMode.ADVENTURE);
            }


            if(kit) {
                GameMessages.announceOpponents(teamA, teamB);
                GameMessages.announceOpponents(teamB, teamA);
                recolorHunterRoom(aRunners, teamA);
                recolorHunterRoom(bRunners, teamB);
            }
            else {
                GameMessages.preroundAnnouncements(teamA, teamB, hunterRounds.get(currentHunters.get(teamA))-1, currentHunters);
                GameMessages.preroundAnnouncements(teamB, teamA, hunterRounds.get(currentHunters.get(teamB))-1, currentHunters);
            }

            arenaCounter++;
            teamCounter++;
        }
        if(kit) {

            teamCounter = 1;
            while(plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + teamCounter + ".1") != null) {
                //side 1
                Location side1 = plugin.getConfig().getLocation("ArenaHunterChambers.Arena" + teamCounter + ".1");
                for (int i = 0; i < 4; i++) {
                    Location glassLoc = new Location(side1.getWorld(), side1.getX(), side1.getY() + i, side1.getZ() + 1);
                    glassLoc.getBlock().setType(Material.AIR);
                }
                //side 2
                Location side2 = plugin.getConfig().getLocation("ArenaHunterChambers.Arena" + teamCounter + ".2");
                for (int i = 0; i < 4; i++) {
                    Location glassLoc = new Location(side2.getWorld(), side2.getX(), side2.getY() + i, side2.getZ() - 1);
                    glassLoc.getBlock().setType(Material.AIR);
                }
                teamCounter++;
            }

        }
    }

    private boolean isConcrete(Material mat) {
        return switch (mat) {
            case BLACK_CONCRETE, CYAN_CONCRETE, BLUE_CONCRETE, BROWN_CONCRETE, GRAY_CONCRETE, GREEN_CONCRETE, LIGHT_BLUE_CONCRETE, LIGHT_GRAY_CONCRETE, LIME_CONCRETE, MAGENTA_CONCRETE, ORANGE_CONCRETE, PINK_CONCRETE, PURPLE_CONCRETE, RED_CONCRETE, WHITE_CONCRETE, YELLOW_CONCRETE ->
                    true;
            default -> false;
        };
    }
    private Material getConcrete(String color) {
        switch (color) {
            case "§2": return Material.GREEN_CONCRETE;
            case "§3": return Material.CYAN_CONCRETE;
            case "§5": return Material.PURPLE_CONCRETE;
            case "§6": return Material.ORANGE_CONCRETE;
            case "§7": return Material.LIGHT_GRAY_CONCRETE;
            case "§8": return Material.BLACK_CONCRETE;
            case "§9": return Material.BLUE_CONCRETE;
            case "§a": return Material.LIME_CONCRETE;
            case "§b": return Material.LIGHT_BLUE_CONCRETE;
            case "§c": return Material.RED_CONCRETE;
            case "§d": return Material.PINK_CONCRETE;
            case "§e": return Material.YELLOW_CONCRETE;
            default: return Material.WHITE_CONCRETE;
        }
    }
}
