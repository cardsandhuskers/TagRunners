package io.github.cardsandhuskers.tag.handlers;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.Tag.GameState;
import io.github.cardsandhuskers.tag.listeners.*;
import io.github.cardsandhuskers.tag.objects.Bracket;
import io.github.cardsandhuskers.tag.objects.Countdown;
import io.github.cardsandhuskers.tag.objects.GameMessages;
import io.github.cardsandhuskers.teams.objects.Team;
import io.github.cardsandhuskers.tag.objects.Stats;


import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static io.github.cardsandhuskers.tag.Tag.*;
import static io.github.cardsandhuskers.teams.Teams.handler;
import static org.bukkit.Bukkit.getServer;

public class GameStageHandler {
    private final Tag plugin;
    static int totalRounds = 0;
    private final HashMap<Team, Player> currentHunters;
    private final Bracket bracket;
    private Team[][] matchups;
    //track how many opportunities each player has to be a hunter
    private final HashMap<Player, Integer> hunterRounds;
    private final ArrayList<Player> aliveRunners;
    private Countdown roundTimer;
    private final PlayerDeathHandler deathHandler;
    private ArenaHandler arenaHandler;
    private final GlowHandler glowHandler;
    private Stats killStats;
    private Stats winStats;

    //private GlowPacketListener glowPacketListener;

    public GameStageHandler(Tag plugin, HashMap<Team, Player> currentHunters, HashMap<Player, Integer> hunterRounds, Stats killStats) {
        this.plugin = plugin;
        this.currentHunters = currentHunters;
        this.hunterRounds = hunterRounds;
        aliveRunners = new ArrayList<>();
        bracket = new Bracket();
        this.deathHandler = new PlayerDeathHandler(plugin, this, aliveRunners, killStats);
        getServer().getPluginManager().registerEvents(new PlayerAttackListener(plugin, currentHunters, aliveRunners, deathHandler), plugin);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(plugin), plugin);
        getServer().getPluginManager().registerEvents(new PlayerLeaveListener(plugin, deathHandler), plugin);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(), plugin);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(currentHunters, hunterRounds, plugin), plugin);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), plugin);

        glowHandler = new GlowHandler(plugin, aliveRunners, matchups, currentHunters, this);
        getServer().getPluginManager().registerEvents(new PlayerClickListener(glowHandler), plugin);
        
        
        this.killStats = killStats;
        this.winStats = new Stats("Round,winningPlayer,winningTeam,losingTeam");
    }


    public void startGame() {
        round = 1;
        if(handler.getNumTeams() % 2 == 1) totalRounds = handler.getNumTeams();
        else totalRounds = handler.getNumTeams() - 1;

        //System.out.println("NUM TAGS: " + numTags);
        for(Team t: handler.getTeams()) {
            int numTags = (int)Math.ceil((double)totalRounds / (double)t.getSize());
            for(Player p:t.getOnlinePlayers()) {
                hunterRounds.put(p, numTags);
            }
        }
        for(Player p:Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            p.setSaturation(20);
            p.setHealth(20);
            p.setFoodLevel(20);
        }
        arenaHandler = new ArenaHandler(plugin);

        //glowPacketListener = new GlowPacketListener(plugin, aliveRunners, currentHunters, this);
        startRound();
    }

    public void startRound() {
        matchups = bracket.getMatchups(handler.getTeams(), round);
        deathHandler.setMatchups(matchups);
        deathHandler.resetEliminations();
        currentHunters.clear();
        aliveRunners.clear();
        deathHandler.unopposed = null;
        arenaHandler.teleportPlayers(true, matchups, deathHandler, currentHunters, hunterRounds);
        hunterTimer();

    }



    /**
     * Select team's hunter
     */
    public void hunterTimer() {
        int totalTime = plugin.getConfig().getInt("TagSelectionTime");
        Countdown timer = new Countdown(plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.KIT_SELECTION;
                    arenaHandler.buildWalls();

                },

                //Timer End
                () -> {
                    //make sure teams have a hunter, else one has to be picked, this needs to be improved
                    //TODO
                    for(Team t:handler.getTeams()) {
                        if (!currentHunters.containsKey(t)) {
                            for(Player p:t.getOnlinePlayers()) {
                                if(!hunterRounds.containsKey(p)) {
                                    hunterRounds.put(p, totalRounds/t.getOnlinePlayers().size() + 1);
                                }
                                if(hunterRounds.get(p) > 0) {
                                    currentHunters.put(t, p);
                                    break;
                                    //timevar
                                }
                            }
                        }
                        //in case no one has hunting abilities left
                        if(!currentHunters.containsKey(t)) currentHunters.put(t, t.getOnlinePlayers().get(0));

                    }
                    preround();


                },

                //Each Second
                (t) -> {
                    timeVar = t.getSecondsLeft();

                    if(t.getSecondsLeft() == totalTime) {
                        Bukkit.broadcastMessage(ChatColor.AQUA + "You have " + ChatColor.GOLD + ChatColor.BOLD + t.getSecondsLeft() + ChatColor.RESET + ChatColor.AQUA + " seconds to choose your Hunter!");
                    }

                    if(t.getSecondsLeft() == 5) {
                        Bukkit.broadcastMessage(ChatColor.AQUA + "You have " + ChatColor.GOLD + ChatColor.BOLD + t.getSecondsLeft() + ChatColor.RESET + ChatColor.AQUA + " seconds choose your Hunter!");
                        for(Player p:Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                        }
                    }
                }
        );

        // Start scheduling, don't use the "run" method unless you want to skip a second
        timer.scheduleTimer();
    }

    /**
     * Preround countdown time
     */
    public void preround() {
        int totalTime = plugin.getConfig().getInt("PreroundTime");
        Countdown timer = new Countdown(plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.ROUND_STARTING;

                    for(Team t: handler.getTeams()) {
                        for(Player p:t.getOnlinePlayers()) {
                            if(!currentHunters.get(t).equals(p)) {
                                aliveRunners.add(p);
                                glowHandler.giveRunnerVision(p, Material.ENDER_PEARL);
                            }
                            p.setGameMode(GameMode.ADVENTURE);
                            p.setSaturation(20);
                            p.setHealth(20);
                            p.setFoodLevel(20);

                            p.setSwimming(false);
                        }
                    }
                    arenaHandler.teleportPlayers(false, matchups, deathHandler, currentHunters, hunterRounds);

                    glowHandler.enableGlow();


                },

                //Timer End
                () -> {
                    for(Team t: handler.getTeams()) {
                        for(Player p:t.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                            p.sendTitle(handler.getPlayerTeam(p).color + "GO!", "", 2, 12, 2);
                            p.setSwimming(false);
                        }
                    }

                    arenaHandler.deleteWalls();
                    roundActive();
                },

                //Each Second
                (t) -> {
                    timeVar = t.getSecondsLeft();
                    if(t.getSecondsLeft() < 5) {
                        for (Team team : handler.getTeams()) {
                            for (Player p : team.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                                p.sendTitle(handler.getPlayerTeam(p).color + "Round Starts in", ">" + t.getSecondsLeft() + "<", 0, 20, 0);
                            }
                        }
                    }
                }
        );

        // Start scheduling, don't use the "run" method unless you want to skip a second
        timer.scheduleTimer();

    }

    /**
     * During round timer
     */
    public void roundActive() {
        int totalTime = plugin.getConfig().getInt("RoundTime");
        roundTimer = new Countdown(plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.ROUND_ACTIVE;
                },

                //Timer End
                () -> {
                    roundOver();
                },

                //Each Second
                (t) -> {
                    timeVar = t.getSecondsLeft();
                    //System.out.println(aliveRunners);

                    if(t.getSecondsLeft() == 10 || t.getSecondsLeft() <= 5) {
                        Bukkit.broadcastMessage(ChatColor.AQUA + "Round ends in " + ChatColor.GOLD + ChatColor.BOLD + t.getSecondsLeft() + ChatColor.RESET + ChatColor.AQUA + " Seconds!");
                    }
                    if(t.getSecondsLeft() != t.getTotalSeconds() && t.getSecondsLeft() % 10 == 0) {
                        double survivalPoints = plugin.getConfig().getInt("survivalPoints") * multiplier;
                        for(Player p: aliveRunners) {
                            handler.getPlayerTeam(p).addTempPoints(p, survivalPoints);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                            p.sendMessage("[+" +  ChatColor.YELLOW + "" + ChatColor.BOLD + survivalPoints + ChatColor.RESET + "] You survived " + (t.getTotalSeconds() - t.getSecondsLeft()) + " seconds!");
                        }
                    }
                }
        );

        // Start scheduling, don't use the "run" method unless you want to skip a second
        roundTimer.scheduleTimer();

    }

    /**
     * end of round timer
     */
    public void roundOver() {
        double survivalPoints = plugin.getConfig().getInt("fullSurvivalPoints") * multiplier;
        for(Player surviver:aliveRunners) {
            handler.getPlayerTeam(surviver).addTempPoints(surviver, survivalPoints);
            glowHandler.takeRunnerVision(surviver);

            Team survivorTeam = handler.getPlayerTeam(surviver);
            Team hunterTeam = null;

            for (Team[] matchup : matchups) {
                if (matchup[0].equals(survivorTeam)) hunterTeam = matchup[1];
                if (matchup[1].equals(survivorTeam)) hunterTeam = matchup[0];
            }

            Player hunter = currentHunters.get(hunterTeam);

            //round, playerName, playerTeam, attackerName, attackerTeam, timeOfDeath
            String entryLine = totalRounds + "," + surviver.getName() + "," + handler.getPlayerTeam(surviver).getTeamName() + "," + hunter.getName() + "," + hunterTeam.getTeamName() + ",Survived";
            killStats.addEntry(entryLine);
        }

        updateWinStats();

        GameMessages.roundOverAnnouncements(aliveRunners, currentHunters, (int)survivalPoints, deathHandler.winningTeams, matchups);
        glowHandler.disableGlow();
        if(roundTimer != null) roundTimer.cancelTimer();

        int totalTime = plugin.getConfig().getInt("RoundOverTime");
        Countdown timer = new Countdown(plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.ROUND_OVER;
                },

                //Timer End
                () -> {
                    endRound();
                },

                //Each Second
                (t) -> {
                    timeVar = t.getSecondsLeft();

                }
        );

        // Start scheduling, don't use the "run" method unless you want to skip a second
        timer.scheduleTimer();
    }


    public void endRound() {
        for(Player p: currentHunters.values()) {
            hunterRounds.put(p, hunterRounds.get(p)-1);
        }


        round++;
        if(round > totalRounds) {
            //Bukkit.broadcastMessage("GAME OVER!");
            gameOver();
        } else {
            startRound();
        }
    }

    public void gameOver() {

        int totalTime = plugin.getConfig().getInt("GameOverTime");
        Countdown timer = new Countdown(plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = GameState.GAME_OVER;
                    killStats.writeToFile(plugin.getDataFolder().toPath().toString(), "tagStats");
                },

                //Timer End
                () -> {
                    try {
                        plugin.statCalculator.saveRecords();
                    } catch (IOException e) {
                        StackTraceElement[] trace = e.getStackTrace();
                        String str = "";
                        for(StackTraceElement element:trace) str += element.toString() + "\n";
                        plugin.getLogger().severe("ERROR Calculating Stats!\n" + str);
                    }
                    HandlerList.unregisterAll(plugin);

                    try {
                        Location lobby = plugin.getConfig().getLocation("Lobby");
                        for (Player p : Bukkit.getOnlinePlayers()) p.teleport(lobby);
                    } catch (Exception e) {Bukkit.broadcastMessage("Lobby does not exist!");}

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "startRound");
                },

                //Each Second
                (t) -> {
                    timeVar = t.getSecondsLeft();

                    if (t.getSecondsLeft() == t.getTotalSeconds() - 2) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.setGameMode(GameMode.SPECTATOR);
                        }
                    }

                    if(t.getSecondsLeft() == t.getTotalSeconds() - 1) GameMessages.announceTopPlayers();
                    if(t.getSecondsLeft() == t.getTotalSeconds() - 6) GameMessages.announceTeamPlayers();
                    if(t.getSecondsLeft() == t.getTotalSeconds() - 11) GameMessages.announceTeamLeaderboard();

                }
        );

        // Start scheduling, don't use the "run" method unless you want to skip a second
        timer.scheduleTimer();
    }

    public Team[][] getMatchups() {
        return matchups;
    }
    public int getElapsedTime() {
        if(roundTimer.getSecondsLeft() == 0) return 60;
        return roundTimer.getTotalSeconds() - roundTimer.getSecondsLeft();
    }

    public Player getHunter(Team t) {
        if(!currentHunters.containsKey(t)) return null;
        return currentHunters.get(t);
    }
    public ArrayList<Player> getAliveRunners() {
        return aliveRunners;
    }

    /**
     * Adds the teams that won to the winStats object,
     * which is used to store the teams/players that won
     * each round. This function should be called before
     * stats have been cleared for the start of a new round.
     * 
     * @author J. Scotty Solomon
     */
    private void updateWinStats() {
        //Round,winningPlayer,winningTeam,losingTeam
        ArrayList<Team> winningTeams = deathHandler.getWinningTeams();

        for(Team team: winningTeams) {
            Team loserTeam = null;

            for (Team[] matchup : matchups) {
                if (matchup[0].equals(team)) loserTeam = matchup[1];
                if (matchup[1].equals(team)) loserTeam = matchup[0];
            }

            ArrayList<Player> players = team.getOnlinePlayers();

            for(Player player: players) {
                String lineEntry = totalRounds + "," + player.getName() + "," + team.getTeamName() + "," + loserTeam.getTeamName();
                winStats.addEntry(lineEntry);
            }
        }
        
    }

}
