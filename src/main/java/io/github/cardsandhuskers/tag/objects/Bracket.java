package io.github.cardsandhuskers.tag.objects;

import io.github.cardsandhuskers.teams.objects.Team;

import java.util.ArrayList;

public class Bracket {

    public Bracket() {

    }



    public Team[][] getMatchups(ArrayList<Team> teamArray, int selectedRound) {
        /*
        ArrayList<String> ListTeam = new ArrayList<String>();
        for(Team t:teamArray) {
            ListTeam.add(t.getTeamName());
        }
        */

        if (teamArray.size() % 2 != 0)
        {
            teamArray.add(new Team("DUMMYTEAM")); // If odd number of teams add a dummy
        }

        int numTeams = teamArray.size();

        int numrounds = (numTeams - 1); // rounds needed to complete tournament
        int halfSize = numTeams / 2;

        ArrayList<Team> teams = new ArrayList<>();

        // Add teams to List and remove the first team
        for(Team t:teamArray) {
            teams.add(t);
        }
        teams.remove(0);

        int teamsSize = teams.size();

        Team[][] selectedMatchups = new Team[numTeams/2][2];

        //iterate through all the rounds
        for (int round = 0; round < numrounds; round++) {
            //System.out.println("round {" + Integer.toString(round + 1) + "}");

            int teamIdx = round % teamsSize;

            //System.out.println("{" + teams.get(teamIdx) + "} vs {" + teamArray.get(0) + "}");
            if(round + 1 == selectedRound) {
                selectedMatchups[0][0] = teams.get(teamIdx);
                selectedMatchups[0][1] = teamArray.get(0);
            }

            for (int idx = 1; idx < halfSize; idx++) {
                int firstTeam = (round + idx) % teamsSize;
                int secondTeam = (round  + teamsSize - idx) % teamsSize;
                //System.out.println("{" + teams.get(firstTeam).getTeamName() + "} vs {" + teams.get(secondTeam).getTeamName() + "}");
                if(round + 1 == selectedRound) {
                    selectedMatchups[idx][0] = teams.get(firstTeam);
                    selectedMatchups[idx][1] = teams.get(secondTeam);
                }
            }

        }
        return selectedMatchups;
    }
}
