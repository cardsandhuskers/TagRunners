package io.github.cardsandhuskers.tag.commands;

import io.github.cardsandhuskers.tag.Tag;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetArenaWallCommand implements CommandExecutor {
    private Tag plugin;

    public SetArenaWallCommand(Tag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player p) {
            if (p.isOp()) {
                if (args.length == 2) {
                    try {
                        int arenaIndex = Integer.parseInt(args[0]);
                        int teamSide = Integer.parseInt(args[1]);
                        Location location = p.getLocation();

                        plugin.getConfig().set("ArenaWalls.Arena" + arenaIndex + "." + teamSide, location);
                        plugin.saveConfig();

                        p.sendMessage("Arena" + arenaIndex + " Team " + teamSide + " Arena Wall Location set at:\nWorld: " + location.getWorld() + "\nX: " + location.getX() + " Y: " + location.getY() + " Z: " + location.getZ());

                    } catch (Exception e) {
                        p.sendMessage(ChatColor.RED + "ERROR: Arguments must be integers");
                    }
                } else {
                    p.sendMessage(ChatColor.RED + "Incorrect Usage: /setBattleBoxArenaWall [ArenaNumber] [TeamSideNumber 1 or 2]");
                }
            } else {
                p.sendMessage(ChatColor.RED + "ERROR: You do not have Permission to do this");
            }
        } else {
            System.out.println("ERROR: cannot run from console.");
        }



        return true;
    }
}
