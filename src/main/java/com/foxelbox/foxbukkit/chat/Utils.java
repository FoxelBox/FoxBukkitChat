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
    public static String concat(Collection<String> parts, int start, String defaultText) {
        // TODO: optimize
        return concatArray(parts.toArray(new String[parts.size()]), start, defaultText);
    }

    public static UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("[CONSOLE]".getBytes());

    public static UUID getCommandSenderUUID(CommandSender commandSender) {
        if(commandSender instanceof Player)
            return ((Player) commandSender).getUniqueId();
        if(commandSender instanceof ConsoleCommandSender)
            return CONSOLE_UUID;
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

    public static String concatArray(String[] array, int start, String defaultText) {
        if (array.length <= start)
            return defaultText;

        if (array.length <= start + 1)
            return array[start]; // optimization

        StringBuilder ret = new StringBuilder(array[start]);
        for(int i = start + 1; i < array.length; i++) {
            ret.append(' ');
            ret.append(array[i]);
        }
        return ret.toString();
    }
}
