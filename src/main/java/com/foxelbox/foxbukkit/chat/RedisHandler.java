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

import com.foxelbox.dependencies.redis.AbstractRedisHandler;
import com.foxelbox.foxbukkit.chat.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chat.json.ChatMessageOut;
import com.google.gson.Gson;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class RedisHandler extends AbstractRedisHandler {
    private final FoxBukkitChat plugin;
    public RedisHandler(FoxBukkitChat plugin) {
        super(plugin.redisManager, "foxbukkit:to_server");
        this.plugin = plugin;
    }

    public void sendMessage(final CommandSender player, final String message) {
        sendMessage(player, message, "text");
    }

    public void sendMessage(final ChatMessageIn messageIn) {
        if(messageIn == null)
            throw new NullPointerException();
        final String messageJSON;
        synchronized (gson) {
            messageJSON = gson.toJson(messageIn);
        }
        plugin.redisManager.lpush("foxbukkit:from_server", messageJSON);
    }

    public void sendMessage(final CommandSender player, final String message, final String type) {
        if(player == null || message == null)
            throw new NullPointerException();
        ChatMessageIn messageIn = new ChatMessageIn(plugin, player);
        messageIn.contents = message;
        if(type != null) {
            messageIn.type = type;
        }
        sendMessage(messageIn);
    }

    private static final Gson gson = new Gson();

    @Override
    public void onMessage(final String c_message) {
        try {
            final ChatMessageOut chatMessageOut;
            synchronized (gson) {
                chatMessageOut = gson.fromJson(c_message, ChatMessageOut.class);
            }

            onMessage(chatMessageOut);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMessage(final ChatMessageOut chatMessageOut) {
        try {
            if(chatMessageOut.type.equals("kick")) {
                final UUID target = UUID.fromString(chatMessageOut.to.filter[0]);
                final Player ply = plugin.getServer().getPlayer(target);
                if (ply != null) {
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.registeredPlayers.remove(target);
                            ply.kickPlayer(chatMessageOut.contents);
                        }
                    });
                }
                return;
            }  else if(!chatMessageOut.type.equals("text")) {
                return;
            }

            Collection<? extends Player> allPlayers = plugin.getServer().getOnlinePlayers();
            List<Player> targetPlayers = new ArrayList<>();
            switch(chatMessageOut.to.type) {
                case "all":
                    targetPlayers = new ArrayList<>(allPlayers);
                    break;
                case "permission":
                    for(String permission : chatMessageOut.to.filter)
                        for (Player player : allPlayers)
                            if (player.hasPermission(permission) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
                case "player":
                    for(String playerUUID : chatMessageOut.to.filter)
                        for (Player player : allPlayers)
                            if (player.getUniqueId().equals(UUID.fromString(playerUUID)) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
            }

            if(chatMessageOut.from.uuid != null) {
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
