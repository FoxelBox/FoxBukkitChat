/**
 * This file is part of FoxBukkitChatComponent.
 *
 * FoxBukkitChatComponent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitChatComponent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitChatComponent.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.chatcomponent;

import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageOut;
import com.google.gson.Gson;
import com.foxelbox.dependencies.redis.AbstractRedisHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RedisHandler extends AbstractRedisHandler {
    public RedisHandler() {
        super(FBChatComponent.instance.redisManager, "foxbukkit:to_server");
    }

    public static final UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("COMMANDSENDER:CONSOLE".getBytes());

    private static final Gson gson = new Gson();

    public static void sendMessage(final CommandSender player, final String message) {
        sendMessage(player, message, "text");
    }

    public static void sendMessage(final CommandSender player, final String message, final String type) {
        if(player == null || message == null)
            throw new NullPointerException();
        ChatMessageIn messageIn = new ChatMessageIn(player);
        messageIn.contents = message;
        messageIn.type = type;
        final String messageJSON;
        synchronized (gson) {
            messageJSON = gson.toJson(messageIn);
        }
        FBChatComponent.instance.redisManager.lpush("foxbukkit:from_server", messageJSON);
    }

	@Override
	public void onMessage(final String c_message) {
		try {
            final ChatMessageOut chatMessageOut;
            synchronized (gson) {
                chatMessageOut = gson.fromJson(c_message, ChatMessageOut.class);
            }

            if (!chatMessageOut.server.equals(FBChatComponent.instance.configuration.getValue("server-name", "Main"))) {
                chatMessageOut.contents.plain = "\u00a72[" + chatMessageOut.server + "]\u00a7f " + chatMessageOut.contents.plain;
            }

            List<Player> allPlayers = Arrays.asList(FBChatComponent.instance.getServer().getOnlinePlayers());
            List<Player> targetPlayers = new ArrayList<>();
            switch(chatMessageOut.to.type) {
                case "all":
                    targetPlayers = allPlayers;
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
                            if (Utils.getPlayerUUID(player).equals(UUID.fromString(playerUUID)) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
            }

            for(Player targetPlayer : targetPlayers) {
                targetPlayer.sendMessage(chatMessageOut.contents.plain);
            }
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
