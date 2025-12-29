package mgo.echo.handler.character.packet;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterBlocked;
import mgo.echo.data.entity.CharacterFriend;
import mgo.echo.data.entity.User;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for character info response (0x4101).
 * Buffer size: 0x243 bytes
 */
public final class CharacterInfoPacket {
    private static final Logger logger = LogManager.getLogger(CharacterInfoPacket.class);

    private static final int BUFFER_SIZE = 0x243;
    private static final int FRIENDS_PAD_OFFSET = 0x129;
    private static final int BLOCKED_PAD_OFFSET = 0x229;

    private static final byte[] BYTES_1 = {
            (byte) 0x16, (byte) 0xAE, (byte) 0x03, (byte) 0x38,
            (byte) 0x01, (byte) 0x3E, (byte) 0x01, (byte) 0x50
    };

    private static final byte[] BYTES_2 = {
            (byte) 0x00, (byte) 0xB7, (byte) 0xFD, (byte) 0xAB, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF,
            (byte) 0x7B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    private CharacterInfoPacket() {
    }

    public static void write(ChannelHandlerContext ctx, User user, Character character) {
        ByteBuf bo = null;

        try {
            int currentEpoch = (int) Instant.now().getEpochSecond();
            int secondLastLogin = currentEpoch - 1;
            int lastLogin = currentEpoch;
            int exp = getCharacterExperience(user, character);

            bo = ctx.alloc().directBuffer(BUFFER_SIZE);
            bo.writeInt(character.getId());
            Util.writeString(character.getName(), 16, bo);
            bo.writeBytes(BYTES_1);
            bo.writeInt(exp).writeInt(secondLastLogin).writeInt(lastLogin).writeZero(1);

            for (CharacterFriend friend : character.getFriends()) {
                bo.writeInt(friend.getTargetId());
            }
            Util.padTo(FRIENDS_PAD_OFFSET, bo);

            for (CharacterBlocked blocked : character.getBlocked()) {
                bo.writeInt(blocked.getTargetId());
            }
            Util.padTo(BLOCKED_PAD_OFFSET, bo);

            bo.writeBytes(BYTES_2);
            Packets.write(ctx, CharactersCmd.GET_CHARACTER_INFO_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing character info packet.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.GET_CHARACTER_INFO_RESPONSE, Error.GENERAL);
        }
    }

    private static int getCharacterExperience(User user, Character character) {
        boolean isMainCharacter = user.getMainCharacterId() != null
                && character.getId().equals(user.getMainCharacterId());
        return isMainCharacter ? user.getMainExp() : user.getAltExp();
    }
}
