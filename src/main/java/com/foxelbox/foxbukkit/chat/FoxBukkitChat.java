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
import com.foxelbox.foxbukkit.chat.json.ChatMessageIn;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.UUID;

public class FoxBukkitChat extends JavaPlugin {
    public FoxBukkitChat() {
        try {
            Class.forName("com.sun.xml.bind.v2.ContextFactory");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    HashSet<UUID> registeredPlayers = new HashSet<>();

    public Configuration configuration;
    public PlayerHelper playerHelper;
    public ChatHelper chatHelper;
    public SharedFormatHandler formatHandler;

    public String getPlayerNick(Player ply) {
        return getPlayerNick(ply.getUniqueId());
    }

    public String getPlayerNick(UUID uuid) {
        return playerHelper.playerNicks.get(uuid.toString());
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        getDataFolder().mkdirs();
        configuration = new Configuration(getDataFolder());
        playerHelper = new PlayerHelper(this);
        chatHelper = new ChatHelper(this);
        formatHandler = new SharedFormatHandler(this);
        getServer().getPluginManager().registerEvents(new FBChatListener(), this);
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

            if (cmd.equals("me") || cmd.equals("action")) {
                chatHelper.sendMessage(formatHandler.generateMe(new ChatMessageIn(FoxBukkitChat.this, ply), argStr));
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            event.setCancelled(true);
            final String msg = event.getMessage();
            final Player ply = event.getPlayer();

            chatHelper.sendMessage(formatHandler.generateText(ply.getUniqueId(), msg));
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            playerHelper.refreshUUID(event.getPlayer());
            event.setJoinMessage(null);
            if(registeredPlayers.add(event.getPlayer().getUniqueId())) {
                chatHelper.sendMessage(formatHandler.generateJoin(event.getPlayer().getUniqueId()));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            event.setQuitMessage(null);
            if(registeredPlayers.remove(event.getPlayer().getUniqueId())) {
                chatHelper.sendMessage(formatHandler.generateLeave(event.getPlayer().getUniqueId()));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerKick(PlayerKickEvent event) {
            event.setLeaveMessage(null);
            if(registeredPlayers.remove(event.getPlayer().getUniqueId())) {
                chatHelper.sendMessage(formatHandler.generateKick(event.getPlayer().getUniqueId(), event.getReason()));
            }
        }
    }
}
