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

import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

public class PlayerHelper {
    private FBChatComponent plugin;
    public Map<String,String> playerNameToUUID;
    public Map<String,String> playerUUIDToName;

    public void refreshUUID(Player player) {
        playerUUIDToName.put(player.getUniqueId().toString(), player.getName());
        playerNameToUUID.put(player.getName().toLowerCase(), player.getUniqueId().toString());
    }

    public PlayerHelper(FBChatComponent plugin) {
        this.plugin = plugin;
        playerNameToUUID = plugin.redisManager.createCachedRedisMap("playerNameToUUID");
        playerUUIDToName = plugin.redisManager.createCachedRedisMap("playerUUIDToName");
    }

    public void refreshPlayerListRedis(Player ignoreMe) {
        Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
        final String keyName = "playersOnline:" + plugin.configuration.getValue("server-name", "Main");
        plugin.redisManager.del(keyName);
        for(Player ply : players) {
            if(ply.equals(ignoreMe))
                continue;
            plugin.redisManager.lpush(keyName, ply.getUniqueId().toString());
        }
    }

    public void sendPacketToPlayer(final Player ply, final Packet packet) {
        ((CraftPlayer)ply).getHandle().playerConnection.sendPacket(packet);
    }
}
