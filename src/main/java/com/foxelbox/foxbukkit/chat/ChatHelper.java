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
import com.foxelbox.foxbukkit.chat.json.MessageType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ChatHelper {
    private final FoxBukkitChat plugin;

    public ChatHelper(FoxBukkitChat plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(final CommandSender player, final String message) {
        sendMessage(player, message, MessageType.TEXT);
    }

    public void sendMessage(final ChatMessageIn messageIn) {
        if(messageIn == null) {
            throw new NullPointerException();
        }

        sendMessage(new ChatMessageOut(messageIn));
    }

    public void sendMessage(final CommandSender player, final String message, final MessageType type) {
        if(player == null || message == null)
            throw new NullPointerException();
        ChatMessageIn messageIn = new ChatMessageIn(plugin, player);
        messageIn.contents = message;
        if(type != null) {
            messageIn.type = type;
        }
        sendMessage(messageIn);
    }

    public void sendMessage(final ChatMessageOut chatMessageOut) {
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

            if(chatMessageOut.type == MessageType.KICK) {
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    for(Player target : targetPlayers) {
                        plugin.registeredPlayers.remove(target.getUniqueId());
                        target.kickPlayer(chatMessageOut.contents);
                    }
                });
                return;
            } else if(chatMessageOut.type == MessageType.INJECT) {
                final String[] contents = chatMessageOut.contents.split("\n");
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    for (Player target : targetPlayers) {
                        for(String cmd : contents) {
                            target.chat(cmd);
                        }
                    }
                });
                return;
            } else if(chatMessageOut.type != MessageType.TEXT) {
                return;
            }

            if(chatMessageOut.from != null && chatMessageOut.from.uuid != null) {
                targetPlayers.removeIf(player -> plugin.playerHelper.getIgnore(player.getUniqueId()).contains(chatMessageOut.from.uuid));
            }

            if(targetPlayers.isEmpty()) {
                return;
            }

            if(chatMessageOut.contents != null && !chatMessageOut.contents.isEmpty()) {
                if (chatMessageOut.server != null && !chatMessageOut.server.isEmpty() && !chatMessageOut.server.equals(plugin.configuration.getValue("server-name", "Main"))) {
                    chatMessageOut.contents = "<color name=\"dark_green\">[" + chatMessageOut.server + "]</color> " + chatMessageOut.contents;
                }
                HTMLParser.sendToPlayers(plugin, targetPlayers, chatMessageOut.contents);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

