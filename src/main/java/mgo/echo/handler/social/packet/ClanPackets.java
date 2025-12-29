package mgo.echo.handler.social.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Clan;
import mgo.echo.protocol.command.ClansCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;

/**
 * Packet writers for clan response codes.
 * Complex multi-packet operations remain in ClanHandler.
 */
public final class ClanPackets {

    private ClanPackets() {
    }

    // =========================================================================
    // Emblem
    // =========================================================================

    public static void writeEmblem(ChannelHandlerContext ctx, int command, Clan clan, boolean getWip) {
        ByteBuf bo = ctx.alloc().directBuffer(4 + 565);
        bo.writeInt(0);
        writeEmblemData(bo, clan, getWip);
        Packets.write(ctx, command, bo);
    }

    private static void writeEmblemData(ByteBuf bo, Clan clan, boolean getWip) {
        if (getWip && clan.getEmblemWip() != null) {
            bo.writeBytes(clan.getEmblemWip());
            return;
        }

        if (clan.getEmblem() != null) {
            bo.writeBytes(clan.getEmblem());
            return;
        }

        bo.writeZero(565);
    }

    // =========================================================================
    // Simple Response Codes
    // =========================================================================

    public static void writeSetEmblemResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.SET_EMBLEM_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeSetEmblemEditorResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.SET_EMBLEM_EDITOR_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeUpdateCommentResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.UPDATE_COMMENT_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeUpdateNoticeResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.UPDATE_NOTICE_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeCreateResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.CREATE_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeApplyResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.APPLY_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeLeaveResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.LEAVE_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeAcceptJoinResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.ACCEPT_JOIN_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeDeclineJoinResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.DECLINE_JOIN_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeDisbandResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.DISBAND_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeBanishResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.BANISH_RESPONSE, error != null ? error.getCode() : 0);
    }

    public static void writeTransferLeadershipResponse(ChannelHandlerContext ctx, Error error) {
        Packets.write(ctx, ClansCmd.TRANSFER_LEADERSHIP_RESPONSE, error != null ? error.getCode() : 0);
    }
}
