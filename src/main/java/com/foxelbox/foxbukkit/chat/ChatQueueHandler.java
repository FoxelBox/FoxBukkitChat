/**
 * This file is part of FoxBukkitChat.
 *
 * FoxBukkitChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitChat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitChat.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.chat;

import com.foxelbox.foxbukkit.chat.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chat.json.ChatMessageOut;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zeromq.ZMQ;

import java.util.*;

public class ChatQueueHandler {
    private final FoxBukkitChat plugin;
    private final ZMQ.Context zmqContext = ZMQ.context(4);
    private final ZMQ.Socket sender;

    public ChatQueueHandler(FoxBukkitChat plugin) {
        sender = zmqContext.socket(ZMQ.PUSH);
        ZeroMQConfigurator.parseZeroMQConfig(
                plugin.configuration.getValue("zmq-server2link", "mdns;null"),
                sender,
                "fbchat-server2link",
                plugin.configuration.getValue("zmq-mdns-server2link", "default")
        );

        final ZMQ.Socket receiver = zmqContext.socket(ZMQ.SUB);
        ZeroMQConfigurator.parseZeroMQConfig(
                plugin.configuration.getValue("zmq-link2server", "mdns;null"),
                receiver,
                "fbchat-link2server",
                plugin.configuration.getValue("zmq-mdns-link2server", "default")
        );

        receiver.subscribe("CMO".getBytes());

        Thread t = new Thread() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    receiver.recv(); // Topic
                    onMessage(receiver.recv());
                }
            }
        };
        t.setDaemon(true);
        t.setName("ZMQ SUB");
        t.start();

        this.plugin = plugin;
    }

    public void sendMessage(final CommandSender player, final String message) {
        sendMessage(player, message, Messages.MessageType.TEXT);
    }

    public void sendMessage(final ChatMessageIn messageIn) {
        if(messageIn == null)
            throw new NullPointerException();

        sender.send(messageIn.toProtoBuf().toByteArray(), 0);
    }

    public void sendMessage(final CommandSender player, final String message, final Messages.MessageType type) {
        if(player == null || message == null)
            throw new NullPointerException();
        ChatMessageIn messageIn = new ChatMessageIn(plugin, player);
        messageIn.contents = message;
        if(type != null) {
            messageIn.type = type;
        }
        sendMessage(messageIn);
    }

    public void onMessage(final byte[] c_message) {
        try {
            final ChatMessageOut chatMessageOut = ChatMessageOut.fromProtoBuf(Messages.ChatMessageOut.parseFrom(c_message));
            onMessage(chatMessageOut);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMessage(final ChatMessageOut chatMessageOut) {
        try {
            Collection<? extends Player> allPlayers = plugin.getServer().getOnlinePlayers();
            final List<Player> targetPlayers = new ArrayList<>();
            switch(chatMessageOut.to.type) {
                case ALL:
                    targetPlayers.addAll(allPlayers);
                    break;
                case PERMISSION:
                    for(String permission : chatMessageOut.to.filter)
                        for (Player player : allPlayers)
                            if (player.hasPermission(permission) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
                case PLAYER:
                    for(String playerUUID : chatMessageOut.to.filter)
                        for (Player player : allPlayers)
                            if (player.getUniqueId().equals(UUID.fromString(playerUUID)) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
            }

            if(chatMessageOut.type == Messages.MessageType.KICK) {
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        for(Player target : targetPlayers) {
                            plugin.playerHelper.refreshPlayerListRedis(target);
                            plugin.registeredPlayers.remove(target.getUniqueId());
                            target.kickPlayer(chatMessageOut.contents);
                        }
                    }
                });
                return;
            } else if(chatMessageOut.type == Messages.MessageType.INJECT) {
                final String[] contents = chatMessageOut.contents.split("\n");
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        for (Player target : targetPlayers) {
                            for(String cmd : contents) {
                                target.chat(cmd);
                            }
                        }
                    }
                });
                return;
            } else if(chatMessageOut.type != Messages.MessageType.TEXT) {
                return;
            }

            if(chatMessageOut.from != null && chatMessageOut.from.uuid != null) {
                Set<UUID> ignoringSet = plugin.playerHelper.getIgnoredBy(chatMessageOut.from.uuid);
                if(ignoringSet != null) {
                    Iterator<Player> playerIterator = targetPlayers.iterator();
                    while(playerIterator.hasNext()) {
                        if(ignoringSet.contains(playerIterator.next().getUniqueId())) {
                            playerIterator.remove();
                        }
                    }
                }
            }

            if(targetPlayers.isEmpty()) {
                return;
            }

            if (chatMessageOut.server != null && !chatMessageOut.server.equals(plugin.configuration.getValue("server-name", "Main"))) {
                if(chatMessageOut.contents != null) {
                    chatMessageOut.contents = "<color name=\"dark_green\">[" + chatMessageOut.server + "]</color> " + chatMessageOut.contents;
                }
            }

            if(chatMessageOut.contents != null) {
                HTMLParser.sendToPlayers(plugin, targetPlayers, chatMessageOut.contents);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
