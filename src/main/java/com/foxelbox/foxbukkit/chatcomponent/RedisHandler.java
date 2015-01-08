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

import com.foxelbox.dependencies.redis.AbstractRedisHandler;
import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chatcomponent.json.ChatMessageOut;
import com.google.gson.Gson;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class RedisHandler extends AbstractRedisHandler {
	public RedisHandler() {
		super(FBChatComponent.instance.redisManager, "foxbukkit:to_server");
	}

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

	private static final Gson gson = new Gson();

	@Override
	public void onMessage(final String c_message) {
		try {
			final ChatMessageOut chatMessageOut;
			synchronized (gson) {
				chatMessageOut = gson.fromJson(c_message, ChatMessageOut.class);
			}

			if(!chatMessageOut.type.equals("text"))
				return;

			Collection<? extends Player> allPlayers = Arrays.asList(FBChatComponent.instance.getServer().getOnlinePlayers());
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

			if(targetPlayers.isEmpty())
				return;

			if (!chatMessageOut.server.equals(FBChatComponent.instance.configuration.getValue("server-name", "Main"))) {
				chatMessageOut.contents.plain = "\u00a72[" + chatMessageOut.server + "]\u00a7f " + chatMessageOut.contents.plain;
				if(chatMessageOut.contents.xml != null)
					chatMessageOut.contents.xml = "<color name=\"dark_green\">[" + chatMessageOut.server + "]</color> " + chatMessageOut.contents.xml;
			}

			if(chatMessageOut.contents.xml != null)
				HTMLParser.sendToPlayers(targetPlayers, chatMessageOut.contents.xml);
			else
				for(Player plyTarget : targetPlayers)
					plyTarget.sendMessage(chatMessageOut.contents.plain);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
