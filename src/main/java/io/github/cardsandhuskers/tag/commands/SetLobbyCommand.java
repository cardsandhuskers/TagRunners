package io.github.cardsandhuskers.tag.commands;

import io.github.cardsandhuskers.tag.Tag;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetLobbyCommand implements CommandExecutor {
    private Tag plugin;

    public SetLobbyCommand(Tag plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player p) {
            if(p.isOp()) {
                Location location = p.getLocation();

                plugin.getConfig().set("Lobby", location);
                plugin.saveConfig();
                p.sendMessage("Lobby set at:\nWorld: " + location.getWorld() + "\nX: " + location.getX() + " Y: " + location.getY() + " Z: " + location.getZ());
            }
        } else {
            System.out.println("ERROR: cannot run from console.");
        }



        return true;
    }
}
