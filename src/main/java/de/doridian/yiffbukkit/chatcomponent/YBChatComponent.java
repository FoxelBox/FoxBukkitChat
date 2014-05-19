package de.doridian.yiffbukkit.chatcomponent;

import de.doridian.dependencies.config.Configuration;
import de.doridian.dependencies.redis.RedisManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class YBChatComponent extends JavaPlugin {
	public static YBChatComponent instance;

	public YBChatComponent() {
		instance = this;
	}

    public Configuration configuration;

	@Override
	public void onEnable() {
		super.onEnable();
        getDataFolder().mkdirs();
        configuration = new Configuration(getDataFolder());
		RedisManager.initialize(configuration);
        new RedisHandler();

		getServer().getPluginManager().registerEvents(new YBChatListener(), this);
		getServer().getPluginCommand("me").setExecutor(new YBForwardedCommand());
		getServer().getPluginCommand("pm").setExecutor(new YBForwardedCommand());
		getServer().getPluginCommand("conv").setExecutor(new YBForwardedCommand());

        PlayerHelper.refreshPlayerListRedis();
	}

	class YBForwardedCommand implements CommandExecutor {
		@Override
		public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
			if(commandSender instanceof Player) {
				RedisHandler.sendMessage((Player)commandSender, "/" + s + " " + Utils.concatArray(strings, 0, ""));
				return true;
			}
			return false;
		}
	}

	class YBChatListener implements Listener {
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerChat(AsyncPlayerChatEvent event) {
			if(event.isCancelled())
				return;
			event.setCancelled(true);
			RedisHandler.sendMessage(event.getPlayer(), event.getMessage());
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerJoin(PlayerJoinEvent event) {
            PlayerHelper.refreshUUID(event.getPlayer());
            PlayerHelper.refreshPlayerListRedis();
			event.setJoinMessage(null);
			RedisHandler.sendMessage(event.getPlayer(), "\u0123join");
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerQuit(PlayerQuitEvent event) {
            PlayerHelper.refreshPlayerListRedis();
			event.setQuitMessage(null);
			RedisHandler.sendMessage(event.getPlayer(), "\u0123quit");
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onPlayerKick(PlayerKickEvent event) {
            PlayerHelper.refreshPlayerListRedis();
			event.setLeaveMessage(null);
			RedisHandler.sendMessage(event.getPlayer(), "\u0123kick " + event.getReason());
		}
	}
}
