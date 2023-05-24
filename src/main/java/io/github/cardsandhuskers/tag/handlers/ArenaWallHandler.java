package io.github.cardsandhuskers.tag.handlers;

import io.github.cardsandhuskers.tag.Tag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class ArenaWallHandler {
    Tag plugin;
    ArrayList<Location> wallLocations;
    public ArenaWallHandler(Tag plugin) {
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
}
