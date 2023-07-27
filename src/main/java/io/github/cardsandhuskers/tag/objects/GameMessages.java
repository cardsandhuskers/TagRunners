package io.github.cardsandhuskers.tag.objects;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.teams.objects.Team;
import io.github.cardsandhuskers.teams.objects.TempPointsHolder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static io.github.cardsandhuskers.tag.Tag.*;
import static io.github.cardsandhuskers.teams.Teams.handler;

public class GameMessages {

    /**
     * @return String to announce for game rules
     */
    public static String gameDescription() {
        String GAME_DESCRIPTION =
                ChatColor.STRIKETHROUGH + "----------------------------------------\n" + ChatColor.RESET +
                StringUtils.center(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Tag Runners", 30) +
                ChatColor.BLUE + "" + ChatColor.BOLD + "\nHow To Play:" +
                ChatColor.RESET + "\nYou will be placed in an arena against the other teams in a round robin format." +
                "\nYou must choose a player to be the hunter, while the rest will be runners." +
                "\nTo hunt, walk into the jar. Each player has a limited amount of hunting opportunities, so choose wisely." +
                "\nYour hunter must tag all the opposing runners before your opposing hunter tags all of you to get points" +
                "\n----------------------------------------";
        return GAME_DESCRIPTION;
    }

    /**
     *
     * @param plugin
     * @return String to announce for points
     */
    public static String pointsDescription(Tag plugin) {
        String POINTS_DESCRIPTION =
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
        return POINTS_DESCRIPTION;
    }



    /**
     * Announces the top 5 earning players in the game
     */
    public static void announceTopPlayers() {
        ArrayList<TempPointsHolder> tempPointsList = new ArrayList<>();
        for(Team team: handler.getTeams()) {
            for(Player p:team.getOnlinePlayers()) {
                tempPointsList.add(team.getPlayerTempPoints(p));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            }
        }

        Collections.sort(tempPointsList, Comparator.comparing(TempPointsHolder::getPoints));
        Collections.reverse(tempPointsList);

        int max;
        if(tempPointsList.size() >= 5) {
            max = 4;
        } else {
            max = tempPointsList.size() - 1;
        }

        Bukkit.broadcastMessage("\n" + ChatColor.RED + "" + ChatColor.BOLD + "Top 5 Players:");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "------------------------------");
        int number = 1;
        for(int i = 0; i <= max; i++) {
            TempPointsHolder h = tempPointsList.get(i);
            Bukkit.broadcastMessage(number + ". " + handler.getPlayerTeam(h.getPlayer()).color + h.getPlayer().getName() + ChatColor.RESET + "    Points: " +  h.getPoints());
            number++;
        }
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "------------------------------");
    }

    /**
     * Announces the leaderboard for players on your team based on points earned in the game
     */
    public static void announceTeamPlayers() {
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
                    p.sendMessage(number + ". " + handler.getPlayerTeam(p).color + h.getPlayer().getName() + ChatColor.RESET + "    Points: " + h.getPoints());
                    number++;
                }
                p.sendMessage(ChatColor.DARK_BLUE + "------------------------------\n");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            }
        }
    }

    /**
     * Announces the leaderboard of teams based on points earned in the game
     */
    public static void announceTeamLeaderboard() {
        ArrayList<Team> teamList = handler.getTeams();
        Collections.sort(teamList, Comparator.comparing(Team::getTempPoints));
        Collections.reverse(teamList);

        Bukkit.broadcastMessage(ChatColor.BLUE + "" + ChatColor.BOLD + "Team Leaderboard:");
        Bukkit.broadcastMessage(ChatColor.GREEN + "------------------------------");
        int counter = 1;
        for(Team team:teamList) {
            Bukkit.broadcastMessage(counter + ". " + team.color + ChatColor.BOLD +  team.getTeamName() + ChatColor.RESET + " Points: " + team.getTempPoints());
            counter++;
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "------------------------------");
        for(Player p: Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        }
    }

    public static void announceOpponents(Team playerTeam, Team opponentTeam) {
        for(Player p: playerTeam.getOnlinePlayers()) {
            p.sendTitle("Opponent: ", opponentTeam.color + opponentTeam.getTeamName(), 5, 60, 5);
            p.sendMessage("Round " + round + ": " + playerTeam.color + playerTeam.getTeamName() + ChatColor.RESET + " vs. " + opponentTeam.color + opponentTeam.getTeamName());
        }
    }
    public static void preroundAnnouncements(Team playerTeam, Team opponentTeam, int rounds, HashMap<Team, Player> currentHunters) {
        Player hunter = currentHunters.get(playerTeam);

        if (opponentTeam == null || opponentTeam.getTeamName().equalsIgnoreCase("DUMMYTEAM")) {
            hunter.sendMessage(ChatColor.BOLD + "You will be hunting no one!");
            hunter.sendTitle("No Opponent", "", 5, 50, 5);

            for(Player p: playerTeam.getOnlinePlayers()) {
                if(p == hunter) continue;;
                //player is runner
                p.sendMessage("You will be hunted by no one!");
                p.sendTitle("No Hunter!", "", 5, 50, 5);
            }
        } else {

            hunter.sendMessage(ChatColor.GOLD + "Hunts Remaining: " + ChatColor.RED + ChatColor.BOLD + rounds);
            hunter.sendMessage(ChatColor.BOLD + "You will be hunting " + opponentTeam.color + opponentTeam.getTeamName());
            String members = "";


            for (Player pl : Bukkit.getOnlinePlayers()) {
                Team team = handler.getPlayerTeam(pl);
                if (team != null && team == opponentTeam && currentHunters.get(team) != pl)
                    members += pl.getDisplayName() + ", ";
            }
            if(members.length() > 1) members = members.substring(0, members.length() - 2);
            hunter.sendMessage("Players: " + opponentTeam.color + members);
            hunter.sendTitle("Chase Down:", opponentTeam.color + opponentTeam.getTeamName(), 5, 50, 5);

            Player oppHunter = currentHunters.get(opponentTeam);
            for (Player p : playerTeam.getOnlinePlayers()) {
                if (p == hunter) continue;
                p.sendMessage("You will be hunted by: " + opponentTeam.color + oppHunter.getDisplayName());
                p.sendTitle("Hunter:", opponentTeam.color + oppHunter.getDisplayName(), 5, 50, 5);
            }
        }
    }

    public static void roundOverAnnouncements(ArrayList<Player> aliveRunners, HashMap<Team,Player> currentHunters, int survivalPoints, ArrayList<Team> winningTeams, Team[][] matchups) {
        Bukkit.broadcastMessage(ChatColor.GREEN + "Round over!");

        for(Player p:Bukkit.getOnlinePlayers()) {
            Team t = handler.getPlayerTeam(p);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1,2);
            if(aliveRunners.contains(p)) {
                p.sendMessage("[+" +  ChatColor.YELLOW + "" + ChatColor.BOLD + survivalPoints + ChatColor.RESET + "] " + ChatColor.GREEN + " You survived to the end of the round!");
                p.sendTitle(ChatColor.GREEN + "Round Over", ChatColor.GREEN + "You Survived!", 5, 60, 5);
            } else if(t != null && currentHunters.get(t) == p) {
                p.sendTitle(ChatColor.GREEN + "Round Over", ChatColor.GREEN + "Good Job Hunting", 5, 60, 5);
            } else {
                if(t != null) p.sendTitle(ChatColor.RED + "Round Over", ChatColor.RED + "You didn't make it", 5, 60, 5);
            }
        }

        Bukkit.broadcastMessage("\nRound Summary: ");
        for(int i = 0; i <matchups.length; i++) {
            boolean gameFound = false;
            for(Team t: winningTeams) {
                if(matchups[i][0].equals(t)) {
                    Bukkit.broadcastMessage(StringUtils.center(matchups[i][0].color + ChatColor.BOLD + matchups[i][0].getTeamName() + ChatColor.RESET + "  vs.  " + ChatColor.DARK_GRAY + matchups[i][1].getTeamName(), 45));
                    gameFound = true;
                } else if(matchups[i][1].equals(t)){
                    Bukkit.broadcastMessage(StringUtils.center(ChatColor.DARK_GRAY + matchups[i][0].getTeamName() + ChatColor.RESET + "  vs.  " + matchups[i][1].color + ChatColor.BOLD + matchups[i][1].getTeamName(), 45));
                    gameFound = true;
                }
            }
            if(!gameFound) {
                Bukkit.broadcastMessage(StringUtils.center(matchups[i][0].color + ChatColor.BOLD + matchups[i][0].getTeamName() + ChatColor.RESET + "  vs.  " + matchups[i][1].color + ChatColor.BOLD + matchups[i][1].getTeamName(), 45));
            }
        }

    }
}
