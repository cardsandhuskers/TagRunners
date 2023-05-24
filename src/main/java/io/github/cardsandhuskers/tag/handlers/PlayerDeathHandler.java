package io.github.cardsandhuskers.tag.handlers;

import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.teams.objects.Team;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

import static io.github.cardsandhuskers.tag.Tag.*;

public class PlayerDeathHandler {
    Tag plugin;
    GameStageHandler gameStageHandler;
    Team[][] matchups;
    //have all runners on a team been killed
    HashMap<Team, Boolean> allEliminated;
    ArrayList<Player> aliveRunners;
    Team unopposed;
    public PlayerDeathHandler(Tag plugin, GameStageHandler gameStageHandler, ArrayList<Player> aliveRunners) {
        this.plugin = plugin;
        this.gameStageHandler = gameStageHandler;
        this.aliveRunners = aliveRunners;
        allEliminated = new HashMap<>();
    }


    /**
     *
     * @param attacked - attacked
     * @param attacker - attacker
     */
    public void onPlayerDeath(Player attacked, Player attacker) {
        if(attacker == null) {
            if(gameStageHandler.getHunter(handler.getPlayerTeam(attacked)).equals(attacked)) {
                //hunter quit, bad lol
                //System.out.println("HUNTER LEFT");

                return;
            }

            //if they're not an alive runner, do nothing
            if(!aliveRunners.contains(attacked)) return;

            //get team and opponent team
            Team t = handler.getPlayerTeam(attacked);
            Team oppTeam = null;
            for(Team[] teams:matchups) {
                if(t.equals(teams[0])) oppTeam = teams[1];
                if(t.equals(teams[1])) oppTeam = teams[0];
            }
            //get opponent hunter and make them attacker
            if(oppTeam == null) return;
            attacker = gameStageHandler.getHunter(oppTeam);
            if(attacker == null) return;

        }
        if(attacker == null) return;

        boolean hasAlivePlayers = false;
        int timePassed = gameStageHandler.getElapsedTime();
        aliveRunners.remove(attacked);
        Team attackerTeam = handler.getPlayerTeam(attacker);

        double tagPoints = plugin.getConfig().getInt("tagPoints") * multiplier;
        tagPoints -= (plugin.getConfig().getInt("tagPointsDropoff") * (Math.floor(timePassed/10))) * multiplier;

        attacker.sendMessage("[+" + ChatColor.YELLOW + "" + ChatColor.BOLD + (int)tagPoints + ChatColor.RESET + "] You Tagged " + handler.getPlayerTeam(attacked).color + attacked.getName() + ChatColor.RESET);
        handler.getPlayerTeam(attacker).addTempPoints(attacker, tagPoints);

        for(Player p:handler.getPlayerTeam(attacked).getOnlinePlayers()) {
            if(aliveRunners.contains(p)) hasAlivePlayers = true;
        }
        if(tags.containsKey(attacker)) tags.put(attacker, tags.get(attacker) + 1);
        else tags.put(attacker, 1);
        attacked.setGameMode(GameMode.SPECTATOR);


        if(!hasAlivePlayers) {
            //team finished off
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 2);

            double totalTagPoints = plugin.getConfig().getInt("allTagPoints") * multiplier;
            totalTagPoints -= (plugin.getConfig().getInt("allTagPointsDropoff") * (Math.floor(timePassed/10))) * multiplier;
            attackerTeam.addTempPoints(attacker, totalTagPoints);
            attacker.sendMessage("[+" + ChatColor.YELLOW + "" + ChatColor.BOLD + totalTagPoints + ChatColor.RESET + "] You tagged all the opponents!");

            //System.out.println(allEliminated.keySet());
            if(allEliminated.containsKey(attackerTeam) && allEliminated.get(attackerTeam)) {
                //eliminated second
                allEliminated.put(handler.getPlayerTeam(attacked), true);
                for(Player p:attackerTeam.getOnlinePlayers()) {
                    p.sendMessage("Your hunter eliminated the opposing runners second, no bonus points.");
                    if(roundWins.containsKey(p)) roundWins.put(p, roundWins.get(p) + 1);
                    else roundWins.put(p, 1);
                }

            } else {
                //eliminated first
                double playerPoints = plugin.getConfig().getInt("hunterTagFirstPoints") * multiplier / handler.getPlayerTeam(attacker).getSize();
                for(Player p:attackerTeam.getOnlinePlayers()) {
                    attackerTeam.addTempPoints(p, playerPoints);
                    p.sendMessage("[+" + ChatColor.YELLOW + "" + ChatColor.BOLD + (int)playerPoints + ChatColor.RESET + "] Your hunter eliminated the opposing runners first!");
                    if(roundWins.containsKey(p)) roundWins.put(p, roundWins.get(p) + 1);
                    else roundWins.put(p, 1);
                }

                allEliminated.put(handler.getPlayerTeam(attacked), true);
            }


        } else {
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        }

        if(aliveRunners.size() == 0) {
            gameStageHandler.roundOver();
        } else if(unopposed != null) {
            boolean exists = false;
            for(Player p: aliveRunners) {
                if(!handler.getPlayerTeam(p).equals(unopposed)) exists = true;
            }
            if(!exists) gameStageHandler.roundOver();
        }


    }

    public void setMatchups(Team[][] matchups) {
        this.matchups = matchups;
    }
    public void resetEliminations() {
        allEliminated.clear();
        for(Team t: handler.getTeams()) allEliminated.put(t, false);
    }

    /**
     * Manual override to attempt to remove a runner
     * @param p - player to remove
     */
    public void removePlayer(Player p) {
        if(aliveRunners.contains(p)) aliveRunners.remove(p);
    }
}
