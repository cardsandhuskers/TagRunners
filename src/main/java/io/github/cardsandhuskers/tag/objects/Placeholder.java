package io.github.cardsandhuskers.tag.objects;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.teams.objects.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static io.github.cardsandhuskers.tag.Tag.*;

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
        if(s.equalsIgnoreCase("yourRunners")) {
            Team team = handler.getPlayerTeam((Player) p);
            if(team == null) return "";

            int aliveOpps = 0;
            for(Player opp:plugin.startGameCommand.gameStageHandler.getAliveRunners()) {
                if(handler.getPlayerTeam(opp) == team) aliveOpps++;
            }
            String str = team.color + team.getTeamName() + ChatColor.RESET + "  ";
            for(int i = 0; i < aliveOpps; i++) {
                str += "⬤ ";
            }
            for(int i = aliveOpps; i < team.getSize() - 1; i++) {
                str += "◯ ";
            }

            return str;

        }
        if(s.equalsIgnoreCase("oppRunners")) {
            Team team = handler.getPlayerTeam((Player) p);
            if(team == null) return "";
            Team hunterTeam = null;

            Team[][] matchups = plugin.startGameCommand.gameStageHandler.getMatchups();
            if(matchups == null) return "";
            for (Team[] matchup : matchups) {
                if (matchup[0].equals(team)) hunterTeam = matchup[1];
                if (matchup[1].equals(team)) hunterTeam = matchup[0];
            }
            if(hunterTeam != null) {
                int aliveOpps = 0;
                for(Player opp:plugin.startGameCommand.gameStageHandler.getAliveRunners()) {
                    if(handler.getPlayerTeam(opp) == hunterTeam) aliveOpps++;
                }
                String str = hunterTeam.color + hunterTeam.getTeamName() + ChatColor.RESET + "  ";
                for(int i = 0; i < aliveOpps; i++) {
                    str += "⬤ ";
                }
                for(int i = aliveOpps; i < hunterTeam.getSize() - 1; i++) {
                    str += "◯ ";
                }

                return str;
            }
            return "";
        }


        String[] values = s.split("_");
        //playerKills, totalKills, wins
        // lb pos
        //playerKills_1

        try {
            int place = Integer.parseInt(values[1]);

            if (values[0].equalsIgnoreCase("playerKills")) {
                StatCalculator.EventStatsHolder holder = plugin.statCalculator.getEventStatsHolder(StatCalculator.PlayerStatsComparator.SortType.KILLS, place);
                if(holder == null) return "";

                String color = "";
                if (handler.getPlayerTeam(Bukkit.getPlayer(holder.name)) != null)
                    color = handler.getPlayerTeam(Bukkit.getPlayer(holder.name)).color;
                return color + holder.name + ChatColor.RESET + " Event " + holder.eventNum + ": " + holder.kills;
            }

            StatCalculator.PlayerStatsComparator.SortType sortType = StatCalculator.PlayerStatsComparator.SortType.WINS;
            if (values[0].equalsIgnoreCase("totalKills")) sortType = StatCalculator.PlayerStatsComparator.SortType.KILLS;
            else if (values[0].equalsIgnoreCase("wins")) sortType = StatCalculator.PlayerStatsComparator.SortType.WINS;

            StatCalculator.PlayerStatsHolder holder = plugin.statCalculator.getStatsHolder(sortType, place);
            if(holder == null) return "";

            int value = 0;
            if (values[0].equalsIgnoreCase("totalKills")) value = holder.getKills();
            else if (values[0].equalsIgnoreCase("wins")) value = holder.getWins();

            String color = "";
            if (handler.getPlayerTeam(Bukkit.getPlayer(holder.name)) != null)
                color = handler.getPlayerTeam(Bukkit.getPlayer(holder.name)).color;
            return color + holder.name + ChatColor.RESET + ": " + value;

        } catch (Exception e) {
            e.printStackTrace();

            StackTraceElement[] trace = e.getStackTrace();
            String str = "";
            for(StackTraceElement element:trace) str += element.toString() + "\n";
            plugin.getLogger().severe("Error with Placeholder!\n" + str);
        }

        return null;
    }
}
