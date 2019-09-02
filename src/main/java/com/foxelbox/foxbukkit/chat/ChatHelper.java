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

import com.foxelbox.foxbukkit.chat.json.*;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ChatHelper {
    private final FoxBukkitChat plugin;

    public ChatHelper(FoxBukkitChat plugin) {
        this.plugin = plugin;
    }

    public void sendMessageTo(final CommandSender player, final String message) {
        if(player == null || message == null) {
            throw new NullPointerException();
        }

        UserInfo user = new UserInfo(plugin, player);
        ChatMessageOut msgOut = new ChatMessageOut(plugin, user);
        msgOut.contents = message;
        msgOut.to = new MessageTarget(TargetType.PLAYER, new String[] { user.uuid.toString() });
        msgOut.type = MessageType.TEXT;
        sendMessage(msgOut);
    }

    public void sendMessage(final ChatMessageOut chatMessageOut) {
        try {
            final Set<Player> targetPlayers = new HashSet<>();
            boolean sendToConsole = false;
            switch(chatMessageOut.to.type) {
                case ALL:
                    targetPlayers.addAll(plugin.getServer().getOnlinePlayers());
                    sendToConsole = true;
                    break;
                case PERMISSION:
                    sendToConsole = true;
                    for(String permission : chatMessageOut.to.filter)
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            if (player.hasPermission(permission)) {
                                targetPlayers.add(player);
                            }
                        }
                    break;
                case PLAYER:
                    final Server srv = plugin.getServer();
                    for(String playerUUID : chatMessageOut.to.filter) {
                        if (playerUUID.equals(Utils.CONSOLE_UUID.toString())) {
                            sendToConsole = true;
                            continue;
                        }

                        final UUID uuid = UUID.fromString(playerUUID);
                        final Player ply = srv.getPlayer(uuid);
                        if (ply != null && ply.isOnline()) {
                            targetPlayers.add(ply);
                        }
                    }
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

            if(targetPlayers.isEmpty() && !sendToConsole) {
                return;
            }

            if(chatMessageOut.contents != null && !chatMessageOut.contents.isEmpty()) {
                if (chatMessageOut.server != null && !chatMessageOut.server.isEmpty() && !chatMessageOut.server.equals(plugin.configuration.getValue("server-name", "Main"))) {
                    chatMessageOut.contents = "<color name=\"dark_green\">[" + chatMessageOut.server + "]</color> " + chatMessageOut.contents;
                }
                final Set<CommandSender> targets = new HashSet<>(targetPlayers);
                if (sendToConsole) {
                    targets.add(plugin.getServer().getConsoleSender());
                }
                HTMLParser.sendToPlayers(plugin, targets, chatMessageOut.contents);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

