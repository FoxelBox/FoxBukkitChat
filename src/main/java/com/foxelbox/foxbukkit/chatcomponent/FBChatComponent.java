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
import com.foxelbox.dependencies.threading.SimpleThreadCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;

public class FBChatComponent extends JavaPlugin {
    public static FBChatComponent instance;

    public FBChatComponent() {
        instance = this;
    }

    public Configuration configuration;
    public RedisManager redisManager;
    public RedisHandler redisHandler;

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public RedisHandler getRedisHandler() {
        return redisHandler;
    }

    private final HashSet<String> redisCommands = new HashSet<>();
    public void loadRedisCommands() {
        List<String> commands = redisManager.lrange("chatLinkCommands", 0, -1);
        synchronized (redisCommands) {
            redisCommands.clear();
            for (String str : commands)
                redisCommands.add(str);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        getDataFolder().mkdirs();
        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);
        redisHandler = new RedisHandler();

        loadRedisCommands();

        getServer().getPluginManager().registerEvents(new FBChatListener(), this);

        PlayerHelper.refreshPlayerListRedis(null);
    }

    class FBChatListener implements Listener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
            final Player ply = event.getPlayer();
            final String baseCmd = event.getMessage().substring(1).trim();

            int posSpace = baseCmd.indexOf(' ');
            String cmd; String argStr;
            if (posSpace < 0) {
                cmd = baseCmd.toLowerCase();
                argStr = "";
            } else {
                cmd = baseCmd.substring(0, posSpace).trim().toLowerCase();
                argStr = baseCmd.substring(posSpace).trim();
            }

            if(redisCommands.contains(cmd)) {
                event.setCancelled(true);
                RedisHandler.sendMessage(ply, "/" + cmd + " " + argStr);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            event.setCancelled(true);
            final String msg = event.getMessage();
            final Player ply = event.getPlayer();
            RedisHandler.sendMessage(ply, msg);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            PlayerHelper.refreshUUID(event.getPlayer());
            PlayerHelper.refreshPlayerListRedis(null);
            event.setJoinMessage(null);
            RedisHandler.sendMessage(event.getPlayer(), "join", "playerstate");
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            PlayerHelper.refreshPlayerListRedis(event.getPlayer());
            event.setQuitMessage(null);
            RedisHandler.sendMessage(event.getPlayer(), "quit", "playerstate");
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerKick(PlayerKickEvent event) {
            PlayerHelper.refreshPlayerListRedis(event.getPlayer());
            event.setLeaveMessage(null);
            RedisHandler.sendMessage(event.getPlayer(), "kick " + event.getReason(), "playerstate");
        }
    }
}
