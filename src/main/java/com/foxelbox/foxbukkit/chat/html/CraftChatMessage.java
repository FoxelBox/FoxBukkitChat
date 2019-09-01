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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CraftChatMessage {
    private static class FromString {
        public static boolean IsColor(ChatColor cc) {
            char c = cc.toString().charAt(1);
            return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9');
        }

        private final List<BaseComponent> list = new ArrayList<>();
        private BaseComponent currentChatComponent = new TextComponent("");
        private BaseComponent defaultModifier;
        private BaseComponent modifier = new TextComponent();
        private StringBuilder builder = new StringBuilder();
        private final BaseComponent[] output;
        private static final Pattern url = Pattern.compile("^(\u00A7.)*?((?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*?)?)(\u00A7.)*?$");
        private int lastWord = 0;

        private FromString(String message) {
            this(message, new TextComponent(""));
        }

        private FromString(String message, BaseComponent style) {
            modifier = (defaultModifier = style).duplicate();
            if (message == null) {
                output = new BaseComponent[] { currentChatComponent };
                return;
            }
            list.add(currentChatComponent);

            ChatColor format = null;
            Matcher matcher = url.matcher(message);
            lastWord = 0;

            for (int i = 0; i < message.length(); i++) {
                char currentChar = message.charAt(i);
                if (currentChar == '\u00A7' && (i < (message.length() - 1)) && (format = ChatColor.getByChar(message.charAt(i + 1))) != null) {
                    checkUrl(matcher, message, i, false);
                    if (builder.length() > 0) {
                        appendNewComponent();
                    }

                    if (format == ChatColor.RESET) {
                        modifier = defaultModifier.duplicate();
                    } else if (IsColor(format)) {
                        switch (format) {
                            case BOLD:
                                modifier.setBold(Boolean.TRUE);
                                break;
                            case ITALIC:
                                modifier.setItalic(Boolean.TRUE);
                                break;
                            case STRIKETHROUGH:
                                modifier.setStrikethrough(Boolean.TRUE);
                                break;
                            case UNDERLINE:
                                modifier.setUnderlined(Boolean.TRUE);
                                break;
                            case MAGIC:
                                modifier.setObfuscated(Boolean.TRUE);
                                break;
                            default:
                                throw new AssertionError("Unexpected message format");
                        }
                    } else { // Color resets formatting
                        modifier = defaultModifier.duplicate();
                        modifier.setColor(format);
                    }
                    i++;
                } else if (currentChar == '\n') {
                    if (builder.length() > 0) {
                        appendNewComponent();
                    }
                    currentChatComponent = null;
                } else {
                    if (currentChar == ' ' || i == message.length() - 1) {
                        if (checkUrl(matcher, message, i, true)) {
                            break;
                        }
                    }
                    builder.append(currentChar);
                }
            }

            if (builder.length() > 0) {
                appendNewComponent();
            }

            output = list.toArray(new BaseComponent[0]);
        }

        private boolean checkUrl(Matcher matcher, String message, int i, boolean newWord) {
            Matcher urlMatcher = matcher.region(lastWord, i == message.length() - 1 ? message.length() : i);
            if (newWord) {
                lastWord = i + 1;
            }
            if (urlMatcher.find()) {
                String fullUrl = urlMatcher.group(2);
                String protocol = urlMatcher.group(3);
                String url = urlMatcher.group(4);
                String path = urlMatcher.group(5);
                builder.delete(builder.length() - fullUrl.length() + (i == message.length() - 1 ? 1 : 0), builder.length());
                if (builder.length() > 0) {
                    appendNewComponent();
                }
                builder.append(fullUrl);
                ClickEvent link = new ClickEvent(ClickEvent.Action.OPEN_URL,
                        (protocol!=null?protocol:"http") + "://" + url + (path!=null?path:""));
                modifier.setClickEvent(link);
                appendNewComponent();
                modifier.setClickEvent(null);
                if (!newWord) { //Force new word to prevent double checking
                    lastWord = i + 1;
                }
                if (i == message.length() - 1) {
                    return true;
                }
            }
            return false;
        }

        private void appendNewComponent() {
            BaseComponent addition = new TextComponent(builder.toString());
            addition.copyFormatting(modifier);
            builder = new StringBuilder();
            modifier = modifier.duplicate();
            if (currentChatComponent == null) {
                currentChatComponent = new TextComponent("");
                list.add(currentChatComponent);
            }
            currentChatComponent.addExtra(addition);
        }

        private BaseComponent[] getOutput() {
            return output;
        }
    }

    public static BaseComponent[] fromString(String message) {
        return new FromString(message).getOutput();
    }

    private CraftChatMessage() {
    }

    public static BaseComponent[] fromString(String message, BaseComponent style) {
        return new FromString(message, style).getOutput();
    }
}
