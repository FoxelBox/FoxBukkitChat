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
package com.foxelbox.foxbukkit.chat.html;

import com.foxelbox.foxbukkit.chat.HTMLParser;
import net.minecraft.server.v1_10_R1.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XmlSeeAlso({
        A.class,
        B.class,
        Color.class,
        I.class,
        Obfuscated.class,
        S.class,
        Span.class,
        Tr.class,
        U.class,
})
public abstract class Element {
    @XmlAttribute
    private String onClick = null;

    @XmlAttribute
    private String onHover = null;

    @XmlElementRef(type = Element.class)
    @XmlMixed
    private List<Object> mixedContent = new ArrayList<>();

    protected abstract void modifyStyle(ChatModifier style);

    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^([^(]+)\\('(.*)'\\)$");
    public List<ChatBaseComponent> getNmsComponents(ChatModifier style, boolean condenseElements) throws Exception {
        modifyStyle(style);

        if (onClick != null) {
            final Matcher matcher = FUNCTION_PATTERN.matcher(onClick);
            if (!matcher.matches()) {
                throw new RuntimeException("Invalid click handler");
            }

            final String eventType = matcher.group(1);
            final String eventString = matcher.group(2);
            final ChatClickable.EnumClickAction enumClickAction = ChatClickable.EnumClickAction.a(eventType.toLowerCase());
            if (enumClickAction == null) {
                throw new RuntimeException("Unknown click action "+eventType);
            }

            style.setChatClickable(new ChatClickable(enumClickAction, eventString));
        }

        if (onHover != null) {
            final Matcher matcher = FUNCTION_PATTERN.matcher(onHover);
            if (!matcher.matches()) {
                throw new RuntimeException("Invalid hover handler");
            }

            final String eventType = matcher.group(1);
            final String eventString = matcher.group(2);
            final ChatHoverable.EnumHoverAction enumClickAction = ChatHoverable.EnumHoverAction.a(eventType.toLowerCase());
            if (enumClickAction == null) {
                throw new RuntimeException("Unknown click action "+eventType);
            }

            style.setChatHoverable(new ChatHoverable(enumClickAction, HTMLParser.parse(eventString)));
        }

        final List<ChatBaseComponent> components = new ArrayList<>();
        if (!condenseElements)
            mixedContent.add(0, "");
        for (Object o : mixedContent) {
            if (o instanceof String) {
                for (IChatBaseComponent baseComponent : CraftChatMessage.fromString(((String)o).replace('\u000B', ' '), style.clone())) {
                    components.add((ChatBaseComponent) baseComponent);
                }
            }
            else if (o instanceof Element) {
                final Element element = (Element) o;
                if (condenseElements) {
                    components.add(element.getNmsComponent(style.clone()));
                }
                else {
                    components.addAll(element.getNmsComponents(style.clone(), false));
                }
            }
            else {
                throw new RuntimeException(o.getClass().toString());
            }
        }

        return components;
    }

    public ChatBaseComponent getDefaultNmsComponent() throws Exception {
        return getNmsComponent(new ChatModifier());
    }

    public ChatBaseComponent getNmsComponent(ChatModifier style) throws Exception {
        return condense(getNmsComponents(style, false));
    }

    private static final Field ChatBaseComponent_listChildren;
    static {
        try {
            Field _ChatBaseComponent_listChildren = null;
            for(Field field : ChatBaseComponent.class.getDeclaredFields()) {
                if(field.getType().equals(List.class)) {
                    _ChatBaseComponent_listChildren = field;
                    break;
                }
            }
            if(_ChatBaseComponent_listChildren == null)
                throw new Exception("Could not find listOfChildren field in ChatBaseComponent");
            ChatBaseComponent_listChildren = _ChatBaseComponent_listChildren;
            ChatBaseComponent_listChildren.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ChatBaseComponent condense(List<ChatBaseComponent> components) {
        if (components.isEmpty()) {
            return null;
        }

        components = new ArrayList<>(components);

        final ChatBaseComponent head = components.remove(0);

        if (!components.isEmpty()) {
            try {
                ChatBaseComponent_listChildren.set(head, components);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return head;
    }
}
