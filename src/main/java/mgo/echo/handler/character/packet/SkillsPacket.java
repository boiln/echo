package mgo.echo.handler.character.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for skills response (0x4125).
 */
public final class SkillsPacket {
    private static final Logger logger = LogManager.getLogger(SkillsPacket.class);

    private static final int NUM_SKILLS = 25;

    private SkillsPacket() {
    }

    public static void write(ChannelHandlerContext ctx) {
        ByteBuf bo = null;
        try {
            bo = ctx.alloc().directBuffer(0x4 + NUM_SKILLS * 0x4);

            bo.writeInt(NUM_SKILLS);

            for (int i = 1; i <= NUM_SKILLS; i++) {
                int exp = (i == 17 || i == 20 || i == 22) ? 0x2000 : 0x6000;
                bo.writeByte(i).writeShort(exp).writeZero(1);
            }

            Packets.write(ctx, CharactersCmd.GET_SKILLS_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing skills packet.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.GET_SKILLS_RESPONSE, Error.GENERAL);
        }
    }

    /**
     * Write skills data to a buffer (used in post-game info).
     */
    public static void writeSkillsData(ByteBuf bo) {
        bo.writeInt(NUM_SKILLS);

        for (int i = 1; i <= NUM_SKILLS; i++) {
            int exp = (i == 17 || i == 20 || i == 22) ? 0x2000 : 0x6000;
            bo.writeByte(i).writeShort(exp).writeZero(1);
        }
    }
}
