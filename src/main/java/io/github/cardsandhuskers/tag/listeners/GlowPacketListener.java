package io.github.cardsandhuskers.tag.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.cardsandhuskers.tag.Tag;
import io.github.cardsandhuskers.tag.handlers.GameStageHandler;
import io.github.cardsandhuskers.teams.objects.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.github.cardsandhuskers.tag.Tag.handler;

/**
 * @deprecated
 * Class that handles glowing through packets so that only specific players will be glowing for others
 */
public class GlowPacketListener {
    private final Tag plugin;
    private PacketAdapter packetListener;
    ArrayList<Player> aliveRunners;
    HashMap<Team, Player> currentHunters;
    GameStageHandler gameStageHandler;

    public GlowPacketListener(Tag plugin, ArrayList<Player> aliveRunners, HashMap<Team, Player> currentHunters, GameStageHandler gameStageHandler) {
        this.plugin = plugin;
        this.aliveRunners = aliveRunners;
        this.currentHunters = currentHunters;
        this.gameStageHandler = gameStageHandler;
    }

    /**
     * Handles the logic for what players are meant to glow for the player
     * @param player player receiving the packet
     * @return Arraylist - players that glow for the param player
     */
    private ArrayList<Player> getGlows(Player player) {
        ArrayList<Player> isGlowing = new ArrayList<>();
        Team playerTeam = handler.getPlayerTeam(player);
        if(playerTeam == null) return new ArrayList<>();

        Team[][] matchups = gameStageHandler.getMatchups();
        //System.out.println(currentHunters);
        //player is hunter
        if(currentHunters.containsValue(player)) {
            //System.out.println(player.getDisplayName() + " is hunter");

            for (int i = 0; i < matchups.length; i++) {
                if (matchups[i][0] == playerTeam) {
                    Team hiderTeam = matchups[i][1];
                    for (Player p : aliveRunners) {
                        if (handler.getPlayerTeam(p) == hiderTeam) isGlowing.add(p);
                    }
                }
                if (matchups[i][1] == playerTeam) {
                    Team hiderTeam = matchups[i][0];
                    for (Player p : aliveRunners) {
                        if (handler.getPlayerTeam(p) == hiderTeam) isGlowing.add(p);
                    }
                }
            }
            return isGlowing;
        } else {//player is runner
            for(Player p: playerTeam.getOnlinePlayers()) {
                if(aliveRunners.contains(p)) isGlowing.add(p);
            }
            return isGlowing;
        }
    }

    /**
     * Sets correct players as glowing and enables the packet listener that will keep them glowing
     */
    public void enableGlow() {

        var protocolManager = ProtocolLibrary.getProtocolManager();

        //sends fake packet to trigger glow for players
        for(Player player: currentHunters.values()) {

            ArrayList<Player> isGlowing = getGlows(player);

            for(Player pl:isGlowing) {
                sendFakePacket(pl, player, (byte) 0x40);
            }
        }

        //create and register a new packet listener
        packetListener = new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            @Override
            public void onPacketSending(PacketEvent event) {
                //event.getPlayer() is the player being sent the packet

                try {
                    ArrayList<Player> isGlowing = getGlows(event.getPlayer());
                    //System.out.println(event.getPlayer().getDisplayName() + ": " + isGlowing + "\n\n");
                    //for each player the recipient of the packet should see glowing
                    boolean found = false;
                    //System.out.println(event.getPlayer().getDisplayName() + isGlowing);

                    //check each player that should be glowing to see if the packet is for that player
                    for (Player player : isGlowing) {
                        //entityID is a unique identifier for each entity

                        //check if player is in the packet
                        if (player.getEntityId() == event.getPacket().getIntegers().read(0)) {
                            found = true;
                            //entityMetadata and EntitySpawn packets work differently
                            if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                                List<WrappedDataValue> dataValues = event.getPacket().getDataValueCollectionModifier().read(0);
                                if(dataValues.isEmpty()) return;
                                for(WrappedDataValue metadata: dataValues) {
                                    if(metadata.getIndex() == 0) {
                                        byte b = (byte) metadata.getValue();
                                        b |= 0b01000000;
                                        metadata.setValue(b);

                                        System.out.println("byte changed to " + b + " so " + player.getDisplayName() + " is glowing for " + event.getPlayer().getDisplayName());
                                    }
                                }

                            } else {
                                /*WrappedDataWatcher watcher = event.getPacket().getDataWatcherModifier().read(0);
                                if (watcher.hasIndex(0)) {
                                    byte b = watcher.getByte(0);
                                    b |= 0b01000000;
                                    watcher.setObject(0, b);
                                }*/
                            }
                            break;
                        }
                    }

                    //if the player isn't in packet, they're not meant to be glowing and make sure to send the packet without a glow
                    if (!found) {
                        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                            List<WrappedDataValue> watchableObjectList = event.getPacket().getDataValueCollectionModifier().read(0);
                            if (watchableObjectList.isEmpty()) return;
                            for (WrappedDataValue metadata : watchableObjectList) {
                                if (metadata.getIndex() == 0) {
                                    byte b = (byte) metadata.getValue();
                                    b &= ~(1 << 6);
                                    metadata.setValue(b);
                                }
                            }
                        } else {
                            //WrappedDataWatcher watcher = event.getPacket().getDataWatcherModifier().read(0);

                            /*if (watcher.hasIndex(0)) {
                                byte b = watcher.getByte(0);
                                b &= ~(1 << 6);
                                watcher.setObject(0, b);
                            }*/
                        }
                        System.out.println(event.getPlayer().getDisplayName() + " received packet for player " + event.getPacket().getIntegers().read(0) + " who wasn't glowing");
                        String ids = "";
                        for(Player p: Bukkit.getOnlinePlayers()) ids += p.getEntityId() + " is " + p.getDisplayName() + " ;; ";
                        System.out.println(ids);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        protocolManager.addPacketListener(packetListener);


    }

    /**
     * Stop all players glowing by disabling the packet listener
     */
    public void disableGlow() {
        if(packetListener != null) {
            try { ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);}catch (Exception e){e.printStackTrace();}
        }

        for(Player player: currentHunters.values()) {

            ArrayList<Player> isGlowing = getGlows(player);

            for(Player pl:isGlowing) {
                sendFakePacket(pl, player, (byte) 0x0);
            }
        }

    }

    public void sendFakePacket(Player target, Player recipient, byte type) {
        var protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId()); //Set packet's entity id
        WrappedDataWatcher watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class); //Found this through google, needed for some stupid reason
        watcher.setEntity(target); //Set the new data watcher's target
        //watcher.setObject(0, serializer, (byte) (0x40)); //Set status to glowing, found on protocol page
        //packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects()); //Make the packet's datawatcher the one we created
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
