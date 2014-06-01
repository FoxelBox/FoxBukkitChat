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
package de.doridian.foxbukkit.chatcomponent;

import com.google.gson.Gson;
import de.doridian.dependencies.redis.AbstractRedisHandler;
import de.doridian.foxbukkit.chatcomponent.json.ChatMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RedisHandler extends AbstractRedisHandler {
    public RedisHandler() {
        super(FBChatComponent.instance.redisManager, "yiffbukkit:to_server");
    }

    public static void sendMessage(final Player player, final String  message) {
		if(player == null || message == null)
			throw new NullPointerException();
        FBChatComponent.instance.redisManager.publish("yiffbukkit:from_server", FBChatComponent.instance.configuration.getValue("server-name", "Main") + "|" + Utils.getPlayerUUID(player).toString() + "|" + player.getName() + "|" + message);
	}

    private final Gson gson = new Gson();

	@Override
	public void onMessage(final String c_message) {
		try {
            final ChatMessage chatMessage;
            synchronized (gson) {
                chatMessage = gson.fromJson(c_message, ChatMessage.class);
            }

            if (!chatMessage.server.equals(FBChatComponent.instance.configuration.getValue("server-name", "Main"))) {
                chatMessage.contents.plain = "\u00a72[" + chatMessage.server + "]\u00a7f " + chatMessage.contents.plain;
            }

            List<Player> allPlayers = Arrays.asList(FBChatComponent.instance.getServer().getOnlinePlayers());
            List<Player> targetPlayers = new ArrayList<>();
            switch(chatMessage.to.type) {
                case "all":
                    targetPlayers = allPlayers;
                    break;
                case "permission":
                    for(String permission : chatMessage.to.filter)
                        for (Player player : allPlayers)
                            if (player.hasPermission(permission) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
                case "player":
                    for(String playerUUID : chatMessage.to.filter)
                        for (Player player : allPlayers)
                            if (Utils.getPlayerUUID(player).equals(UUID.fromString(playerUUID)) && !targetPlayers.contains(player))
                                targetPlayers.add(player);
                    break;
            }

            for(Player targetPlayer : targetPlayers) {
                targetPlayer.sendMessage(chatMessage.contents.plain);
            }
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
