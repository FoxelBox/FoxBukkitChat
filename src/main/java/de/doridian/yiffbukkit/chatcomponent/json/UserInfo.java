package de.doridian.yiffbukkit.chatcomponent.json;

import java.util.UUID;

public class UserInfo {
    public UserInfo(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid;
    public String name;
}
