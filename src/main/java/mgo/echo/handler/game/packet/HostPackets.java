package mgo.echo.handler.game.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.protocol.command.HostsCmd;
import mgo.echo.util.Packets;

/**
 * Packet writers for host operation responses.
 */
public final class HostPackets {

    private HostPackets() {
    }

    public static void writeCreateGameResponse(ChannelHandlerContext ctx, int gameId) {
        ByteBuf bo = ctx.alloc().directBuffer(0x8);
        bo.writeInt(0).writeInt(gameId);
        Packets.write(ctx, HostsCmd.CREATE_GAME_RESPONSE, bo);
    }

    public static void writePlayerIdResponse(ChannelHandlerContext ctx, int command, int playerId) {
        ByteBuf bo = ctx.alloc().directBuffer(0x8);
        bo.writeInt(0).writeInt(playerId);
        Packets.write(ctx, command, bo);
    }
}
