/**
 * This file is part of FoxBukkit.
 *
 * FoxBukkit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkit.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.chatcomponent.html;

import net.minecraft.server.v1_7_R4.ChatModifier;
import net.minecraft.server.v1_7_R4.EnumChatFormat;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.Field;
import java.util.Map;

@XmlRootElement
public class Color extends Element {
	@XmlAttribute
	private String name;

	@XmlAttribute
	private String id;

	private static final Map<Character, EnumChatFormat> EnumChatFormat_w;
	static {
		try {
			Field field = EnumChatFormat.class.getField("w");
			field.setAccessible(true);
			EnumChatFormat_w = (Map<Character, EnumChatFormat>)field.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void modifyStyle(ChatModifier style) {
		if (name != null) {
			style.setColor(EnumChatFormat.b(name.toUpperCase())); // v1_7_R1
		}

		if (id != null && !id.isEmpty()) {
			@SuppressWarnings("unchecked")
			final Map<Character, EnumChatFormat> idToChatFormat = EnumChatFormat_w; // v1_7_R1
			style.setColor(idToChatFormat.get(id.charAt(0)));
		}
	}
}
