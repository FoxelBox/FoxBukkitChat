package de.doridian.yiffbukkit.chatcomponent.config;

import de.doridian.yiffbukkit.chatcomponent.YBChatComponent;

import java.io.*;

public class ConfigFileWriter extends FileWriter {
	public ConfigFileWriter(String file) throws IOException {
        super(new File(YBChatComponent.instance.getDataFolder(), file));
	}
}
