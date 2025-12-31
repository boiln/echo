package mgo.echo.controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.User;
import mgo.echo.handler.game.dto.JoinResult;
import mgo.echo.handler.game.packet.GameDetailsPacket;
import mgo.echo.handler.game.packet.GameListEntryPacket;
import mgo.echo.handler.game.packet.JoinResponsePacket;
import mgo.echo.handler.game.service.GameService;
import mgo.echo.protocol.command.GamesCmd;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.session.ActiveGames;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Error;
import mgo.echo.util.Guards;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Game operations controller.
 * Thin routing layer - delegates to GameService for logic and packets for
 * serialization.
 */
public class GameController implements Controller {

    // ========================================================================
    // Game List (0x4300)
    // ========================================================================

    @Command(0x4300)
    public boolean getList(CommandContext ctx) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();

        try {
            User user = Guards.requireUser(ctx.nettyCtx(), 0x4301);
            if (user == null) {
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int type = bi.readInt();

            Character character = user.getCurrentCharacter();
            Lobby lobby = ctx.lobby();
            List<Game> filteredGames = GameService.filterGames(lobby, type);

            Packets.handleMutliElementPayload(ctx.nettyCtx(), filteredGames.size(), 18, 0x37, payloads, (i, bo) -> {
                Game game = filteredGames.get(i);
                GameListEntryPacket.write(bo, game, character);
            });

            Packets.write(ctx.nettyCtx(), 0x4301, 0);
            Packets.write(ctx.nettyCtx(), 0x4302, payloads);
            Packets.write(ctx.nettyCtx(), 0x4303, 0);
        } catch (Exception e) {
            Util.releaseBuffers(payloads);
            Packets.write(ctx.nettyCtx(), 0x4301, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Game Details (0x4312)
    // ========================================================================

    @Command(0x4312)
    public boolean getDetails(CommandContext ctx) {
        try {
            ByteBuf bi = ctx.packet().getPayload();
            int gameId = bi.readInt();

            Game game = ActiveGames.get(gameId);
            if (game == null) {
                Packets.write(ctx.nettyCtx(), GamesCmd.GET_DETAILS_RESPONSE, Error.INVALID_SESSION);
                return true;
            }

            GameDetailsPacket.write(ctx.nettyCtx(), game, ctx.lobby());
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), GamesCmd.GET_DETAILS_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Join Game (0x4320)
    // ========================================================================

    @Command(0x4320)
    public boolean join(CommandContext ctx) {
        try {
            User user = Guards.requireUser(ctx.nettyCtx(), GamesCmd.JOIN_RESPONSE);
            if (user == null) {
                return true;
            }

            Character character = user.getCurrentCharacter();

            ByteBuf bi = ctx.packet().getPayload();
            int gameId = bi.readInt();
            String password = Util.readString(bi, 16);

            JoinResult result = GameService.joinGame(character, gameId, password);
            JoinResponsePacket.write(ctx.nettyCtx(), result);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), GamesCmd.JOIN_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Join Failed (0x4322)
    // ========================================================================

    @Command(0x4322)
    public boolean joinFailed(CommandContext ctx) {
        try {
            User user = Guards.requireUser(ctx.nettyCtx(), GamesCmd.JOIN_FAILED_RESPONSE);
            if (user == null) {
                return true;
            }

            GameService.handleJoinFailed(user.getCurrentCharacter());
            Packets.write(ctx.nettyCtx(), GamesCmd.JOIN_FAILED_RESPONSE, 0);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), GamesCmd.JOIN_FAILED_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Quit Game (0x4380)
    // ========================================================================

    @Command(0x4380)
    public boolean quitGame(CommandContext ctx) {
        try {
            Packets.write(ctx.nettyCtx(), GamesCmd.QUIT_RESPONSE, 0);

            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            GameService.quitGame(user);
        } catch (Exception e) {
            // Ignore - cleanup operation
        }

        return true;
    }

    // ========================================================================
    // Chat (0x4400)
    // ========================================================================

    @Command(0x4400)
    public boolean chat(CommandContext ctx) {
        mgo.echo.handler.social.packet.ChatPacketHandler.send(ctx.nettyCtx(), ctx.packet());
        return true;
    }

    // ========================================================================
    // Unknown 0x4440
    // ========================================================================

    @Command(0x4440)
    public boolean unknown4440(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), 0x4441, 0);
        return true;
    }
}
