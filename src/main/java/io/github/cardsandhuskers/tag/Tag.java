package io.github.cardsandhuskers.tag;

import io.github.cardsandhuskers.tag.commands.*;
import io.github.cardsandhuskers.tag.objects.Placeholder;
import io.github.cardsandhuskers.tag.objects.StatCalculator;
import io.github.cardsandhuskers.teams.Teams;
import io.github.cardsandhuskers.teams.handlers.TeamHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class Tag extends JavaPlugin {
    public static TeamHandler handler;
    public static float multiplier = 1;
    public static GameState gameState = GameState.GAME_STARTING;
    public StartGameCommand startGameCommand;
    public static int timeVar = 0;
    public static int round = 0;
    public static HashMap<Player, Integer> tags;
    public static HashMap<Player, Integer> roundWins;
    public StatCalculator statCalculator;
    public static final int TEAM_SIZE = 3;

    @Override
    public void onEnable() {
        // Plugin startup logic

        //Placeholder API validation
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            /*
             * We register the EventListener here, when PlaceholderAPI is installed.
             * Since all events are in the main class (this class), we simply use "this"
             */
            new Placeholder(this).register();

        } else {
            /*
             * We inform about the fact that PlaceholderAPI isn't installed.
             */
            System.out.println("Could not find PlaceholderAPI!");
            //Bukkit.getPluginManager().disablePlugin(this);
        }

        handler = Teams.handler;

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        //Register Commands
        startGameCommand = new StartGameCommand(this);
        getCommand("startTag").setExecutor(startGameCommand);
        getCommand("setTagWorldSpawn").setExecutor(new SetWorldSpawnCommand(this));
        getCommand("setTagArenaSpawn").setExecutor(new SetArenaSpawnCommand(this));
        getCommand("setTagKitSpawn").setExecutor(new SetHunterSelectionCommand(this));
        getCommand("setTagLobby").setExecutor(new SetLobbyCommand(this));
        getCommand("setTagArenaWall").setExecutor(new SetArenaWallCommand(this));
        getCommand("setTagHunterChamber").setExecutor(new SetHunterChamber(this));
        getCommand("reloadTag").setExecutor(new ReloadConfigCommand(this));

        statCalculator = new StatCalculator(this);
        try {
            statCalculator.calculateStats();
        } catch (Exception e) {
            StackTraceElement[] trace = e.getStackTrace();
            String str = "";
            for(StackTraceElement element:trace) str += element.toString() + "\n";
            this.getLogger().severe("ERROR Calculating Stats!\n" + str);
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public enum GameState {
        GAME_STARTING,
        ROUND_STARTING,
        KIT_SELECTION,
        ROUND_ACTIVE,
        ROUND_OVER,
        GAME_OVER
    }
}
