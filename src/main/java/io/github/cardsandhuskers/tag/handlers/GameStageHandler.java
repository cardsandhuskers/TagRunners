package io.github.cardsandhuskers.tag.handlers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.listeners.GlowPacketListener;
import io.github.cardsandhuskers.tag.listeners.PlayerAttackListener;
import io.github.cardsandhuskers.tag.listeners.PlayerLeaveListener;
import io.github.cardsandhuskers.tag.objects.Bracket;
import io.github.cardsandhuskers.tag.objects.Countdown;
import io.github.cardsandhuskers.teams.objects.Team;
import io.github.cardsandhuskers.teams.objects.TempPointsHolder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static io.github.cardsandhuskers.tag.Tag.*;
import static io.github.cardsandhuskers.teams.Teams.handler;
import static org.bukkit.Bukkit.getServer;

public class GameStageHandler {
    private Tag plugin;
    int totalRounds = 0;
    private HashMap<Team, Player> currentHunters;
    private Bracket bracket;

    private Team[][] matchups;
    //track how many opportunities each player has to be a hunter
    private HashMap<Player, Integer> hunterRounds;
    private ArrayList<Player> aliveRunners;
    private Countdown roundTimer;
    private PlayerDeathHandler deathHandler;
    private ArenaWallHandler wallHandler;
    private GlowPacketListener glowPacketListener;

    public GameStageHandler(Tag plugin, HashMap<Team, Player> currentHunters, HashMap<Player, Integer> hunterRounds) {
        this.plugin = plugin;
        this.currentHunters = currentHunters;
        this.hunterRounds = hunterRounds;
        aliveRunners = new ArrayList<>();
        bracket = new Bracket();
        this.deathHandler = new PlayerDeathHandler(plugin, this, aliveRunners);
        getServer().getPluginManager().registerEvents(new PlayerAttackListener(plugin, currentHunters, aliveRunners, deathHandler), plugin);
        getServer().getPluginManager().registerEvents(new PlayerLeaveListener(plugin, deathHandler), plugin);
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
        wallHandler = new ArenaWallHandler(plugin);
        glowPacketListener = new GlowPacketListener(plugin, aliveRunners, currentHunters, this);
        startRound();
    }

    public void startRound() {
        matchups = bracket.getMatchups(handler.getTeams(), round);
        deathHandler.setMatchups(matchups);
        deathHandler.resetEliminations();
        currentHunters.clear();
        aliveRunners.clear();
        deathHandler.unopposed = null;
        teleport(true);
        hunterTimer();


    }

    public void teleport(boolean kit) {
        String add = "";
        if(kit) add = "Kit";
        int teamCounter = 1;
        int arenaCounter = 1;
        while(plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + teamCounter + ".1") != null) {
            if(matchups.length < teamCounter) {
                break;
            }

            //System.out.println(teamCounter);
            Team teamA = matchups[teamCounter-1][0];
            Team teamB = matchups[teamCounter-1][1];

            if(!kit && teamA.getTeamName().equalsIgnoreCase("DUMMYTEAM")) {
                deathHandler.unopposed = teamB;
            } else if(!kit && teamB.getTeamName().equalsIgnoreCase("DUMMYTEAM")) {
                deathHandler.unopposed = teamA;
            }
            //System.out.println(teamA);
            //System.out.println(teamB);

            Location aRunners = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 2);
            Location bHunter = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 1);

            arenaCounter++;

            Location bRunners = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 2);
            Location aHunter = plugin.getConfig().getLocation("Arena" + add + "Spawns.Arena" + arenaCounter + "." + 1);

            for(Player p:teamA.getOnlinePlayers()) {
                if(p.equals(currentHunters.get(teamA)) && kit == false) p.teleport(aHunter);
                else p.teleport(aRunners);
                if(kit) {
                    p.sendTitle("Opponent: ", teamB.color + teamB.getTeamName(), 5, 20, 5);
                    p.sendMessage("Round " + round + ": " + teamA.color + teamA.getTeamName() + ChatColor.RESET + " vs. " + teamB.color + teamB.getTeamName());
                } else {
                    if(p.equals(currentHunters.get(teamA))) {
                        p.sendMessage("Hunts Remaining: " + (hunterRounds.get(p)-1));
                    }
                }
                p.setGameMode(GameMode.ADVENTURE);
            }
            for(Player p:teamB.getOnlinePlayers()) {
                if(p.equals(currentHunters.get(teamB)) && kit == false) p.teleport(bHunter);
                else p.teleport(bRunners);
                if(kit) {
                    p.sendTitle("Opponent: ", teamA.color + teamB.getTeamName(), 5, 20, 5);
                    p.sendMessage("Round " + round + ": " + teamB.color + teamB.getTeamName() + ChatColor.RESET + " vs. " + teamA.color + teamA.getTeamName());
                } else {
                    if(p.equals(currentHunters.get(teamB))) {
                        p.sendMessage("Hunts Remaining: " + (hunterRounds.get(p)-1));
                    }
                }
                p.setGameMode(GameMode.ADVENTURE);
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

    /**
     * Select team's hunter
     */
    public void hunterTimer() {
        int totalTime = plugin.getConfig().getInt("TagSelectionTime");
        Countdown timer = new Countdown((JavaPlugin)plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.KIT_SELECTION;
                    wallHandler.buildWalls();

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
                                }
                            }
                        }
                        //in case no one has hunting abilities left
                        if(!currentHunters.containsKey(t)) currentHunters.put(t, t.getOnlinePlayers().get(0));

                    }
                    for(Team t: handler.getTeams()) {
                        for(Player p:t.getOnlinePlayers()) {
                            Team playerTeam = handler.getPlayerTeam(p);
                            if(playerTeam == null) {

                            }
                            else {
                                Team opponentTeam = null;
                                for (Team[] matchup : matchups) {
                                    if (matchup[0].equals(playerTeam)) opponentTeam = matchup[1];
                                    if (matchup[1].equals(playerTeam)) opponentTeam = matchup[0];
                                }

                                //dummy team
                                //System.out.println("OPPTEAM: " + opponentTeam);

                                if(opponentTeam == null || opponentTeam.getTeamName().equalsIgnoreCase("DUMMYTEAM")) {
                                    if(currentHunters.get(playerTeam).equals(p)) {
                                        p.sendMessage(ChatColor.BOLD + "You will be hunting no one!");
                                        p.sendTitle("No Opponent", "", 5, 50, 5);

                                    } else {
                                        //player is runner
                                        p.sendMessage("You will be hunted by no one!");
                                        p.sendTitle("No Hunter!", "", 5, 50, 5);
                                    }
                                } else {
                                    //player is hunter
                                    if(!currentHunters.containsKey(playerTeam)) continue;
                                    if (currentHunters.get(playerTeam).equals(p)) {
                                        p.sendMessage(ChatColor.BOLD + "You will be hunting " + opponentTeam.color + opponentTeam.getTeamName());
                                        String members = "";
                                        for(Player pl:Bukkit.getOnlinePlayers()) {
                                            if(handler.getPlayerTeam(pl) != null && handler.getPlayerTeam(pl).equals(opponentTeam) && !currentHunters.containsKey(pl)) members += pl.getDisplayName() + ", ";
                                        }
                                        p.sendMessage("Players: " + opponentTeam.color + members);
                                        p.sendTitle("Chase Down:", opponentTeam.color + opponentTeam.getTeamName(), 5, 50, 5);

                                    } else {
                                        //player is runner
                                        Player hunter = currentHunters.get(opponentTeam);
                                        //System.out.println("HUNTER: " + hunter);
                                        p.sendMessage("You will be hunted by: " + opponentTeam.color + hunter.getDisplayName());
                                        p.sendTitle("Hunter:", opponentTeam.color + hunter.getDisplayName(), 5, 50, 5);
                                    }
                                }
                            }
                        }
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
        Countdown timer = new Countdown((JavaPlugin)plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.ROUND_STARTING;
                    teleport(false);
                    for(Team t: handler.getTeams()) {
                        for(Player p:t.getOnlinePlayers()) {
                            if(!currentHunters.get(t).equals(p)) aliveRunners.add(p);
                            p.setGameMode(GameMode.ADVENTURE);
                            p.setSaturation(20);
                            p.setHealth(20);
                            p.setFoodLevel(20);

                            p.setSwimming(false);
                        }
                    }
                    //System.out.println(currentHunters);
                    //System.out.println(aliveRunners);

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
                    var protocolManager = ProtocolLibrary.getProtocolManager();

                    /*for(Player recipient:Bukkit.getOnlinePlayers()) {
                        for(Player target:Bukkit.getOnlinePlayers()) {
                            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                            packet.getIntegers().write(0, target.getEntityId()); //Set packet's entity id
                            WrappedDataWatcher watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
                            WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class); //Found this through google, needed for some stupid reason
                            watcher.setEntity(target); //Set the new data watcher's target
                            watcher.setObject(0, serializer, (byte) (0x40)); //Set status to glowing, found on protocol page
                            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects()); //Make the packet's datawatcher the one we created

                            try {
                                protocolManager.sendServerPacket(recipient, packet);
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    }*/
                    wallHandler.deleteWalls();
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
        roundTimer = new Countdown((JavaPlugin)plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.ROUND_ACTIVE;

                    glowPacketListener.enableGlow();
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
        for(Player p:aliveRunners) {
            p.sendMessage("[+" +  ChatColor.YELLOW + "" + ChatColor.BOLD + survivalPoints + ChatColor.RESET + "] You survived to the end of the round!");
            handler.getPlayerTeam(p).addTempPoints(p, survivalPoints);
        }
        for(Player p:Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1,2);
        Bukkit.broadcastMessage("Round Over!");

        if(roundTimer != null) roundTimer.cancelTimer();

        int totalTime = plugin.getConfig().getInt("RoundOverTime");
        Countdown timer = new Countdown((JavaPlugin)plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = Tag.GameState.ROUND_OVER;
                    glowPacketListener.disableGlow();
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
            Bukkit.broadcastMessage("GAME OVER!");
            gameOver();
        } else {
            startRound();
        }
    }

    public void gameOver() {

        int totalTime = plugin.getConfig().getInt("GameOverTime");
        Countdown timer = new Countdown((JavaPlugin)plugin,
                totalTime,
                //Timer Start
                () -> {
                    gameState = GameState.GAME_OVER;
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

                    if (t.getSecondsLeft() == t.getTotalSeconds() - 5) {
                        for (Team team : handler.getTeams()) {
                            ArrayList<TempPointsHolder> tempPointsList = new ArrayList<>();
                            for (Player p : team.getOnlinePlayers()) {
                                if (team.getPlayerTempPoints(p) != null) {
                                    tempPointsList.add(team.getPlayerTempPoints(p));
                                }
                            }
                            Collections.sort(tempPointsList, Comparator.comparing(TempPointsHolder::getPoints));
                            Collections.reverse(tempPointsList);

                            for (Player p : team.getOnlinePlayers()) {
                                p.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Your Team Standings:");
                                p.sendMessage(ChatColor.DARK_BLUE + "------------------------------");
                                int number = 1;
                                for (TempPointsHolder h : tempPointsList) {
                                    p.sendMessage(number + ". " + handler.getPlayerTeam(p).color + h.getPlayer().getName() + ChatColor.RESET + "    Points: " + (int)h.getPoints());
                                    number++;
                                }
                                p.sendMessage(ChatColor.DARK_BLUE + "------------------------------\n");
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                            }
                        }
                    }
                    if (t.getSecondsLeft() == t.getTotalSeconds() - 10) {
                        ArrayList<TempPointsHolder> tempPointsList = new ArrayList<>();
                        for (Team team : handler.getTeams()) {
                            for (Player p : team.getOnlinePlayers()) {
                                tempPointsList.add(team.getPlayerTempPoints(p));
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                            }
                        }
                        Collections.sort(tempPointsList, Comparator.comparing(TempPointsHolder::getPoints));
                        Collections.reverse(tempPointsList);

                        int max;
                        if (tempPointsList.size() >= 5) {
                            max = 4;
                        } else {
                            max = tempPointsList.size() - 1;
                        }

                        Bukkit.broadcastMessage("\n" + ChatColor.RED + "" + ChatColor.BOLD + "Top 5 Players:");
                        Bukkit.broadcastMessage(ChatColor.DARK_RED + "------------------------------");
                        int number = 1;
                        for (int i = 0; i <= max; i++) {
                            TempPointsHolder h = tempPointsList.get(i);
                            Bukkit.broadcastMessage(number + ". " + handler.getPlayerTeam(h.getPlayer()).color + h.getPlayer().getName() + ChatColor.RESET + "    Points: " + (int)h.getPoints());
                            number++;
                        }
                        Bukkit.broadcastMessage(ChatColor.DARK_RED + "------------------------------");
                    }

                    if (t.getSecondsLeft() == t.getTotalSeconds() - 15) {
                        ArrayList<Team> teamList = handler.getTempPointsSortedList();

                        Bukkit.broadcastMessage(ChatColor.BLUE + "" + ChatColor.BOLD + "Team Performance:");
                        Bukkit.broadcastMessage(ChatColor.GREEN + "------------------------------");
                        int counter = 1;
                        for (Team team : teamList) {
                            Bukkit.broadcastMessage(counter + ". " + team.color + ChatColor.BOLD + team.getTeamName() + ChatColor.RESET + " Points: " + (int)team.getTempPoints());
                            counter++;
                        }
                        Bukkit.broadcastMessage(ChatColor.GREEN + "------------------------------");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                        }
                    }




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

}
