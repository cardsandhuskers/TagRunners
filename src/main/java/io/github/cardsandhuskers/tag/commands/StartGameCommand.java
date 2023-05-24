package io.github.cardsandhuskers.tag.commands;

import io.github.cardsandhuskers.tag.handlers.GameStageHandler;
import io.github.cardsandhuskers.tag.listeners.*;
import io.github.cardsandhuskers.tag.objects.Countdown;
import io.github.cardsandhuskers.teams.objects.Team;
import io.github.cardsandhuskers.tag.Tag;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import static io.github.cardsandhuskers.tag.Tag.*;
import static org.bukkit.Bukkit.getServer;

public class StartGameCommand implements CommandExecutor {
    Tag plugin;
    Countdown pregameCountdown;
    HashMap<Team, Player> currentHunters;
    public HashMap<Player, Integer> hunterRounds;
    public ArrayList<Player> aliveRunners;
    private GameStageHandler gameStageHandler;
    private final String GAME_DESCRIPTION, POINTS_DESCRIPTION;
    public StartGameCommand(Tag plugin) {
        this.plugin = plugin;
        GAME_DESCRIPTION =
                ChatColor.STRIKETHROUGH + "----------------------------------------\n" + ChatColor.RESET +
                StringUtils.center(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Tag Runners", 30) +
                ChatColor.BLUE + "" + ChatColor.BOLD + "\nHow To Play:" +
                ChatColor.RESET + "\nYou will be placed in an arena against the other teams in a round robin format." +
                "\nYou must choose a player to be the hunter, while the rest will be runners." +
                "\nTo hunt, walk into the jar. Each player has a limited amount of hunting opportunities, so choose wisely." +
                "\nYour hunter must tag all the opposing runners before your opposing hunter tags all of you to get points" +
                "\n----------------------------------------";
        POINTS_DESCRIPTION = ChatColor.STRIKETHROUGH + "----------------------------------------" +
                ChatColor.GOLD + "" + ChatColor.BOLD + "\nHow is the game Scored:" +
                ChatColor.RESET + "\nFor winning: " + ChatColor.GOLD + (int)(plugin.getConfig().getInt("hunterTagFirstPoints") * multiplier) + ChatColor.RESET + " points per team (" +
                ChatColor.GOLD + (int)(plugin.getConfig().getInt("hunterTagFirstPoints") * multiplier/TEAM_SIZE) + ChatColor.RESET + " points per player" +
                "\nFor tagging a player, the hunter gets: " + ChatColor.GOLD + (int)(plugin.getConfig().getInt("tagPoints") * multiplier) + ChatColor.RESET + " points" +
                ChatColor.GOLD + " -" + (int)(plugin.getConfig().getInt("tagPointsDropoff") * multiplier) + ChatColor.RESET + " points for every 10 seconds that passes" +
                "\nFor tagging all players, the hunter gets: " + ChatColor.GOLD + (int)(plugin.getConfig().getInt("allTagPoints") * multiplier) + ChatColor.RESET + " points" +
                ChatColor.GOLD + " -" + (int)(plugin.getConfig().getInt("allTagPointsDropoff") * multiplier) + ChatColor.RESET + " points for every 10 seconds that passes" +
                "\nFor every 10 seconds you survive as a runner: " + ChatColor.GOLD + (int)(plugin.getConfig().getInt("survivalPoints") * multiplier) + ChatColor.RESET + " points" +
                "\nFor surviving to the end of the round as a runner: " + ChatColor.GOLD + (int)(plugin.getConfig().getInt("fullSurvivalPoints") * multiplier) + ChatColor.RESET + " points" +
                ChatColor.STRIKETHROUGH + "\n----------------------------------------";


    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Tag.round = 1;
        if(sender instanceof  Player player) {
            //Check if player is operator to prevent non ops from running it
            if(player.isOp()) {
                //make sure there are teams
                if (handler.getNumTeams() == 0) {
                    player.sendMessage(ChatColor.RED + "ERROR: There are no Teams");
                } else {

                    startGame();
                    try {
                        if(args.length > 0) {
                            multiplier = Float.parseFloat(args[0]);
                        }
                    } catch(Exception e) {
                        player.sendMessage(ChatColor.RED + "ERROR: MULTIPLIER MUST BE A FLOAT");
                    }

                }
            } else {
                player.sendMessage(ChatColor.RED + "You do not have Permission to do this");
            }
        //make sure there are teams
        } else {
            if(handler.getNumTeams() == 0) {
                for(Player p:Bukkit.getOnlinePlayers()) {
                    if(p.isOp()) {
                        p.sendMessage(ChatColor.RED + "ERROR: No Teams");
                    }
                }
            } else {
                startGame();

                try {
                    if(args.length > 0) {
                        multiplier = Float.parseFloat(args[0]);
                    }
                } catch(Exception e) {
                    System.out.println(ChatColor.RED + "ERROR: MULTIPLIER MUST BE A FLOAT");
                }
            }
        }

        return false;
    }

    /**
     * Starts the countdown before the first round starts
     */
    public void startGame() {

        Tag.gameState = Tag.GameState.GAME_STARTING;
        Location location = plugin.getConfig().getLocation("WorldSpawn");
        for(Player p: Bukkit.getOnlinePlayers()) {
            p.teleport(location);
        }

        currentHunters = new HashMap<>();
        hunterRounds = new HashMap<>();
        Tag.tags = new HashMap<>();
        Tag.roundWins = new HashMap<>();
        gameStageHandler = new GameStageHandler(plugin, currentHunters, hunterRounds);



        getServer().getPluginManager().registerEvents(new PlayerDamageListener(), plugin);
        //getServer().getPluginManager().registerEvents(new PlayerJoinListener(plugin), plugin);
        //getServer().getPluginManager().registerEvents(new PlayerLeaveListener(plugin), plugin);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(currentHunters, hunterRounds, plugin), plugin);
        //getServer().getPluginManager().registerEvents(new PlayerMoveListener(), plugin);

        //Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Battlebox is Starting Soon. Get Ready!");
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ()-> {
            for(Player p:Bukkit.getOnlinePlayers()) {
                if(handler.getPlayerTeam(p) != null) {
                    p.setGameMode(GameMode.ADVENTURE);
                } else {
                    p.setGameMode(GameMode.SPECTATOR);
                }
            }
        }, 5L);
        Location spawn = plugin.getConfig().getLocation("WorldSpawn");
        spawn.getWorld().setSpawnLocation(spawn);



        for(Team t: handler.getTeams()) {
            t.resetTempPoints();
        }


        int totalSeconds = plugin.getConfig().getInt("PregameTime");
        pregameCountdown = new Countdown((JavaPlugin)plugin,
                //should be 80
                totalSeconds,
                //Timer Start
                () -> {

                },

                //Timer End
                () -> {
                    gameStageHandler.startGame();
                },

                //Each Second
                (t) -> {
                    if(t.getSecondsLeft() == totalSeconds - 1) Bukkit.broadcastMessage(GAME_DESCRIPTION);
                    if(t.getSecondsLeft() == totalSeconds - 11) Bukkit.broadcastMessage(POINTS_DESCRIPTION);

                    if(t.getSecondsLeft() == 15 || t.getSecondsLeft() == 10 || t.getSecondsLeft() == 5) {
                        for(Player p:Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                        }
                        Bukkit.broadcastMessage(ChatColor.AQUA + "There are " + ChatColor.GOLD + ChatColor.BOLD + t.getSecondsLeft() + ChatColor.RESET + ChatColor.AQUA + " seconds until Tag Runners Starts");
                    }
                    Tag.timeVar = t.getSecondsLeft();
                }
        );

        // Start scheduling, don't use the "run" method unless you want to skip a second
        pregameCountdown.scheduleTimer();

    }

}
