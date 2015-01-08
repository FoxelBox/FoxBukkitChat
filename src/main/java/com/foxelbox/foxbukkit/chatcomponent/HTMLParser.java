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
package com.foxelbox.foxbukkit.chatcomponent;

import com.foxelbox.foxbukkit.chatcomponent.PlayerHelper;
import com.foxelbox.foxbukkit.chatcomponent.html.Element;
import net.minecraft.server.v1_7_R4.ChatBaseComponent;
import net.minecraft.server.v1_7_R4.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HTMLParser {
	/*
	 * ChatComponentText = TextComponent
	 * ChatMessage = TranslatableComponent
	 * ChatModifier = Style
	 * ChatClickable = ClickEvent
	 * ChatHoverable = HoverEvent
	 */
	public static String formatParams(String xmlSource, String... params) {
        return String.format(xmlSource, xmlEscapeArray(params));
    }

    private static String[] xmlEscapeArray(String[] in) {
        final String[] out = new String[in.length];
        for(int i = 0; i < in.length; i++)
            out[i] = escape(in[i]);
        return out;
    }

    public static ChatBaseComponent parse(String xmlSource) throws JAXBException {
		xmlSource = "<span>" + xmlSource + "</span>";

		final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
		final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		final Element element = (Element) unmarshaller.unmarshal(new StringReader(xmlSource));

		return element.getDefaultNmsComponent();
	}

	public static ChatBaseComponent format(String format) throws JAXBException {
		return parse(format);
	}

	public static boolean sendToAll(String format) {
		return sendToPlayers(Arrays.asList(Bukkit.getOnlinePlayers()), format);
	}

	public static boolean sendToPlayers(List<? extends CommandSender> targetPlayers, String format) {
		try {
			final PacketPlayOutChat packet = createChatPacket(format);

			for (CommandSender commandSender : targetPlayers) {
				if (!(commandSender instanceof Player)) {
					commandSender.sendMessage(parsePlain(format));
					continue;
				}

				PlayerHelper.sendPacketToPlayer((Player) commandSender, packet);
			}

			return true;
		}
		catch (JAXBException e) {
			System.out.println("ERROR ON MESSAGE: " + format);
			e.printStackTrace();
			Bukkit.broadcastMessage("Error parsing XML");

			return false;
		}
	}

	public static boolean sendToPlayer(Player player, String format) {
		try {
			PlayerHelper.sendPacketToPlayer(player, createChatPacket(format));

			return true;
		} catch (JAXBException e) {
			System.out.println("ERROR ON MESSAGE: " + format);
			e.printStackTrace();
			player.sendMessage("Error parsing XML");

			return false;
		}
	}

	public static boolean sendToPlayer(CommandSender commandSender, String format) {
		if (commandSender instanceof Player)
			return sendToPlayer((Player) commandSender, format);

		commandSender.sendMessage(parsePlain(format));
		return true;
	}

	private static String parsePlain(String format) {
		return format; // TODO: strip XML tags
	}

	private static PacketPlayOutChat createChatPacket(String format) throws JAXBException {
		return new PacketPlayOutChat(format(format));
	}

	public static String escape(String s) {
		s = s.replace("&", "&amp;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&apos;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");

		return s;
	}
}
