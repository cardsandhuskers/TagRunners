package io.github.cardsandhuskers.tag.commands;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.handlers.GameStageHandler;
import io.github.cardsandhuskers.tag.listeners.PlayerDamageListener;
import io.github.cardsandhuskers.tag.listeners.PlayerMoveListener;
import io.github.cardsandhuskers.tag.objects.Countdown;
import io.github.cardsandhuskers.tag.objects.GameMessages;
import io.github.cardsandhuskers.teams.objects.Team;
import io.github.cardsandhuskers.tag.objects.Stats;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import static io.github.cardsandhuskers.tag.Tag.handler;
import static io.github.cardsandhuskers.tag.Tag.multiplier;
import static org.bukkit.Bukkit.getServer;

public class StartGameCommand implements CommandExecutor {
    Tag plugin;
    Countdown pregameCountdown;
    HashMap<Team, Player> currentHunters;
    public HashMap<Player, Integer> hunterRounds;
    public ArrayList<Player> aliveRunners;
    public GameStageHandler gameStageHandler;
    private Stats stats;


    public StartGameCommand(Tag plugin) {
        this.plugin = plugin;
        this.stats = new Stats("Round, Player, Hunter, Killed");
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
        gameStageHandler = new GameStageHandler(plugin, currentHunters, hunterRounds, stats);

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
        pregameCountdown = new Countdown(plugin,
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
                    if(t.getSecondsLeft() == totalSeconds - 2) Bukkit.broadcastMessage(GameMessages.gameDescription());
                    if(t.getSecondsLeft() == totalSeconds - 12) Bukkit.broadcastMessage(GameMessages.pointsDescription(plugin));

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
