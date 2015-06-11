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
package com.foxelbox.foxbukkit.chatcomponent.json;

import com.foxelbox.foxbukkit.chatcomponent.FBChatComponent;
import com.foxelbox.foxbukkit.chatcomponent.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChatMessageIn {
    public ChatMessageIn(FBChatComponent plugin, CommandSender commandSender) {
        this(plugin);
        if(commandSender instanceof Player)
            this.from = new UserInfo(Utils.getCommandSenderUUID(commandSender), commandSender.getName());
        else
            this.from = new UserInfo(Utils.CONSOLE_UUID, commandSender.getName());
    }

    public ChatMessageIn(FBChatComponent plugin) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.context = UUID.randomUUID();
        this.server = plugin.configuration.getValue("server-name", "Main");
    }

    public String server;

    public UserInfo from;

    public long timestamp;
    public UUID context;

    public String type;
    public String contents;
}
