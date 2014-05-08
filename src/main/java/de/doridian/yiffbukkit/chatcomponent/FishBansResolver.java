package de.doridian.yiffbukkit.chatcomponent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FishBansResolver {
	private static final HttpProfileRepository HTTP_PROFILE_REPOSITORY = new HttpProfileRepository("minecraft");

	private static final Cache<String, UUID> playerUUIDMap = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).softValues().build(new CacheLoader<String, UUID>() {
		@Override
		public UUID load(String username) throws Exception {
			Profile[] profiles = HTTP_PROFILE_REPOSITORY.findProfilesByNames(username);
			if(profiles.length == 1) {
				String uuidStr = profiles[0].getId();
				if(uuidStr.indexOf('-') < 1)
					uuidStr = uuidStr.substring(0, 8) + "-" + uuidStr.substring(8, 12) + "-" + uuidStr.substring(12, 16) + "-" + uuidStr.substring(16, 20) + "-" + uuidStr.substring(20);
				return UUID.fromString(uuidStr);
			}
			return null;
		}
	});

	public static UUID getUUID(String username) {
		try {
			return playerUUIDMap.get(username.toLowerCase());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
