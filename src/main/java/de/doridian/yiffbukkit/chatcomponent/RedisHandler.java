package de.doridian.yiffbukkit.chatcomponent;

import com.google.gson.Gson;
import de.doridian.dependencies.redis.AbstractRedisHandler;
import de.doridian.dependencies.redis.RedisManager;
import de.doridian.yiffbukkit.chatcomponent.json.ChatMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RedisHandler extends AbstractRedisHandler {
    public RedisHandler() {
        super(YBChatComponent.instance.redisManager, "yiffbukkit:to_server");
    }

    public static void sendMessage(final Player player, final String  message) {
		if(player == null || message == null)
			throw new NullPointerException();
        YBChatComponent.instance.redisManager.publish("yiffbukkit:from_server", YBChatComponent.instance.configuration.getValue("server-name", "Main") + "|" + Utils.getPlayerUUID(player).toString() + "|" + player.getName() + "|" + message);
	}

    private final Gson gson = new Gson();

	@Override
	public void onMessage(final String c_message) {
		try {
            final ChatMessage chatMessage;
            synchronized (gson) {
                chatMessage = gson.fromJson(c_message, ChatMessage.class);
            }

            if (!chatMessage.server.equals(YBChatComponent.instance.configuration.getValue("server-name", "Main"))) {
                chatMessage.contents.plain = "\u00a72[" + chatMessage.server + "]\u00a7f " + chatMessage.contents.plain;
            }

            List<Player> allPlayers = Arrays.asList(YBChatComponent.instance.getServer().getOnlinePlayers());
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
