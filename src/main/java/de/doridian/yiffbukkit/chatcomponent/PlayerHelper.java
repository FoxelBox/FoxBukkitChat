/**
 * This file is part of YiffBukkitChatComponent.
 *
 * YiffBukkitChatComponent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * YiffBukkitChatComponent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with YiffBukkitChatComponent.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.doridian.yiffbukkit.chatcomponent;

import de.doridian.dependencies.redis.RedisManager;
import org.bukkit.entity.Player;

import java.util.Map;

public class PlayerHelper {
    public static Map<String,String> playerNameToUUID = YBChatComponent.instance.redisManager.createCachedRedisMap("playerNameToUUID");
    public static Map<String,String> playerUUIDToName = YBChatComponent.instance.redisManager.createCachedRedisMap("playerUUIDToName");
    public static void refreshUUID(Player player) {
        playerUUIDToName.put(Utils.getPlayerUUID(player).toString(), player.getName());
        playerNameToUUID.put(player.getName().toLowerCase(), Utils.getPlayerUUID(player).toString());
    }

    public static void refreshPlayerListRedis(Player ignoreMe) {
        Player[] players = YBChatComponent.instance.getServer().getOnlinePlayers();
        final String keyName = "playersOnline:" + YBChatComponent.instance.configuration.getValue("server-name", "Main");
        YBChatComponent.instance.redisManager.del(keyName);
        for(Player ply : players) {
            if(ply.equals(ignoreMe))
                continue;
            YBChatComponent.instance.redisManager.lpush(keyName, Utils.getPlayerUUID(ply).toString());
        }
    }
}
