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
package com.foxelbox.foxbukkit.chat.json;

import com.foxelbox.foxbukkit.chat.FoxBukkitChat;
import com.foxelbox.foxbukkit.chat.Utils;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class UserInfo {
    public UserInfo(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UserInfo(FoxBukkitChat plugin, UUID ply) {
        this(ply, plugin.formatHandler.formatPlayerName(ply));
    }

    public UserInfo(FoxBukkitChat plugin, CommandSender ply) {
        this(plugin, Utils.getCommandSenderUUID(ply));
    }

    public UUID uuid;
    public String name;
}
