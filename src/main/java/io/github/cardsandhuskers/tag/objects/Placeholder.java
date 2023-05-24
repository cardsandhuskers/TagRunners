package io.github.cardsandhuskers.tag.objects;

import io.github.cardsandhuskers.tag.Tag;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static io.github.cardsandhuskers.tag.Tag.gameState;
import static io.github.cardsandhuskers.tag.Tag.handler;
import static io.github.cardsandhuskers.tag.Tag.round;
import static io.github.cardsandhuskers.tag.Tag.timeVar;

public class Placeholder extends PlaceholderExpansion {
    private final Tag plugin;

    public Placeholder(Tag plugin) {
        this.plugin = plugin;
    }


    @Override
    public String getIdentifier() {
        return "Tag";
    }
    @Override
    public String getAuthor() {
        return "cardsandhuskers";
    }
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    @Override
    public boolean persist() {
        return true;
    }


    @Override
    public String onRequest(OfflinePlayer p, String s) {
        if(s.equalsIgnoreCase("timer")) {
            int mins = timeVar / 60;
            String seconds = String.format("%02d", timeVar - (mins * 60));
            return mins + ":" + seconds;
        }
        if(s.equalsIgnoreCase("timerstage")) {
            switch (gameState) {
                case GAME_STARTING: return "Game Starts in";
                case KIT_SELECTION: return "Kit Selection";
                case ROUND_STARTING: return "Round Starts";
                case ROUND_ACTIVE: return "Round Ends";
                case ROUND_OVER: return "Next Round";
                case GAME_OVER: return "Return to Lobby";
            }
        }
        if(s.equalsIgnoreCase("round")) {
            int currentRound;
            int totalRounds;
            if(handler.getNumTeams() %2 == 0) {
                totalRounds = handler.getNumTeams() - 1;
            } else {
                totalRounds = handler.getNumTeams();
            }
            if(round <= totalRounds) {
                currentRound = round;
            } else {
                currentRound = totalRounds;
            }
            return currentRound + "/" + (totalRounds);
        }

        if(s.equalsIgnoreCase("huntsRemaining")) {
            if(p.getPlayer() != null) {
                if(plugin.startGameCommand == null || plugin.startGameCommand.hunterRounds == null) return "-";
                if(plugin.startGameCommand.hunterRounds.containsKey(p.getPlayer())) {
                    return plugin.startGameCommand.hunterRounds.get(p.getPlayer()) + "";
                }
            }
            return "-";
        }

        String[] values = s.split("_");
        //playerKills, totalKills, wins
        // lb pos
        //playerKills_1
        try {
            if (values[0].equalsIgnoreCase("playerKills")) {
                ArrayList<StatCalculator.SingleGameKillsHolder> killsHolders = plugin.statCalculator.getSGKillsHolders();
                if(Integer.parseInt(values[1]) > killsHolders.size()) return  "";
                StatCalculator.SingleGameKillsHolder holder = killsHolders.get(Integer.parseInt(values[1]) - 1);

                String color = "";
                if (handler.getPlayerTeam(Bukkit.getPlayer(holder.name)) != null)
                    color = handler.getPlayerTeam(Bukkit.getPlayer(holder.name)).color;
                return color + holder.name + ChatColor.RESET + " Event " + holder.eventNum + ": " + holder.kills;


            }
            if (values[0].equalsIgnoreCase("totalKills")) {
                ArrayList<StatCalculator.PlayerStatsHolder> killsHolders = plugin.statCalculator.getStatsHolders(StatCalculator.PlayerStatsComparator.SortType.KILLS);
                if(Integer.parseInt(values[1]) > killsHolders.size()) return  "";
                StatCalculator.PlayerStatsHolder holder = killsHolders.get(Integer.parseInt(values[1]) - 1);
                String color = "";
                if (handler.getPlayerTeam(Bukkit.getPlayer(holder.name)) != null)
                    color = handler.getPlayerTeam(Bukkit.getPlayer(holder.name)).color;
                return color + holder.name + ChatColor.RESET + ": " + holder.kills;
            }
            if (values[0].equalsIgnoreCase("wins")) {
                ArrayList<StatCalculator.PlayerStatsHolder> killsHolders = plugin.statCalculator.getStatsHolders(StatCalculator.PlayerStatsComparator.SortType.WINS);
                if(Integer.parseInt(values[1]) > killsHolders.size()) return  "";
                StatCalculator.PlayerStatsHolder holder = killsHolders.get(Integer.parseInt(values[1]) - 1);
                String color = "";
                if (handler.getPlayerTeam(Bukkit.getPlayer(holder.name)) != null)
                    color = handler.getPlayerTeam(Bukkit.getPlayer(holder.name)).color;
                return color + holder.name + ChatColor.RESET + ": " + holder.wins;
            }
        } catch (Exception e) {
            StackTraceElement[] trace = e.getStackTrace();
            String str = "";
            for(StackTraceElement element:trace) str += element.toString() + "\n";
            plugin.getLogger().severe("Error with Placeholder!\n" + str);
        }

        return null;
    }
}
