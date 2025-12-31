package mgo.echo.controller;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.User;
import mgo.echo.handler.account.service.AccountService;
import mgo.echo.handler.social.packet.ClanPacket;
import mgo.echo.handler.social.packet.ClanPacketHandler;
import mgo.echo.handler.social.service.ClanService;
import mgo.echo.handler.social.service.ClanService.ClanResult;
import mgo.echo.protocol.command.ClansCmd;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Clan operations controller.
 * 
 * Handles:
 * - Clan CRUD
 * - Membership Management
 * - Emblem Management
 * - Roster and Stats
 */
public class ClanController implements Controller {
    // ========================================================================
    // Clan Creation/Deletion (0x4b00, 0x4b04)
    // ========================================================================

    @Command(0x4b00)
    public boolean create(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeCreateResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        String name = Util.readString(bi, 16);
        String comment = Util.readString(bi, 128);

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanResult result = ClanService.createClan(character, name, comment);

        ClanPacket.writeCreateResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b04)
    public boolean disband(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeDisbandResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.disband(character, clanMember);

        ClanPacket.writeDisbandResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    // ========================================================================
    // Clan List/Info (0x4b10, 0x4b20, 0x4b80)
    // ========================================================================

    @Command(0x4b10)
    public boolean getList(CommandContext ctx) {
        ClanPacketHandler.getList(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    @Command(0x4b20)
    public boolean getInformationMember(CommandContext ctx) {
        ClanPacketHandler.getInformationMember(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    @Command(0x4b80)
    public boolean getInformation(CommandContext ctx) {
        ClanPacketHandler.getInformation(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    // ========================================================================
    // Membership (0x4b30, 0x4b32, 0x4b36, 0x4b40, 0x4b42)
    // ========================================================================

    @Command(0x4b30)
    public boolean acceptJoin(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeAcceptJoinResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.acceptJoin(clanMember, targetCharaId);

        ClanPacket.writeAcceptJoinResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b32)
    public boolean declineJoin(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeDeclineJoinResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.declineJoin(clanMember, targetCharaId);

        ClanPacket.writeDeclineJoinResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b36)
    public boolean banish(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeBanishResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.banish(clanMember, targetCharaId);

        ClanPacket.writeBanishResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b40)
    public boolean leave(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeLeaveResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanResult result = ClanService.leave(character);

        ClanPacket.writeLeaveResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b42)
    public boolean apply(CommandContext ctx) {
        // Stub - just acknowledge
        Packets.write(ctx.nettyCtx(), ClansCmd.APPLY_RESPONSE, 0);
        return true;
    }

    // ========================================================================
    // Clan State/Settings (0x4b46)
    // ========================================================================

    @Command(0x4b46)
    public boolean updateState(CommandContext ctx) {
        ClanPacketHandler.updateState(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    // ========================================================================
    // Emblem (0x4b48, 0x4b4a, 0x4b4c, 0x4b50)
    // ========================================================================

    @Command(0x4b48)
    public boolean getEmblem(CommandContext ctx) {
        ClanPacketHandler.getEmblem(ctx.nettyCtx(), ctx.packet(), 0x4b49, false);
        return true;
    }

    @Command(0x4b4a)
    public boolean getEmblem2(CommandContext ctx) {
        ClanPacketHandler.getEmblem(ctx.nettyCtx(), ctx.packet(), 0x4b4b, false);
        return true;
    }

    @Command(0x4b4c)
    public boolean getEmblemWip(CommandContext ctx) {
        ClanPacketHandler.getEmblem(ctx.nettyCtx(), ctx.packet(), 0x4b4d, true);
        return true;
    }

    @Command(0x4b50)
    public boolean setEmblem(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeSetEmblemResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ByteBuf bi = ctx.packet().getPayload();
        int type = bi.readByte();
        byte[] emblem = new byte[565];
        bi.readBytes(emblem);

        boolean isWip = type == 2;
        ClanResult result = ClanService.setEmblem(clanMember, emblem, isWip);

        ClanPacket.writeSetEmblemResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    // ========================================================================
    // Roster (0x4b52)
    // ========================================================================

    @Command(0x4b52)
    public boolean getRoster(CommandContext ctx) {
        ClanPacketHandler.getRoster(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    // ========================================================================
    // Leadership/Management (0x4b60, 0x4b62, 0x4b64, 0x4b66)
    // ========================================================================

    @Command(0x4b60)
    public boolean transferLeadership(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeTransferLeadershipResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.transferLeadership(clanMember, targetCharaId);

        ClanPacket.writeTransferLeadershipResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b62)
    public boolean setEmblemEditor(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeSetEmblemEditorResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.setEmblemEditor(clanMember, targetCharaId);

        ClanPacket.writeSetEmblemEditorResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b64)
    public boolean updateComment(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeUpdateCommentResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        String comment = Util.readString(bi, 128);

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.updateComment(clanMember, comment);

        ClanPacket.writeUpdateCommentResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b66)
    public boolean updateNotice(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPacket.writeUpdateNoticeResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        String notice = Util.readString(bi, 512);

        AccountService.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.updateNotice(clanMember, notice);

        ClanPacket.writeUpdateNoticeResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    // ========================================================================
    // Stats/Search (0x4b70, 0x4b90)
    // ========================================================================

    @Command(0x4b70)
    public boolean getStats(CommandContext ctx) {
        ClanPacketHandler.getStats(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    @Command(0x4b90)
    public boolean search(CommandContext ctx) {
        ClanPacketHandler.search(ctx.nettyCtx(), ctx.packet());
        return true;
    }
}
