package de.doridian.yiffbukkit.chatcomponent;

import de.doridian.dependencies.redis.RedisManager;
import org.bukkit.entity.Player;

import java.util.Map;

public class PlayerHelper {
    public static Map<String,String> playerNameToUUID = YBChatComponent.instance.redisManager.createCachedRedisMap("playerNameToUUID");
    public static Map<String,String> playerUUIDToName = YBChatComponent.instance.redisManager.createCachedRedisMap("playerUUIDToName");
    public static void refreshUUID(Player player) {
        playerUUIDToName.put(player.getUniqueId().toString(), player.getName());
        playerNameToUUID.put(player.getName().toLowerCase(), player.getUniqueId().toString());
    }

    public static void refreshPlayerListRedis() {
        Player[] players = YBChatComponent.instance.getServer().getOnlinePlayers();
        final String keyName = "playersOnline:" + YBChatComponent.instance.configuration.getValue("server-name", "Main");
        YBChatComponent.instance.redisManager.del(keyName);
        for(Player ply : players) {
            YBChatComponent.instance.redisManager.lpush(keyName, ply.getUniqueId().toString());
        }
    }
}
