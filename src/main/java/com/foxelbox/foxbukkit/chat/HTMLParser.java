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

import com.foxelbox.foxbukkit.chat.html.Element;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.xml.sax.*;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParserFactory;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

public class HTMLParser {
    static class WhitespaceAwareUnmarshallerHandler implements ContentHandler {
        private final UnmarshallerHandler uh;
        public WhitespaceAwareUnmarshallerHandler( UnmarshallerHandler uh ) {
            this.uh = uh;
        }
        /**
         * Replace all-whitespace character blocks with the character '\u000B',
         * which satisfies the following properties:
         *
         * 1. "\u000B".matches( "\\s" ) == true
         * 2. when parsing XmlMixed content, JAXB does not suppress the whitespace
         **/
        public void characters(
                char[] ch, int start, int length
        ) throws SAXException {
            for ( int i = start + length - 1; i >= start; --i )
                if ( !Character.isWhitespace( ch[ i ] ) ) {
                    uh.characters( ch, start, length );
                    return;
                }
            Arrays.fill( ch, start, start + length, '\u000B' );
            uh.characters( ch, start, length );
        }
        /* what follows is just blind delegation monkey code */
        public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException { uh.characters( ch, start, length ); }
        public void endDocument() throws SAXException { uh.endDocument(); }
        public void endElement( String uri, String localName, String name ) throws SAXException { uh.endElement( uri,  localName, name ); }
        public void endPrefixMapping( String prefix ) throws SAXException { uh.endPrefixMapping( prefix ); }
        public void processingInstruction( String target, String data ) throws SAXException { uh.processingInstruction(  target, data ); }
        public void setDocumentLocator( Locator locator ) { uh.setDocumentLocator( locator ); }
        public void skippedEntity( String name ) throws SAXException { uh.skippedEntity( name ); }
        public void startDocument() throws SAXException { uh.startDocument(); }
        public void startElement( String uri, String localName, String name, Attributes atts ) throws SAXException { uh.startElement( uri, localName, name, atts ); }
        public void startPrefixMapping( String prefix, String uri ) throws SAXException { uh.startPrefixMapping( prefix, uri ); }
    }

    private static final SAXParserFactory saxParserFactory;
    private static final JAXBContext jaxbContext;
    static {
        try {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(HTMLParser.class.getClassLoader());
            jaxbContext = JAXBContext.newInstance(Element.class);
            Thread.currentThread().setContextClassLoader(old);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        saxParserFactory = SAXParserFactory.newDefaultInstance();
    }

    /*
     * ChatComponentText = TextComponent
     * ChatMessage = TranslatableComponent
     * ChatModifier = Style
     * ChatClickable = ClickEvent
     * ChatHoverable = HoverEvent
     */
    public static String formatParams(String xmlSource, String... params) {
        return String.format(xmlSource, (Object[])xmlEscapeArray(params));
    }

    private static String[] xmlEscapeArray(String[] in) {
        final String[] out = new String[in.length];
        for(int i = 0; i < in.length; i++)
            out[i] = escape(in[i]);
        return out;
    }

    private static XMLReader getXMLReader() throws Exception {
        return saxParserFactory.newSAXParser().getXMLReader();
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T unmarshal(String strData, boolean flgWhitespaceAware) throws Exception {
        final UnmarshallerHandler uh;
        synchronized (jaxbContext) {
            uh = jaxbContext.createUnmarshaller().getUnmarshallerHandler();
            XMLReader xr = getXMLReader();
            xr.setContentHandler(flgWhitespaceAware ? new WhitespaceAwareUnmarshallerHandler(uh) : uh);
            xr.parse(new InputSource(new StringReader(strData)));
        }
        return (T)uh.getResult();
    }

    public static BaseComponent parse(String xmlSource) throws Exception {
        xmlSource = "<span>" + xmlSource + "</span>";
        final Element element = unmarshal(xmlSource, true);
        return element.getDefaultNmsComponent();
    }

    public static BaseComponent format(String format) throws Exception {
        return parse(format);
    }

    public static boolean sendToAll(FoxBukkitChat plugin, String format) {
        return sendToPlayers(plugin, plugin.getServer().getOnlinePlayers(), format);
    }

    public static boolean sendToPlayers(FoxBukkitChat plugin, Collection<? extends CommandSender> targetPlayers, String format) {
        try {
            final BaseComponent packet = format(format);

            for (CommandSender commandSender : targetPlayers) {
                if (!(commandSender instanceof Player)) {
                    commandSender.sendMessage(parsePlain(format));
                    continue;
                }

                ((Player)commandSender).spigot().sendMessage(packet);
            }

            return true;
        }
        catch (Exception e) {
            System.out.println("ERROR ON MESSAGE: " + format);
            e.printStackTrace();
            Bukkit.broadcastMessage("Error parsing XML");

            return false;
        }
    }

    public static boolean sendToPlayer(FoxBukkitChat plugin, Player player, String format) {
        try {
            player.spigot().sendMessage(format(format));
            return true;
        } catch (Exception e) {
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

    public static String escape(String s) {
        s = s.replace("&", "&amp;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'", "&apos;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");

        return s;
    }
}
