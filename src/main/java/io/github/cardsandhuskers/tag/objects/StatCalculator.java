package io.github.cardsandhuskers.tag.objects;

import io.github.cardsandhuskers.tag.Tag;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static io.github.cardsandhuskers.tag.Tag.roundWins;
import static io.github.cardsandhuskers.tag.Tag.tags;
import static io.github.cardsandhuskers.teams.Teams.handler;

public class StatCalculator {
    private final Tag plugin;
    private ArrayList<PlayerStatsHolder> playerStatsHolders;
    private ArrayList<SingleGameKillsHolder> sgKillsHolders;


    public StatCalculator(Tag plugin) {
        this.plugin = plugin;

    }

    public void calculateStats() throws Exception {

        HashMap<String, PlayerStatsHolder> playerStatsMap = new HashMap<>();
        sgKillsHolders = new ArrayList<>();
        playerStatsHolders = new ArrayList<>();


        FileReader reader = null;
        try {
            reader = new FileReader(plugin.getDataFolder() + "/stats.csv");
        } catch (IOException e) {
            plugin.getLogger().warning("Stats file not found!");
            return;
        }

        String[] headers = {"Event", "Team", "Name", "Kills", "Wins"};
        CSVFormat.Builder builder = CSVFormat.Builder.create();
        builder.setHeader(headers);
        CSVFormat format = builder.build();

        CSVParser parser;
        try {
            parser = new CSVParser(reader, format);
        } catch (IOException e) {
            throw new Exception(e);
        }
        List<CSVRecord> recordList = parser.getRecords();

        try {
            reader.close();
        } catch (IOException e) {
            throw new Exception(e);
        }

        //maps records to each event number
        HashMap<Integer, ArrayList<CSVRecord>> recordsMap = new HashMap<>();
        int totalEvents = 0;
        for (CSVRecord r : recordList) {
            //skip header
            if (r.getRecordNumber() == 1) continue;

            //have totalEvents be value of "last event"
            totalEvents = Math.max(totalEvents, Integer.parseInt(r.get(0)));

            //if event number isn't in map, put new arraylist in spot at map
            if(!recordsMap.containsKey(Integer.valueOf(r.get(0)))) recordsMap.put(Integer.valueOf(r.get(0)), new ArrayList<>());
            //append to arraylist
            recordsMap.get(Integer.valueOf(r.get(0))).add(r);
        }
        for(int i = 1; i <= totalEvents; i++) {
            if(!recordsMap.containsKey(i)) continue;
            for(CSVRecord r:recordsMap.get(i)) {
                String name = r.get(2);
                if(playerStatsMap.containsKey(name)) {
                    PlayerStatsHolder h = playerStatsMap.get(name);
                    h.kills += Integer.parseInt(r.get(3));
                    h.wins += Integer.parseInt(r.get(4));
                } else {
                    PlayerStatsHolder h = new PlayerStatsHolder(name);
                    h.kills += Integer.parseInt(r.get(3));
                    h.wins += Integer.parseInt(r.get(4));
                    playerStatsMap.put(name, h);
                }
                SingleGameKillsHolder kh = new SingleGameKillsHolder();
                kh.name = name;
                kh.kills = Integer.parseInt(r.get(3));
                kh.eventNum = Integer.parseInt(r.get(0));
                sgKillsHolders.add(kh);
            }
        }
        playerStatsHolders = new ArrayList<>(playerStatsMap.values());

    }

    public void saveRecords() throws IOException {

        FileWriter writer = new FileWriter("plugins/Tag/stats.csv", true);
        FileReader reader = new FileReader("plugins/Tag/stats.csv");

        String[] headers = {"Event", "Team", "Name", "Kills", "Wins"};

        CSVFormat.Builder builder = CSVFormat.Builder.create();
        builder.setHeader(headers);
        CSVFormat format = builder.build();

        CSVParser parser = new CSVParser(reader, format);

        if(!parser.getRecords().isEmpty()) {
            format = CSVFormat.DEFAULT;
        }

        CSVPrinter printer = new CSVPrinter(writer, format);

        int eventNum;
        try {eventNum = Bukkit.getPluginManager().getPlugin("LobbyPlugin").getConfig().getInt("eventNum");} catch (Exception e) {eventNum = 1;}
        //printer.printRecord(currentGame);
        for(Player p:Bukkit.getOnlinePlayers()) {
            if(p == null) continue;
            if(handler.getPlayerTeam(p) == null) continue;

            int tagNum = 0;
            if(tags.containsKey(p)) tagNum = tags.get(p);

            int wins = 0;
            if(roundWins.containsKey(p)) wins = roundWins.get(p);

            printer.printRecord(eventNum, handler.getPlayerTeam(p).getTeamName(), p.getDisplayName(), tagNum, wins);
        }
        writer.close();
        try {
            plugin.statCalculator.calculateStats();
        } catch (Exception e) {
            StackTraceElement[] trace = e.getStackTrace();
            String str = "";
            for(StackTraceElement element:trace) str += element.toString() + "\n";
            plugin.getLogger().severe("ERROR Calculating Stats!\n" + str);
        }

    }


    public ArrayList<PlayerStatsHolder> getStatsHolders(PlayerStatsComparator.SortType sortType) {
        ArrayList<PlayerStatsHolder> psh = new ArrayList<>(playerStatsHolders);

        Comparator PlayerStatsCompare = new PlayerStatsComparator(sortType);
        psh.sort(PlayerStatsCompare);
        Collections.reverse(psh);
        return psh;
    }

    public ArrayList<SingleGameKillsHolder> getSGKillsHolders() {
        ArrayList<SingleGameKillsHolder> sgkh = new ArrayList<>(sgKillsHolders);

        Comparator SGKHComparator = new SGKHComparator();
        sgkh.sort(SGKHComparator);
        Collections.reverse(sgkh);
        return sgkh;
    }



    public class PlayerStatsHolder {
        int kills = 0;
        int wins = 0;
        String name;
        public PlayerStatsHolder(String name) {
            this.name = name;
        }
    }

    class PlayerStatsComparator implements Comparator<PlayerStatsHolder> {
        public SortType sortType;
        public PlayerStatsComparator(SortType sortType) {
            this.sortType = sortType;
        }
        public int compare(PlayerStatsHolder h1, PlayerStatsHolder h2) {
            if(sortType == SortType.KILLS) {
                int compare = Integer.compare(h1.kills, h2.kills);
                if(compare == 0) h1.name.compareTo(h2.name);
                if(compare == 0) compare = 1;
                return  compare;
            } else {
                int compare = Integer.compare(h1.wins, h2.wins);
                if(compare == 0) compare = h1.name.compareTo(h2.name);
                if(compare == 0) compare = 1;
                return  compare;
            }
        }

        enum SortType {
            KILLS,
            WINS
        }
    }

    public class SingleGameKillsHolder {
        String name;
        int eventNum;
        int kills;
    }

    class SGKHComparator implements Comparator<SingleGameKillsHolder> {
        public int compare(SingleGameKillsHolder kh1, SingleGameKillsHolder kh2) {
            int compare = Integer.compare(kh1.kills, kh2.kills);
            if(compare == 0) compare = kh1.name.compareTo(kh2.name);
            if(compare == 0) compare = 1;
            return compare;
        }
    }
}
