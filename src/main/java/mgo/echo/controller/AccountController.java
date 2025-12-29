package mgo.echo.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.User;
import mgo.echo.handler.account.service.AccountService;
import mgo.echo.protocol.command.UsersCmd;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;

/**
 * Account operations controller for GameLobby.
 *
 * Handles:
 * - Session Check (with character ID validation)
 */
public class AccountController implements Controller {
    private static final Logger logger = LogManager.getLogger(AccountController.class);

    @Command(0x3003)
    public void checkSession(CommandContext ctx) {
        try {
            ByteBuf bi = ctx.payload();
            int id = bi.readInt();
            byte[] bytes = new byte[16];
            bi.readBytes(bytes);

            String mgo2Session = AccountService.decodeSessionBytes(bytes);
            logger.info("Decrypted session: {}", mgo2Session);

            User user = AccountService.findUserBySession(mgo2Session);
            if (user == null) {
                logger.error("Error while checking session: Bad session.");
                Packets.write(ctx.nettyCtx(), UsersCmd.CHECK_SESSION_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            if (id != user.getCurrentCharacterId()) {
                logger.error("Error while checking session: Bad character id ... {} -- {}",
                        id, user.getCurrentCharacterId());
                Packets.write(ctx.nettyCtx(), UsersCmd.CHECK_SESSION_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            if (!AccountService.connectToLobby(ctx.nettyCtx(), ctx.lobby(), user)) {
                Packets.write(ctx.nettyCtx(), UsersCmd.CHECK_SESSION_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            Packets.write(ctx.nettyCtx(), UsersCmd.CHECK_SESSION_RESPONSE, 0);
        } catch (Exception e) {
            logger.error("Exception while checking session.", e);
            Packets.write(ctx.nettyCtx(), UsersCmd.CHECK_SESSION_RESPONSE, Error.GENERAL);
        }
    }
}
