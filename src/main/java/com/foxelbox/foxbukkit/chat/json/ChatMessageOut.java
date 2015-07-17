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
package com.foxelbox.foxbukkit.chat.json;

import com.foxelbox.foxbukkit.chat.Messages;
import com.foxelbox.foxbukkit.chat.ProtobufUUID;

import java.util.List;
import java.util.UUID;

public class ChatMessageOut {
    public String server;
    public UserInfo from;
    public MessageTarget to;

    public long id = 0;
    public long timestamp = System.currentTimeMillis() / 1000;

    public UUID context;
    public Boolean finalizeContext = false;
    public Messages.MessageType type = Messages.MessageType.TEXT;

    public String contents;

    public Messages.ChatMessageOut toProtoBuf() {
        Messages.ChatMessageOut.Builder builder = Messages.ChatMessageOut.newBuilder();

        if(server != null) {
            builder.setServer(server);
        }
        if(from != null) {
            builder.setFrom(Messages.UserInfo.newBuilder()
                    .setUuid(ProtobufUUID.convertJavaToProtobuf(from.uuid))
                    .setName(from.name));
        }
        if(to != null && to.type != Messages.TargetType.ALL) {
            Messages.MessageTarget.Builder toBuilder = Messages.MessageTarget.newBuilder()
                    .setType(to.type);
            toBuilder.clearFilter();
            for(String s : to.filter) {
                toBuilder.addFilter(s);
            }
            builder.setTo(toBuilder);
        }

        builder.setId(id);
        builder.setTimestamp(timestamp);

        builder.setContext(ProtobufUUID.convertJavaToProtobuf(context));
        if(!finalizeContext) {
            builder.setFinalizeContext(false);
        }
        if(type != null && type != Messages.MessageType.TEXT) {
            builder.setType(type);
        }

        if(contents != null) {
            builder.setContents(contents);
        }

        return builder.build();
    }

    public static ChatMessageOut fromProtoBuf(Messages.ChatMessageOut message) {
        ChatMessageOut ret = new ChatMessageOut();

        ret.server = message.getServer();
        if(message.getFrom() != null) {
            ret.from = new UserInfo(ProtobufUUID.convertProtobufToJava(message.getFrom().getUuid()), message.getFrom().getName());
        }

        if(message.getTo() != null) {
            List<String> filterTo = message.getTo().getFilterList();
            ret.to = new MessageTarget(message.getTo().getType(), filterTo.toArray(new String[filterTo.size()]));
        } else {
            ret.to = new MessageTarget(Messages.TargetType.ALL, null);
        }

        ret.id = message.getId();
        ret.timestamp = message.getTimestamp();

        ret.context = ProtobufUUID.convertProtobufToJava(message.getContext());
        ret.finalizeContext = message.getFinalizeContext();
        ret.type = message.getType();

        ret.contents = message.getContents();

        return ret;
    }
}
