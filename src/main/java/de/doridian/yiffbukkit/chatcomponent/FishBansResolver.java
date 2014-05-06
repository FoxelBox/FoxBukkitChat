package de.doridian.yiffbukkit.chatcomponent;

import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;

import java.util.HashMap;
import java.util.UUID;

public class FishBansResolver {
	private static final HashMap<String, UUID> playerUUIDMap = new HashMap<>();

	private static final HttpProfileRepository HTTP_PROFILE_REPOSITORY = new HttpProfileRepository("minecraft");

	public static UUID getUUID(String username) {
		UUID ret = playerUUIDMap.get(username.toLowerCase());
		if(ret != null)
			return ret;
		try {
			Profile[] profiles = HTTP_PROFILE_REPOSITORY.findProfilesByNames(username);
			if(profiles.length == 1) {
				String uuidStr = profiles[0].getId();
				if(uuidStr.indexOf('-') < 1)
					uuidStr = uuidStr.substring(0, 8) + "-" + uuidStr.substring(8, 12) + "-" + uuidStr.substring(12, 16) + "-" + uuidStr.substring(16, 20) + "-" + uuidStr.substring(20);
				ret = UUID.fromString(uuidStr);
				playerUUIDMap.put(username.toLowerCase(), ret);
				return ret;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
