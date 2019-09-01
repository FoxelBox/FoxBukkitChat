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

import com.foxelbox.foxbukkit.chat.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chat.json.ChatMessageOut;
import com.foxelbox.foxbukkit.chat.json.UserInfo;

import java.util.UUID;
import java.util.regex.Pattern;

public class SharedFormatHandler {
    public static final String PLAYER_FORMAT = "<span onHover=\"show_text('%1$s')\" onClick=\"suggest_command('/pm %1$s ')\">%3$s</span>";
    public static final String MESSAGE_FORMAT = PLAYER_FORMAT + "<color name=\"white\">: %4$s</color>";
    public static final String KICK_FORMAT = "<color name=\"dark_red\">[-]</color> " + PLAYER_FORMAT + " <color name=\"yellow\">was kicked (%4$s)!</color>";
    public static final String QUIT_FORMAT = "<color name=\"dark_red\">[-]</color> " + PLAYER_FORMAT + " <color name=\"yellow\">disconnected!</color>";
    public static final String JOIN_FORMAT = "<color name=\"dark_green\">[+]</color> " + PLAYER_FORMAT + " <color name=\"yellow\">joined!</color>";
    public static final Pattern REMOVE_DISALLOWED_CHARS = Pattern.compile("[\u00a7\r\n\t]");
    public static final String EMOTE_FORMAT = "* " + PLAYER_FORMAT + " <color name=\"gray\">%4$s</color>";

    private final FoxBukkitChat plugin;

    public SharedFormatHandler(FoxBukkitChat plugin) {
        this.plugin = plugin;
    }

    public String formatPlayerName(UUID uuid) {
        String nick = plugin.getPlayerNick(uuid);
        if (nick == null) {
            return plugin.playerHelper.playerUUIDToName.get(uuid.toString());
        }
        return nick;
    }

    public ChatMessageOut generateMe(ChatMessageIn messageIn, String argStr)
    {
        argStr = SharedFormatHandler.REMOVE_DISALLOWED_CHARS.matcher(argStr).replaceAll("");

        final ChatMessageOut message = new ChatMessageOut(messageIn);

        message.setContents(
                EMOTE_FORMAT,
                new String[] {
                        messageIn.from.name, messageIn.from.uuid.toString(), formatPlayerName(messageIn.from.uuid), argStr
                });
        return message;
    }

    public ChatMessageOut generateJoin(UUID ply) {
        return generateFormat(ply, JOIN_FORMAT, "");
    }

    public ChatMessageOut generateLeave(UUID ply) {
        return generateFormat(ply, QUIT_FORMAT, "");
    }

    public ChatMessageOut generateKick(UUID ply, String msg) {
        msg = SharedFormatHandler.REMOVE_DISALLOWED_CHARS.matcher(msg).replaceAll("");

        return generateFormat(ply, KICK_FORMAT, msg);
    }

    public ChatMessageOut generateText(UUID ply, String msg) {
        msg = SharedFormatHandler.REMOVE_DISALLOWED_CHARS.matcher(msg).replaceAll("");

        return generateFormat(ply, MESSAGE_FORMAT, msg);
    }

    private ChatMessageOut generateFormat(UUID ply, String format, String arg) {
        final ChatMessageOut msg = new ChatMessageOut(plugin, new UserInfo(plugin, ply));
        msg.setContents(format, new String[] { formatPlayerName(ply), ply.toString(), plugin.playerHelper.playerUUIDToName.get(ply.toString()), arg });
        return msg;
    }
}
