package de.doridian.yiffbukkit.chatcomponent;

import de.doridian.dependencies.redis.AbstractRedisHandler;
import de.doridian.dependencies.redis.RedisManager;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RedisHandler extends AbstractRedisHandler {
    public RedisHandler() {
        super("yiffbukkit:to_server");
    }

    public static void sendMessage(final Player player, final String  message) {
		if(player == null || message == null)
			throw new NullPointerException();
        RedisManager.publish("yiffbukkit:from_server", YBChatComponent.instance.configuration.getValue("server-name", "Main") + "|" + Utils.getPlayerUUID(player).toString() + "|" + player.getName() + "|" + message);
	}

	@Override
	public void onMessage(final String c_message) {
		try {
			final String[] split = c_message.split("\\|", 4);

			// SERVER\0 UUID\0 USER\0 message
			final String server = split[0];
			@SuppressWarnings("UnusedDeclaration")
			final UUID userUUID = UUID.fromString(split[1]);
			@SuppressWarnings("UnusedDeclaration")
			final String userName = split[2];
			String format = split[3];

			if (!server.equals(YBChatComponent.instance.configuration.getValue("server-name", "Main"))) {
				format = "\u00a72[" + server + "]\u00a7f " + format;
			}

			YBChatComponent.instance.getServer().broadcastMessage(format);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
