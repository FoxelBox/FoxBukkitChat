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

import com.foxelbox.dependencies.config.Configuration;
import com.foxelbox.dependencies.redis.RedisManager;
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

public class FBChatComponent extends JavaPlugin {
	public static FBChatComponent instance;

	public FBChatComponent() {
		instance = this;
	}

    public Configuration configuration;

    public RedisManager redisManager;

	@Override
	public void onEnable() {
		super.onEnable();
        getDataFolder().mkdirs();
        configuration = new Configuration(getDataFolder());
		redisManager = new RedisManager(configuration);
        new RedisHandler();

		getServer().getPluginManager().registerEvents(new FBChatListener(), this);
		getServer().getPluginCommand("me").setExecutor(new FBForwardedCommand());
		getServer().getPluginCommand("pm").setExecutor(new FBForwardedCommand());
		getServer().getPluginCommand("conv").setExecutor(new FBForwardedCommand());
		getServer().getPluginCommand("list").setExecutor(new FBForwardedCommand());

        PlayerHelper.refreshPlayerListRedis(null);
	}

	class FBForwardedCommand implements CommandExecutor {
		@Override
		public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
			if(commandSender instanceof Player) {
				RedisHandler.sendMessage((Player)commandSender, "/" + s + " " + Utils.concatArray(strings, 0, ""));
				return true;
			}
			return false;
		}
	}

	class FBChatListener implements Listener {
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerChat(AsyncPlayerChatEvent event) {
			if(event.isCancelled())
				return;
			event.setCancelled(true);
            final String msg = event.getMessage();
            if(msg.charAt(0) == '#')
                RedisHandler.sendMessage(event.getPlayer(), "/opchat " + msg.substring(1));
            else
			    RedisHandler.sendMessage(event.getPlayer(), msg);
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerJoin(PlayerJoinEvent event) {
            PlayerHelper.refreshUUID(event.getPlayer());
            PlayerHelper.refreshPlayerListRedis(null);
			event.setJoinMessage(null);
			RedisHandler.sendMessage(event.getPlayer(), "\u0123join");
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerQuit(PlayerQuitEvent event) {
            PlayerHelper.refreshPlayerListRedis(event.getPlayer());
			event.setQuitMessage(null);
			RedisHandler.sendMessage(event.getPlayer(), "\u0123quit");
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onPlayerKick(PlayerKickEvent event) {
            PlayerHelper.refreshPlayerListRedis(event.getPlayer());
			event.setLeaveMessage(null);
			RedisHandler.sendMessage(event.getPlayer(), "\u0123kick " + event.getReason());
		}
	}
}
