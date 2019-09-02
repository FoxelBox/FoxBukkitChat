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

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

@SuppressWarnings("UnusedDeclaration")
public class Utils {
    public static String concat(Collection parts, int start, String defaultText, char split) {
        return concatArray(parts.toArray(), start, defaultText, split);
    }

    public static String concat(Collection parts, int start, String defaultText) {
        return concatArray(parts.toArray(), start, defaultText);
    }

    public static UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("[CONSOLE]".getBytes());

    public static UUID getCommandSenderUUID(CommandSender commandSender) {
        if(commandSender instanceof Player) {
            return ((Player) commandSender).getUniqueId();
        } else if(commandSender instanceof ConsoleCommandSender) {
            return CONSOLE_UUID;
        }
        return UUID.nameUUIDFromBytes(("[CSUUID:" + commandSender.getClass().getName() + "]").getBytes());
    }

    public static String getCommandSenderDisplayName(CommandSender commandSender) {
        if(commandSender instanceof Player)
            return ((Player) commandSender).getDisplayName();
        return commandSender.getName();
    }

    public static String XMLEscape(String s) {
        s = s.replace("&", "&amp;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'", "&apos;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");

        return s;
    }

    public static String concatArray(Object[] array, int start, String defaultText) {
        return concatArray(array, start, defaultText, ' ');
    }

    public static String concatArray(Object[] array, int start, String defaultText, char split) {
        if (array.length <= start) {
            return defaultText;
        }

        if (array.length <= start + 1) {
            return array[start].toString(); // optimization
        }

        StringBuilder ret = new StringBuilder(array[start].toString());
        for(int i = start + 1; i < array.length; i++) {
            ret.append(split);
            ret.append(array[i]);
        }
        return ret.toString();
    }
}
