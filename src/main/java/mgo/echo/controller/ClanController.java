package mgo.echo.controller;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.User;
import mgo.echo.handler.account.AccountHandler;
import mgo.echo.handler.social.ClanHandler;
import mgo.echo.handler.social.packet.ClanPackets;
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
            ClanPackets.writeCreateResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        String name = Util.readString(bi, 16);
        String comment = Util.readString(bi, 128);

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanResult result = ClanService.createClan(character, name, comment);

        ClanPackets.writeCreateResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b04)
    public boolean disband(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeDisbandResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.disband(character, clanMember);

        ClanPackets.writeDisbandResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    // ========================================================================
    // Clan List/Info (0x4b10, 0x4b20, 0x4b80)
    // ========================================================================

    @Command(0x4b10)
    public boolean getList(CommandContext ctx) {
        ClanHandler.getList(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    @Command(0x4b20)
    public boolean getInformationMember(CommandContext ctx) {
        ClanHandler.getInformationMember(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    @Command(0x4b80)
    public boolean getInformation(CommandContext ctx) {
        ClanHandler.getInformation(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    // ========================================================================
    // Membership (0x4b30, 0x4b32, 0x4b36, 0x4b40, 0x4b42)
    // ========================================================================

    @Command(0x4b30)
    public boolean acceptJoin(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeAcceptJoinResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.acceptJoin(clanMember, targetCharaId);

        ClanPackets.writeAcceptJoinResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b32)
    public boolean declineJoin(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeDeclineJoinResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.declineJoin(clanMember, targetCharaId);

        ClanPackets.writeDeclineJoinResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b36)
    public boolean banish(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeBanishResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.banish(clanMember, targetCharaId);

        ClanPackets.writeBanishResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b40)
    public boolean leave(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeLeaveResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanResult result = ClanService.leave(character);

        ClanPackets.writeLeaveResponse(ctx.nettyCtx(), result.error);
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
        ClanHandler.updateState(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    // ========================================================================
    // Emblem (0x4b48, 0x4b4a, 0x4b4c, 0x4b50)
    // ========================================================================

    @Command(0x4b48)
    public boolean getEmblem(CommandContext ctx) {
        ClanHandler.getEmblem(ctx.nettyCtx(), ctx.packet(), 0x4b49, false);
        return true;
    }

    @Command(0x4b4a)
    public boolean getEmblem2(CommandContext ctx) {
        ClanHandler.getEmblem(ctx.nettyCtx(), ctx.packet(), 0x4b4b, false);
        return true;
    }

    @Command(0x4b4c)
    public boolean getEmblemWip(CommandContext ctx) {
        ClanHandler.getEmblem(ctx.nettyCtx(), ctx.packet(), 0x4b4d, true);
        return true;
    }

    @Command(0x4b50)
    public boolean setEmblem(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeSetEmblemResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ByteBuf bi = ctx.packet().getPayload();
        int type = bi.readByte();
        byte[] emblem = new byte[565];
        bi.readBytes(emblem);

        boolean isWip = type == 2;
        ClanResult result = ClanService.setEmblem(clanMember, emblem, isWip);

        ClanPackets.writeSetEmblemResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    // ========================================================================
    // Roster (0x4b52)
    // ========================================================================

    @Command(0x4b52)
    public boolean getRoster(CommandContext ctx) {
        ClanHandler.getRoster(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    // ========================================================================
    // Leadership/Management (0x4b60, 0x4b62, 0x4b64, 0x4b66)
    // ========================================================================

    @Command(0x4b60)
    public boolean transferLeadership(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeTransferLeadershipResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.transferLeadership(clanMember, targetCharaId);

        ClanPackets.writeTransferLeadershipResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b62)
    public boolean setEmblemEditor(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeSetEmblemEditorResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        int targetCharaId = bi.readInt();

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.setEmblemEditor(clanMember, targetCharaId);

        ClanPackets.writeSetEmblemEditorResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b64)
    public boolean updateComment(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeUpdateCommentResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        String comment = Util.readString(bi, 128);

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.updateComment(clanMember, comment);

        ClanPackets.writeUpdateCommentResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    @Command(0x4b66)
    public boolean updateNotice(CommandContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            ClanPackets.writeUpdateNoticeResponse(ctx.nettyCtx(), Error.INVALID_SESSION);
            return true;
        }

        ByteBuf bi = ctx.packet().getPayload();
        String notice = Util.readString(bi, 512);

        AccountHandler.updateUserClan(ctx.nettyCtx());

        Character character = user.getCurrentCharacter();
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

        ClanResult result = ClanService.updateNotice(clanMember, notice);

        ClanPackets.writeUpdateNoticeResponse(ctx.nettyCtx(), result.error);
        return true;
    }

    // ========================================================================
    // Stats/Search (0x4b70, 0x4b90)
    // ========================================================================

    @Command(0x4b70)
    public boolean getStats(CommandContext ctx) {
        ClanHandler.getStats(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    @Command(0x4b90)
    public boolean search(CommandContext ctx) {
        ClanHandler.search(ctx.nettyCtx(), ctx.packet());
        return true;
    }
}
