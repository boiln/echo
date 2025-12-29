package mgo.echo.handler.game.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.handler.game.dto.JoinResult;
import mgo.echo.protocol.command.GamesCmd;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for join game response (0x4311).
 * Buffer size: 0x2b bytes
 */
public final class JoinResponsePacket {

    private static final int BUFFER_SIZE = 0x2b;

    private JoinResponsePacket() {
    }

    public static void write(ChannelHandlerContext ctx, JoinResult result) {
        if (!result.isSuccess()) {
            Packets.writeError(ctx, GamesCmd.JOIN_RESPONSE, result.getErrorCode());
            return;
        }

        ByteBuf bo = ctx.alloc().directBuffer(BUFFER_SIZE);
        bo.writeInt(0);
        Util.writeString(result.getPublicIp(), 16, bo);
        bo.writeShort(result.getPublicPort());
        Util.writeString(result.getPrivateIp(), 16, bo);
        bo.writeShort(result.getPrivatePort());
        bo.writeByte(0).writeByte(result.getCurrentRule()).writeByte(result.getCurrentMap());

        Packets.write(ctx, GamesCmd.JOIN_RESPONSE, bo);
    }
}
