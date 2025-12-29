package mgo.echo.controller;

import java.util.List;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.data.entity.User;
import mgo.echo.handler.account.AccountHandler;
import mgo.echo.handler.social.dto.MessageRecipientError;
import mgo.echo.handler.social.packet.MessagePackets;
import mgo.echo.handler.social.service.MessageService;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Message/Mail operations controller.
 * Thin routing layer - delegates to MessageService for logic.
 */
public class MessageController implements Controller {

    // ========================================================================
    // Send Message (0x4800)
    // ========================================================================

    @Command(0x4800)
    public boolean send(CommandContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                MessagePackets.writeSendError(ctx.nettyCtx());
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            String name = Util.readString(bi, 128);
            String comment = Util.readString(bi, 128);
            bi.skipBytes(708);
            int recipientType = bi.readByte();

            AccountHandler.updateUserClan(ctx.nettyCtx());
            Character character = user.getCurrentCharacter();

            List<MessageRecipientError> errors;

            if (recipientType == 1) {
                errors = MessageService.sendClanApplication(user, character, name, comment);
            } else {
                errors = List.of(new MessageRecipientError("Not implemented!", Error.NOT_IMPLEMENTED));
            }

            MessagePackets.writeSendResponse(ctx.nettyCtx(), errors);
        } catch (Exception e) {
            MessagePackets.writeSendError(ctx.nettyCtx());
        }

        return true;
    }

    // ========================================================================
    // Get Messages (0x4820)
    // ========================================================================

    @Command(0x4820)
    public boolean getMessages(CommandContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                Packets.write(ctx.nettyCtx(), 0x4821, Error.INVALID_SESSION);
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int type = bi.readByte();

            if (type == 0xf) {
                MessagePackets.writeMailMessages(ctx.nettyCtx());
                return true;
            }

            if (type == 0x10) {
                AccountHandler.updateUserClan(ctx.nettyCtx());
                Character character = user.getCurrentCharacter();

                if (character == null) {
                    Packets.write(ctx.nettyCtx(), 0x4821, Error.INVALID_SESSION);
                    return true;
                }

                List<MessageClanApplication> messages = MessageService.getClanApplicationMessages(character);
                if (messages == null) {
                    Packets.write(ctx.nettyCtx(), 0x4821, Error.CLAN_NOTAMEMBER);
                    return true;
                }

                MessagePackets.writeClanApplicationMessages(ctx.nettyCtx(), messages);
            }
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), 0x4821, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Get Message Contents (0x4840)
    // ========================================================================

    @Command(0x4840)
    public boolean getContents(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), 0x4341);
        return true;
    }

    // ========================================================================
    // Add Sent (0x4860)
    // ========================================================================

    @Command(0x4860)
    public boolean addSent(CommandContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                Packets.write(ctx.nettyCtx(), 0x4861, Error.INVALID_SESSION);
                return true;
            }

            Packets.write(ctx.nettyCtx(), 0x4861, 0);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), 0x4861, Error.GENERAL);
        }

        return true;
    }
}
