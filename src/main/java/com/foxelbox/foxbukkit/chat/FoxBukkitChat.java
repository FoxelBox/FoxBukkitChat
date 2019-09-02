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
import com.foxelbox.foxbukkit.chat.json.ChatMessageOut;
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

public class FoxBukkitChat extends JavaPlugin {
    public FoxBukkitChat() {

    }

    HashSet<UUID> registeredPlayers = new HashSet<>();

    public Configuration configuration;
    public PlayerHelper playerHelper;
    public ChatHelper chatHelper;
    public SharedFormatHandler formatHandler;

    private void sendReply(CommandSender sender, String msg) {
        chatHelper.sendMessageTo(sender, "<color name=\"dark_purple\">[FBC]</color> " + msg);
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
        getServer().getPluginCommand("me").setExecutor((commandSender, command, cmd, args) -> {
            if (args.length < 1) {
                return false;
            }
            ChatMessageIn msg = new ChatMessageIn(FoxBukkitChat.this, commandSender);
            msg.contents = Utils.concatArray(args, 0, "");
            chatHelper.sendMessage(formatHandler.generateMe(msg));
            return true;
        });
        getServer().getPluginCommand("reloadchat").setExecutor(((commandSender, command, s, strings) -> {
            playerHelper.reload();
            sendReply(commandSender, "Reloading chat config");
            return true;
        }));
        getServer().getPluginCommand("pm").setExecutor((commandSender, command, cmd, args) -> {
            if (args.length < 2) {
                return false;
            }
            Player targetPly = FoxBukkitChat.this.getServer().getPlayer(args[0]);
            if (targetPly == null) {
                sendReply(commandSender, "Could not find player");
                return true;
            }

            ChatMessageIn msg = new ChatMessageIn(FoxBukkitChat.this, commandSender);
            msg.contents = Utils.concatArray(args, 1, "");
            for (ChatMessageOut msgOut : formatHandler.generatePM(msg, targetPly.getUniqueId())) {
                chatHelper.sendMessage(msgOut);
            }
            return true;
        });
        getServer().getPluginCommand("ignore").setExecutor((commandSender, command, cmd, args) -> {
            if (args.length < 1) {
                return false;
            }

            final String subCmd = args[0].toLowerCase();
            UUID src = Utils.getCommandSenderUUID(commandSender);

            switch (subCmd) {
                case "add":
                case "remove":
                    if (args.length < 2) {
                        return false;
                    }
                    boolean remove = subCmd.charAt(0) == 'r';

                    Player ply = FoxBukkitChat.this.getServer().getPlayer(args[1]);
                    UUID target;
                    if (ply == null) {
                        target = FoxBukkitChat.this.playerHelper.getUUIDByName(args[1]);
                    } else {
                        target = ply.getUniqueId();
                    }
                    if (target == null) {
                        sendReply(commandSender, "Could not find player");
                        return true;
                    }

                    Set<UUID> old = playerHelper.getIgnore(src);
                    if (remove) {
                        sendReply(commandSender, "Removed " + ((ply != null) ? ply.getName() : args[1]) + " from your ignore list");
                        old.remove(target);
                    } else {
                        sendReply(commandSender, "Added " + ((ply != null) ? ply.getName() : args[1]) + " to your ignore list");
                        old.add(target);
                    }
                    playerHelper.ignoreList.put(src.toString(), Utils.concat(old, 0, "", ','));
                    return true;
                case "list":
                    String list = playerHelper.ignoreList.get(src.toString());
                    if (list == null) {
                        sendReply(commandSender, "You have not ignored anyone");
                        return true;
                    }
                    sendReply(commandSender, "Ignore list: " + list);
                    return true;
            }
            return false;
        });
    }

    class FBChatListener implements Listener {
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
