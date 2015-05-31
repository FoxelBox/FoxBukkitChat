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
package com.foxelbox.foxbukkit.chatcomponent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FishBansResolver {
    private static final HttpProfileRepository HTTP_PROFILE_REPOSITORY = new HttpProfileRepository("minecraft");

    private static final LoadingCache<String, UUID> playerUUIDMap = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).softValues().build(new CacheLoader<String, UUID>() {
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
