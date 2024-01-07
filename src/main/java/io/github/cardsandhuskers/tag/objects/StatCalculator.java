package io.github.cardsandhuskers.tag.objects;

import io.github.cardsandhuskers.tag.Tag;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bukkit.Bukkit;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class StatCalculator {
    private final Tag plugin;
    private ArrayList<PlayerStatsHolder> playerStatsHolders;
    private ArrayList<EventStatsHolder> eventStatsHolders;
    int currentEvent;

    public StatCalculator(Tag plugin) {
        this.plugin = plugin;
        try {currentEvent = Bukkit.getPluginManager().getPlugin("LobbyPlugin").getConfig().getInt("eventNum");}
        catch (Exception e) {currentEvent = 1;}

    }

    public void calculateStats() throws Exception {

        HashMap<String, PlayerStatsHolder> playerStatsMap = new HashMap<>();
        FileReader reader;
        //run through kills
        for(int i = 1; i <= currentEvent; i++) {
            try {
                reader = new FileReader(plugin.getDataFolder() + "/" + "tagKillStats" + i + ".csv");
            } catch (IOException e) {
                plugin.getLogger().warning("Stats file not found!");
                continue;
            }
            String[] headers = {"Round", "Player", "Team", "HunterName", "hunterTeam", "timeOfDeath"};

            CSVFormat.Builder builder = CSVFormat.Builder.create();
            builder.setHeader(headers);
            CSVFormat format = builder.build();

            CSVParser parser;
            parser = new CSVParser(reader, format);

            List<CSVRecord> recordList = parser.getRecords();
            reader.close();

            for(CSVRecord r:recordList) {
                if (r.getRecordNumber() == 1) continue;

                String died = r.get(1);
                String killer = r.get(3);

                if (playerStatsMap.containsKey(killer)) {
                    playerStatsMap.get(killer).addKill(i, died);

                } else {
                    PlayerStatsHolder statsHolder = new PlayerStatsHolder(killer);
                    statsHolder.addKill(i, died);
                    playerStatsMap.put(killer, statsHolder);
                }

                if (playerStatsMap.containsKey(died)) {
                    playerStatsMap.get(died).addDeath(i, killer);
                } else {
                    PlayerStatsHolder statsHolder = new PlayerStatsHolder(died);
                    statsHolder.addDeath(i, killer);
                    playerStatsMap.put(died, statsHolder);
                }
            }
        }

        for(int i = 1; i <= currentEvent; i++) {
            try {
                reader = new FileReader(plugin.getDataFolder() + "/" + "tagWinStats" + i + ".csv");
            } catch (IOException e) {
                plugin.getLogger().warning("Stats file not found!");
                continue;
            }
            String[] headers = {"Round", "winningPlayer", "winningTeam", "losingTeam"};

            CSVFormat.Builder builder = CSVFormat.Builder.create();
            builder.setHeader(headers);
            CSVFormat format = builder.build();

            CSVParser parser;
            parser = new CSVParser(reader, format);

            List<CSVRecord> recordList = parser.getRecords();
            reader.close();

            for(CSVRecord r:recordList) {
                if (r.getRecordNumber() == 1) continue;

                String winner = r.get(1);
                String loser = r.get(3);

                if (playerStatsMap.containsKey(winner)) {
                    playerStatsMap.get(winner).addWin(i, loser);

                } else {
                    PlayerStatsHolder statsHolder = new PlayerStatsHolder(winner);
                    statsHolder.addWin(i, loser);
                    playerStatsMap.put(winner, statsHolder);
                }
            }
        }

        playerStatsHolders = new ArrayList<>(playerStatsMap.values());

        eventStatsHolders = new ArrayList<>();
        for(PlayerStatsHolder psh: playerStatsHolders) eventStatsHolders.addAll(psh.getEventKills());


    }

    public PlayerStatsHolder getStatsHolder(PlayerStatsComparator.SortType sortType, int place) {
        ArrayList<PlayerStatsHolder> psh = new ArrayList<>(playerStatsHolders);
        psh.sort(new PlayerStatsComparator(sortType));
        Collections.reverse(psh);
        if(place > psh.size()) return null;
        else return  psh.get(place-1);
    }

    public EventStatsHolder getEventStatsHolder(PlayerStatsComparator.SortType sortType, int place) {
        ArrayList<EventStatsHolder> psh = new ArrayList<>(eventStatsHolders);
        psh.sort(new EventStatsComparator(sortType));
        Collections.reverse(psh);
        if(place > psh.size()) return null;
        else return  psh.get(place-1);
    }

    public class PlayerStatsHolder {
        HashMap<Integer, ArrayList<String>> kills, deaths, wins;
        String name;
        public PlayerStatsHolder(String name) {
            this.name = name;
            kills = new HashMap<>();
            deaths = new HashMap<>();
            wins = new HashMap<>();
        }

        public void addKill(int event, String killedPlayer) {
            if(kills.containsKey(event)) kills.get(event).add(killedPlayer);
            else {
                ArrayList<String> newKills = new ArrayList<>();
                newKills.add(killedPlayer);
                kills.put(event, newKills);
            }
        }

        public void addDeath(int event, String killer) {
            if(deaths.containsKey(event)) deaths.get(event).add(killer);
            else {
                ArrayList<String> newDeaths = new ArrayList<>();
                newDeaths.add(killer);
                deaths.put(event, newDeaths);
            }
        }

        public void addWin(int event, String opponent) {
            if(wins.containsKey(event)) wins.get(event).add(opponent);
            else {
                ArrayList<String> newDeaths = new ArrayList<>();
                newDeaths.add(opponent);
                wins.put(event, newDeaths);
            }
        }

        public ArrayList<EventStatsHolder> getEventKills() {
            ArrayList<EventStatsHolder> eventStats = new ArrayList<>();
            for (int i = 1; i <= currentEvent; i++) {
                int eventWins = 0;
                int eventKills = 0;
                boolean exists = false;

                if (kills.containsKey(i)) {
                    eventKills = kills.get(i).size();
                    exists = true;
                }
                if (wins.containsKey(i)) {
                    eventWins = wins.get(i).size();
                    exists = true;
                }
                if(exists) {
                    eventStats.add(new EventStatsHolder(name, i, eventKills, eventWins));
                }
            }
            return eventStats;
        }


        public int getKills() {
            int sum = 0;
            for(Integer event: kills.keySet()) {
                sum += kills.get(event).size();
            }
            return sum;
        }
        public int getDeaths() {
            int sum = 0;
            for(Integer event: deaths.keySet()) {
                sum += deaths.get(event).size();
            }
            return sum;
        }
        public int getWins() {
            int sum = 0;
            for(Integer event: wins.keySet()) {
                sum += wins.get(event).size();
            }
            return sum;
        }
    }

    class PlayerStatsComparator implements Comparator<PlayerStatsHolder> {
        public SortType sortType;
        public PlayerStatsComparator(SortType sortType) {
            this.sortType = sortType;
        }
        public int compare(PlayerStatsHolder h1, PlayerStatsHolder h2) {
            if(sortType == SortType.KILLS) {
                int compare = Integer.compare(h1.getKills(), h2.getKills());
                if(compare == 0) h1.name.compareTo(h2.name);
                return  compare;
            } else {
                int compare = Integer.compare(h1.getWins(), h2.getWins());
                if(compare == 0) compare = h1.name.compareTo(h2.name);
                return  compare;
            }
        }

        enum SortType {
            KILLS,
            WINS
        }
    }

    public class EventStatsHolder {
        String name;
        int eventNum, kills, wins;
        public EventStatsHolder(String name, int eventNum, int kills, int wins) {
            this.name = name;
            this.eventNum = eventNum;
            this.kills = kills;
            this.wins = wins;
        }
    }

    class EventStatsComparator implements Comparator<EventStatsHolder> {
        public PlayerStatsComparator.SortType sortType;

        public EventStatsComparator(PlayerStatsComparator.SortType sortType) {
            this.sortType = sortType;
        }
        public int compare(EventStatsHolder kh1, EventStatsHolder kh2) {

            if(sortType == PlayerStatsComparator.SortType.KILLS) {
                int compare = Integer.compare(kh1.kills, kh2.kills);
                if(compare == 0) kh1.name.compareTo(kh2.name);
                if(compare == 0) compare = 1;
                return  compare;
            } else {
                int compare = Integer.compare(kh1.wins, kh2.wins);
                if(compare == 0) compare = kh1.name.compareTo(kh2.name);
                if(compare == 0) compare = 1;
                return  compare;
            }
        }
    }
}
