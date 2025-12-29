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
 * Packet writer for gear response (0x4124).
 */
public final class GearPacket {
    private static final Logger logger = LogManager.getLogger(GearPacket.class);

    private static final byte[] GEAR_TERMINATOR = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff
    };

    // Gear items list
    private static final int[] GEAR_ITEMS = new int[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D,
            0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2E, 0x2F,
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40, 0x44, 0x45,
            0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x56, 0x57,
            0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F, 0x60, 0x61, 0x62, 0x63, 0x64, 0x66, 0x67, 0x68,
            0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x80,
            0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F,
            0xA0, 0xA1, 0xA2, 0xB0, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xF0, 0xF1, 0xF2, 0xF3, 0xF4
    };

    private GearPacket() {
    }

    public static void write(ChannelHandlerContext ctx) {
        ByteBuf bo = null;
        try {
            int itemCount = GEAR_ITEMS.length;
            int payloadSize = 36 + (itemCount * 5) + GEAR_TERMINATOR.length + 32;
            bo = ctx.alloc().directBuffer(payloadSize);

            // 35 bytes header - weapon camos
            for (int i = 0; i < 10; i++) {
                bo.writeByte(0x24); // White camo for MK.2
            }
            bo.writeByte(0x08); // Byte 10: M4 Maroon
            bo.writeByte(0x1C); // Byte 11: AK World Champion
            bo.writeZero(23); // bytes 12-34

            // Byte 35 = count
            bo.writeByte(itemCount);

            // Items start at byte 36
            for (int gearItem : GEAR_ITEMS) {
                int colors = 0xffffffff; // All colors unlocked
                bo.writeByte(gearItem).writeInt(colors);
            }

            // 32 bytes of 0xFF terminator
            bo.writeBytes(GEAR_TERMINATOR);

            // 30 bytes zero padding + 0x3370 = 32 bytes trailing
            bo.writeZero(30);
            bo.writeShort(0x3370);

            Packets.write(ctx, CharactersCmd.GET_GEAR_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing gear packet.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.GET_GEAR_RESPONSE, Error.GENERAL);
        }
    }
}
