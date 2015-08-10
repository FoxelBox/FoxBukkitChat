/**
 * This file is part of FoxBukkitChat.
 *
 * FoxBukkitChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitChat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitChat.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.chat;

import com.foxelbox.dependencies.config.Configuration;
import com.foxelbox.dependencies.redis.RedisManager;
import com.foxelbox.dependencies.threading.SimpleThreadCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class FoxBukkitChat extends JavaPlugin {
    public FoxBukkitChat() {

    }

    HashSet<UUID> registeredPlayers = new HashSet<>();

    public Configuration configuration;
    public RedisManager redisManager;
    public ChatQueueHandler chatQueueHandler;
    public PlayerHelper playerHelper;

    public String getPlayerNick(Player ply) {
        return getPlayerNick(ply.getUniqueId());
    }

    public String getPlayerNick(UUID uuid) {
        return playerHelper.playerNicks.get(uuid.toString());
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public ChatQueueHandler getChatQueueHandler() {
        return chatQueueHandler;
    }

    private final HashSet<String> redisCommands = new HashSet<>();
    public void loadRedisCommands() {
        Set<String> commands = redisManager.smembers("chatLinkCommands");
        synchronized (redisCommands) {
            redisCommands.clear();
            for (String str : commands)
                redisCommands.add(str);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        redisManager.stop();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        getDataFolder().mkdirs();
        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);
        playerHelper = new PlayerHelper(this);
        chatQueueHandler = new ChatQueueHandler(this);

        loadRedisCommands();

        getServer().getPluginManager().registerEvents(new FBChatListener(), this);

        playerHelper.refreshPlayerListRedis(null);

        getServer().getPluginCommand("reloadclcommands").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                loadRedisCommands();
                return true;
            }
        });

        attachFilterTo(this.getServer().getLogger());
    }

    private static void attachFilterTo(Logger logger) {
        logger.setFilter(new FBLogFilter(logger.getFilter()));
    }

    private static final Pattern PM_PATTERN = Pattern.compile("^[^ ]+ issued server command: /(pm|msg|conv)( |$)");

    static class FBLogFilter implements Filter {
        private final Filter parentFilter;

        public FBLogFilter(Filter parentFilter) {
            this.parentFilter = parentFilter;
        }

        @Override
        public boolean isLoggable(LogRecord record) {
            if(parentFilter != null && !parentFilter.isLoggable(record)) {
                return false;
            }

            if(record.getLevel() != Level.INFO) {
                return true;
            }

            final String msg = record.getMessage().toLowerCase();
            return !PM_PATTERN.matcher(msg).matches();
        }
    }

    class FBChatListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
                chatQueueHandler.sendMessage(ply, "/" + cmd + " " + argStr);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            event.setCancelled(true);
            final String msg = event.getMessage();
            final Player ply = event.getPlayer();
            chatQueueHandler.sendMessage(ply, msg);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            playerHelper.refreshUUID(event.getPlayer());
            playerHelper.refreshPlayerListRedis(null);
            event.setJoinMessage(null);
            if(registeredPlayers.add(event.getPlayer().getUniqueId())) {
                chatQueueHandler.sendMessage(event.getPlayer(), "join", Messages.MessageType.PLAYERSTATE);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            playerHelper.refreshPlayerListRedis(event.getPlayer());
            event.setQuitMessage(null);
            if(registeredPlayers.remove(event.getPlayer().getUniqueId())) {
                chatQueueHandler.sendMessage(event.getPlayer(), "quit", Messages.MessageType.PLAYERSTATE);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerKick(PlayerKickEvent event) {
            playerHelper.refreshPlayerListRedis(event.getPlayer());
            event.setLeaveMessage(null);
            if(registeredPlayers.remove(event.getPlayer().getUniqueId())) {
                chatQueueHandler.sendMessage(event.getPlayer(), "kick " + event.getReason(), Messages.MessageType.PLAYERSTATE);
            }
        }
    }
}
