package io.github.cardsandhuskers.tag.handlers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.teams.objects.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static io.github.cardsandhuskers.tag.Tag.handler;

public class GlowHandler {
    ArrayList<Player> aliveRunners;
    HashMap<Player, Boolean> canRunnerVision;
    Tag plugin;
    HashMap<Team, Player> currentHunters;
    ArrayList<RunnerVision> activeRunnerVisions;
    GameStageHandler gameStageHandler;

    public GlowHandler(Tag plugin, ArrayList<Player> aliveRunners, Team[][] matchups, HashMap<Team, Player> currentHunters, GameStageHandler gameStageHandler) {
        this.plugin = plugin;
        this.aliveRunners = aliveRunners;
        this.currentHunters = currentHunters;
        this.gameStageHandler = gameStageHandler;
    }

    public void enableGlow() {
        canRunnerVision = new HashMap<>();
        for(Player p:aliveRunners) {
            p.setGlowing(true);
            canRunnerVision.put(p, true);
        }
        activeRunnerVisions = new ArrayList<>();
    }

    public void disableGlow() {
        for(Player p: Bukkit.getOnlinePlayers()) {
            p.setGlowing(false);
        }
        ArrayList<RunnerVision> tempRunnerVisions = new ArrayList<>(activeRunnerVisions);
        for(RunnerVision r:tempRunnerVisions) {
            r.cancelOperation();
        }
        activeRunnerVisions.clear();
    }

    public void triggerRunnerVision(Player p) {
        if(!aliveRunners.contains(p)) return;

        if(canRunnerVision.containsKey(p)) {
            if (canRunnerVision.get(p)) {
                Team playerTeam = handler.getPlayerTeam(p);
                Team hunterTeam = null;

                for (Team[] matchup : gameStageHandler.getMatchups()) {
                    if (matchup[0].equals(playerTeam)) hunterTeam = matchup[1];
                    if (matchup[1].equals(playerTeam)) hunterTeam = matchup[0];
                }

                if (hunterTeam == null) return;

                RunnerVision runnerVision = new RunnerVision(p, currentHunters.get(hunterTeam));
                runnerVision.startOperation();
                giveRunnerVision(p, Material.ENDER_EYE);

                canRunnerVision.put(p, false);
            }

        } else {
            canRunnerVision.put(p, true);
            triggerRunnerVision(p);
        }
    }
    public void takeRunnerVision(Player p) {
        p.getInventory().setItem(4, new ItemStack(Material.AIR));
    }

    public void giveRunnerVision(Player p, Material mat) {
        ItemStack runnerVision = new ItemStack(mat);
        ItemMeta runnerVisionMeta = runnerVision.getItemMeta();
        runnerVisionMeta.setDisplayName("Runner Vision");
        runnerVisionMeta.setLore(Collections.singletonList("Click this to highlight the hunter for 3 seconds."));
        runnerVision.setItemMeta(runnerVisionMeta);
        p.getInventory().setItem(4, runnerVision);

    }


    class RunnerVision implements Runnable {

        private Integer assignedTaskId, timeElapsed = 0;
        private final Player runner;
        private final Player hunter;
        private boolean expired = false;

        public RunnerVision(Player runner, Player hunter) {
            this.runner = runner;
            this.hunter = hunter;
            activeRunnerVisions.add(this);
        }

        @Override
        public void run() {
            timeElapsed++;

            if(timeElapsed < 60) {
                sendFakePacket(hunter, runner, (byte)0x40);
            }
            else if(timeElapsed == 60) {
                //disable runner vision
                sendFakePacket(hunter, runner, (byte)0x0);
                giveRunnerVision(runner, Material.ENDER_PEARL);

                expired = true;
            }

            if(timeElapsed <= 200) {
                String s = ChatColor.YELLOW + "Reloading: ";
                //█
                for(int i = 0; i < timeElapsed / 20; i++) s+= "█";
                s+= ChatColor.BLACK;
                for(int i = timeElapsed / 20; i < 10; i++) s+= "█";

                runner.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(s));
            }

            if(timeElapsed >= 200 && expired) {
                runner.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                //return item
                runner.playSound(runner.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
                canRunnerVision.put(runner, true);

                cancelOperation();
                activeRunnerVisions.remove(this);
            }
        }
        /**
         * Stop the repeating task
         */
        public void cancelOperation() {
            if (assignedTaskId != null) Bukkit.getScheduler().cancelTask(assignedTaskId);
        }


        /**
         * Schedules this instance to "run" every second
         */
        public void startOperation() {
            // Initialize our assigned task's id, for later use so we can cancel
            this.assignedTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 1L);
        }

    }

    public void sendFakePacket(Player target, Player recipient, byte type) {
        var protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId()); //Set packet's entity id
        WrappedDataWatcher watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class); //Found this through google, needed for some stupid reason
        watcher.setEntity(target); //Set the new data watcher's target
        packet.getDataValueCollectionModifier().write(0, List.of(
                new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), type)
        ));

        try {
            protocolManager.sendServerPacket(recipient, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
