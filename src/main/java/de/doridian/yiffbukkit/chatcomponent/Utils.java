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
