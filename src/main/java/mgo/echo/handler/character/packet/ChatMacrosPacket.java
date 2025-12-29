package mgo.echo.handler.character.packet;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterChatMacro;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for chat macros (0x4121).
 * Writes 2 buffers of 0x301 bytes each.
 */
public final class ChatMacrosPacket {
    private static final Logger logger = LogManager.getLogger(ChatMacrosPacket.class);

    private static final int BUFFER_SIZE = 0x301;
    private static final int MACRO_STRING_LENGTH = 64;
    private static final int MACROS_PER_TYPE = 12;

    private ChatMacrosPacket() {
    }

    public static void write(ChannelHandlerContext ctx, List<CharacterChatMacro> macros) {
        ByteBuf[] bos = null;

        try {
            bos = new ByteBuf[2];
            for (int i = 0; i < bos.length; i++) {
                bos[i] = ctx.alloc().directBuffer(BUFFER_SIZE);
                bos[i].writeByte(i);
            }

            for (CharacterChatMacro macro : macros) {
                Util.writeString(macro.getText(), MACRO_STRING_LENGTH, bos[macro.getType()]);
            }

            for (ByteBuf bo : bos) {
                Packets.write(ctx, CharactersCmd.GET_CHAT_MACROS_RESPONSE, bo);
            }
        } catch (Exception e) {
            logger.error("Exception while writing chat macros packet.", e);
            Util.releaseBuffers(bos);
            Packets.writeError(ctx, CharactersCmd.GET_CHAT_MACROS_RESPONSE, 1);
        }
    }

    public static List<CharacterChatMacro> initializeMacros(Character character) {
        List<CharacterChatMacro> macros = character.getChatMacros();

        for (int typem = 0; typem < 2; typem++) {
            for (int index = 0; index < MACROS_PER_TYPE; index++) {
                CharacterChatMacro macro = new CharacterChatMacro();
                macro.setCharacterId(character.getId());
                macro.setCharacter(character);
                macro.setType(typem);
                macro.setIndex(index);
                macros.add(macro);
            }
        }

        return macros;
    }

    /**
     * Read macro update data from packet.
     * Returns the type (0 or 1) and populates the texts array.
     */
    public static int readUpdate(Packet in, String[] texts) {
        ByteBuf bi = in.getPayload();
        int type = bi.readByte();

        for (int i = 0; i < MACROS_PER_TYPE; i++) {
            texts[i] = Util.readString(bi, MACRO_STRING_LENGTH);
        }

        return type;
    }
}
