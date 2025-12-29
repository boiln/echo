package mgo.echo.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterAppearance;
import mgo.echo.data.entity.User;
import mgo.echo.handler.account.packet.AccountPackets;
import mgo.echo.handler.account.service.AccountService;
import mgo.echo.handler.account.service.CharacterCrudService;
import mgo.echo.protocol.command.UsersCmd;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Account lobby controller.
 *
 * Handles:
 * - Session Check
 * - Character List
 * - Character CRUD
 */
public class AccountLobbyController implements Controller {
    private static final Logger logger = LogManager.getLogger(AccountLobbyController.class);

    // ========================================================================
    // Session Commands
    // ========================================================================

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

            if (id != user.getId()) {
                logger.error("Error while checking session: Bad id ... {} -- {}", id, user.getId());
                Packets.write(ctx.nettyCtx(), UsersCmd.CHECK_SESSION_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            AccountService.clearCurrentCharacter(user);

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

    @Command(0x3042)
    public void ack3042(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), 0x3041);
    }

    // ========================================================================
    // Character List
    // ========================================================================

    @Command(0x3048)
    public void getCharacterList(CommandContext ctx) {
        try {
            User user = ctx.user();
            if (user == null) {
                logger.error("Error while getting character list: No User.");
                Packets.write(ctx.nettyCtx(), UsersCmd.GET_CHARACTER_LIST_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            List<Character> characters = CharacterCrudService.getCharacters(user);
            characters = CharacterCrudService.sortByMain(characters, user.getMainCharacterId());

            AccountPackets.writeCharacterList(ctx.nettyCtx(), user, characters);
        } catch (Exception e) {
            logger.error("Exception while getting character list.", e);
            Packets.write(ctx.nettyCtx(), UsersCmd.GET_CHARACTER_LIST_RESPONSE, Error.GENERAL);
        }
    }

    // ========================================================================
    // Character CRUD
    // ========================================================================

    @Command(0x3101)
    public void createCharacter(CommandContext ctx) {
        try {
            User user = ctx.user();
            if (user == null) {
                logger.error("Error while creating character: No User.");
                Packets.write(ctx.nettyCtx(), UsersCmd.CREATE_CHARACTER_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            ByteBuf bi = ctx.payload();
            String name = Util.readString(bi, 16);

            Integer nameError = CharacterCrudService.validateName(name);
            if (nameError != null) {
                Packets.write(ctx.nettyCtx(), UsersCmd.CREATE_CHARACTER_RESPONSE, nameError);
                return;
            }

            if (CharacterCrudService.isNameTaken(name)) {
                logger.error("Error while creating character: Name is taken.");
                Packets.write(ctx.nettyCtx(), UsersCmd.CREATE_CHARACTER_RESPONSE, Error.CHAR_NAMETAKEN);
                return;
            }

            CharacterAppearance appearance = AccountPackets.readAppearance(bi);
            Character character = CharacterCrudService.createCharacter(name, user, appearance);

            AccountPackets.writeCreateResponse(ctx.nettyCtx(), character);
        } catch (Exception e) {
            logger.error("Exception while creating character.", e);
            Packets.write(ctx.nettyCtx(), UsersCmd.CREATE_CHARACTER_RESPONSE, Error.GENERAL);
        }
    }

    @Command(0x3103)
    public void selectCharacter(CommandContext ctx) {
        try {
            User user = ctx.user();
            if (user == null) {
                logger.error("Error while selecting character: No User.");
                Packets.write(ctx.nettyCtx(), UsersCmd.SELECT_CHARACTER_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            ByteBuf bi = ctx.payload();
            int index = bi.readByte();

            CharacterCrudService.selectCharacter(user, index);

            Packets.write(ctx.nettyCtx(), UsersCmd.SELECT_CHARACTER_RESPONSE, 0);
        } catch (Exception e) {
            logger.error("Exception while selecting character.", e);
            Packets.write(ctx.nettyCtx(), UsersCmd.SELECT_CHARACTER_RESPONSE, Error.GENERAL);
        }
    }

    @Command(0x3105)
    public void deleteCharacter(CommandContext ctx) {
        try {
            User user = ctx.user();
            if (user == null) {
                logger.error("Error while deleting character: No User.");
                Packets.write(ctx.nettyCtx(), UsersCmd.DELETE_CHARACTER_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            ByteBuf bi = ctx.payload();
            int index = bi.readByte();

            CharacterCrudService.DeleteResult result = CharacterCrudService.deleteCharacter(user, index);
            if (!result.success) {
                Packets.write(ctx.nettyCtx(), UsersCmd.DELETE_CHARACTER_RESPONSE, result.errorCode);
                return;
            }

            Packets.write(ctx.nettyCtx(), UsersCmd.DELETE_CHARACTER_RESPONSE, 0);
        } catch (Exception e) {
            logger.error("Exception while deleting character.", e);
            Packets.write(ctx.nettyCtx(), UsersCmd.DELETE_CHARACTER_RESPONSE, Error.GENERAL);
        }
    }
}
