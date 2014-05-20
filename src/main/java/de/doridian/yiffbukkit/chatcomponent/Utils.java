/**
 * This file is part of YiffBukkitChatComponent.
 *
 * YiffBukkitChatComponent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * YiffBukkitChatComponent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with YiffBukkitChatComponent.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.doridian.yiffbukkit.chatcomponent;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

@SuppressWarnings("UnusedDeclaration")
public class Utils {
	public static String concat(Collection<String> parts, int start, String defaultText) {
		// TODO: optimize
		return concatArray(parts.toArray(new String[parts.size()]), start, defaultText);
	}

    public static UUID getPlayerUUID(Player player) {
        return getPlayerUUID(player, true);
    }

	public static UUID getPlayerUUID(Player player, boolean mayUseRedis) {
        if(mayUseRedis) {
            String uuid = PlayerHelper.playerNameToUUID.get(player.getName().toLowerCase());
            if (uuid != null)
                return UUID.fromString(uuid);
        }
		return FishBansResolver.getUUID(player.getName());
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
