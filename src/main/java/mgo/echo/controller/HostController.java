package mgo.echo.controller;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.handler.game.dto.HostSettingsDto;
import mgo.echo.handler.game.packet.HostPackets;
import mgo.echo.handler.game.packet.HostSettingsPacket;
import mgo.echo.handler.game.service.GameService;
import mgo.echo.handler.game.service.HostService;
import mgo.echo.handler.game.service.HostSettingsService;
import mgo.echo.protocol.command.HostsCmd;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Error;
import mgo.echo.util.Guards;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Host operations controller.
 * Thin routing layer - delegates to HostService for logic.
 */
public class HostController implements Controller {

    // ========================================================================
    // Settings (0x4304, 0x4310)
    // ========================================================================

    @Command(0x4304)
    public boolean getSettings(CommandContext ctx) {
        User user = Guards.requireUser(ctx.nettyCtx(), HostsCmd.GET_SETTINGS_RESPONSE);
        if (user == null) {
            return true;
        }

        Character character = user.getCurrentCharacter();
        HostSettingsDto settings = HostSettingsService.getOrCreateSettings(user, character, ctx.lobby());
        HostSettingsPacket.writeResponse(ctx.nettyCtx(), settings);
        return true;
    }

    @Command(0x4310)
    public boolean checkSettings(CommandContext ctx) {
        User user = Guards.requireUser(ctx.nettyCtx(), HostsCmd.CHECK_SETTINGS_RESPONSE);
        if (user == null) {
            return true;
        }

        Character character = user.getCurrentCharacter();
        ByteBuf bi = ctx.packet().getPayload();
        HostSettingsDto settings = HostSettingsPacket.read(bi);

        if (settings.games.size() == 0) {
            Packets.writeError(ctx.nettyCtx(), HostsCmd.CHECK_SETTINGS_RESPONSE, 2);
            return true;
        }

        HostSettingsService.saveSettings(user, character, ctx.lobby(), settings);
        Packets.write(ctx.nettyCtx(), HostsCmd.CHECK_SETTINGS_RESPONSE, 0);
        return true;
    }

    // ========================================================================
    // Create Game (0x4316)
    // ========================================================================

    @Command(0x4316)
    public boolean createGame(CommandContext ctx) {
        try {
            User user = Guards.requireUser(ctx.nettyCtx(), HostsCmd.CREATE_GAME_RESPONSE);
            if (user == null) {
                return true;
            }

            Character character = user.getCurrentCharacter();
            HostService.CreateGameResult result = HostService.createGame(user, character, ctx.lobby());

            if (!result.success) {
                Packets.writeError(ctx.nettyCtx(), HostsCmd.CREATE_GAME_RESPONSE, result.errorCode);
                return true;
            }

            HostPackets.writeCreateGameResponse(ctx.nettyCtx(), result.gameId);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), HostsCmd.CREATE_GAME_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Player Connected (0x4340)
    // ========================================================================

    @Command(0x4340)
    public boolean playerConnected(CommandContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                Packets.write(ctx.nettyCtx(), HostsCmd.PLAYER_CONNECTED_RESPONSE, Error.INVALID_SESSION);
                return true;
            }

            Game game = requireHostGame(user, HostsCmd.PLAYER_CONNECTED_RESPONSE, ctx);
            if (game == null) {
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int targetId = bi.readInt();

            int result = GameService.gameAddPlayer(game, targetId, true);
            if (result < 0) {
                Packets.writeError(ctx.nettyCtx(), HostsCmd.PLAYER_CONNECTED_RESPONSE, 0xff + result);
                return true;
            }

            HostPackets.writePlayerIdResponse(ctx.nettyCtx(), HostsCmd.PLAYER_CONNECTED_RESPONSE, targetId);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), HostsCmd.PLAYER_CONNECTED_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Player Disconnected (0x4342)
    // ========================================================================

    @Command(0x4342)
    public boolean playerDisconnected(CommandContext ctx) {
        ByteBuf bi = ctx.packet().getPayload();
        int targetId = bi.readInt();

        // Send response first
        HostPackets.writePlayerIdResponse(ctx.nettyCtx(), HostsCmd.PLAYER_DISCONNECTED_RESPONSE, targetId);

        // Then process disconnection
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                return true;
            }

            Game game = getHostGame(user);
            if (game == null) {
                return true;
            }

            GameService.gameRemovePlayer(game, targetId, true);
        } catch (Exception e) {
            // Ignore - cleanup operation
        }

        return true;
    }

    // ========================================================================
    // Set Player Team (0x4344)
    // ========================================================================

    @Command(0x4344)
    public boolean setPlayerTeam(CommandContext ctx) {
        try {
            User user = Guards.requireUser(ctx.nettyCtx(), HostsCmd.SET_PLAYER_TEAM_RESPONSE);
            if (user == null) {
                return true;
            }

            Game game = requireHostGame(user, HostsCmd.SET_PLAYER_TEAM_RESPONSE, ctx);
            if (game == null) {
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int targetId = bi.readInt();
            int team = bi.readByte();

            int result = HostService.setPlayerTeam(game, targetId, team);
            if (result != 0) {
                Packets.writeError(ctx.nettyCtx(), HostsCmd.SET_PLAYER_TEAM_RESPONSE, result);
                return true;
            }

            HostPackets.writePlayerIdResponse(ctx.nettyCtx(), HostsCmd.SET_PLAYER_TEAM_RESPONSE, targetId);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), HostsCmd.SET_PLAYER_TEAM_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Kick Player (0x4346)
    // ========================================================================

    @Command(0x4346)
    public boolean kickPlayer(CommandContext ctx) {
        try {
            ByteBuf bi = ctx.packet().getPayload();
            if (bi.readableBytes() < 4) {
                Packets.write(ctx.nettyCtx(), HostsCmd.KICK_PLAYER_RESPONSE, Error.GENERAL);
                return true;
            }

            int targetId = bi.readInt();
            HostPackets.writePlayerIdResponse(ctx.nettyCtx(), HostsCmd.KICK_PLAYER_RESPONSE, targetId);
        } catch (Exception e) {
            // Ignore
        }

        return true;
    }

    // ========================================================================
    // Update Stats (0x4390)
    // ========================================================================

    @Command(0x4390)
    public boolean updateStats(CommandContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                return true;
            }

            Game game = getHostGame(user);
            if (game == null) {
                Packets.writeError(ctx.nettyCtx(), HostsCmd.UPDATE_STATS_RESPONSE, 2);
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int result = HostService.updateStats(game, bi);

            if (result != 0) {
                Packets.writeError(ctx.nettyCtx(), HostsCmd.UPDATE_STATS_RESPONSE, result);
                return true;
            }

            Packets.write(ctx.nettyCtx(), HostsCmd.UPDATE_STATS_RESPONSE, 0);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), HostsCmd.UPDATE_STATS_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Set Game (0x4392)
    // ========================================================================

    @Command(0x4392)
    public boolean setGame(CommandContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                return true;
            }

            Game game = getHostGame(user);
            if (game == null) {
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int index = bi.readByte();

            HostService.setGame(game, index);
            Packets.write(ctx.nettyCtx(), HostsCmd.SET_GAME_RESPONSE, 0);
        } catch (Exception e) {
            // Ignore
        }

        return true;
    }

    // ========================================================================
    // Update Game Environment (0x4394)
    // ========================================================================

    @Command(0x4394)
    public boolean updateGameEnvironment(CommandContext ctx) {
        // Not implemented
        return true;
    }

    // ========================================================================
    // Update Pings (0x4398)
    // ========================================================================

    @Command(0x4398)
    public boolean updatePings(CommandContext ctx) {
        try {
            Packets.write(ctx.nettyCtx(), HostsCmd.UPDATE_PINGS_RESPONSE, 0);

            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                return true;
            }

            Game game = getHostGame(user);
            if (game == null) {
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int hostPing = bi.readInt();

            HostService.updatePings(game, hostPing, bi);
            HostService.onPing(game);
        } catch (Exception e) {
            // Ignore
        }

        return true;
    }

    // ========================================================================
    // Pass Host (0x43a0)
    // ========================================================================

    @Command(0x43a0)
    public boolean pass(CommandContext ctx) {
        try {
            Packets.write(ctx.nettyCtx(), HostsCmd.PASS_RESPONSE, 0);

            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                return true;
            }

            Character character = user.getCurrentCharacter();
            Game game = getHostGame(user);
            if (game == null) {
                return true;
            }

            ByteBuf bi = ctx.packet().getPayload();
            int targetId = bi.readInt();

            HostService.passHost(game, character, targetId);
        } catch (Exception e) {
            // Ignore
        }

        return true;
    }

    // ========================================================================
    // Unknown 0x43a2
    // ========================================================================

    @Command(0x43a2)
    public boolean unknown43a2(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), 0x43a3, 0);
        return true;
    }

    // ========================================================================
    // Unknown 0x43c0
    // ========================================================================

    @Command(0x43c0)
    public boolean unknown43c0(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), 0x43c1);
        return true;
    }

    // ========================================================================
    // Start Round (0x43ca)
    // ========================================================================

    @Command(0x43ca)
    public boolean startRound(CommandContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.nettyCtx().channel());
            if (user == null) {
                Packets.write(ctx.nettyCtx(), HostsCmd.START_ROUND_RESPONSE, Error.INVALID_SESSION);
                return true;
            }

            Game game = getHostGame(user);
            if (game == null) {
                Packets.writeError(ctx.nettyCtx(), HostsCmd.START_ROUND_RESPONSE, 2);
                return true;
            }

            HostService.startRound(game);
            Packets.write(ctx.nettyCtx(), HostsCmd.START_ROUND_RESPONSE, 0);
        } catch (Exception e) {
            Packets.write(ctx.nettyCtx(), HostsCmd.START_ROUND_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private Game getHostGame(User user) {
        Character character = user.getCurrentCharacter();
        Player player = Util.getFirstOrNull(character.getPlayer());

        if (player == null) {
            return null;
        }

        Game game = player.getGame();
        if (!character.getId().equals(game.getHostId())) {
            return null;
        }

        return game;
    }

    private Game requireHostGame(User user, int errorCommand, CommandContext ctx) {
        Character character = user.getCurrentCharacter();
        Player player = Util.getFirstOrNull(character.getPlayer());

        if (player == null) {
            Packets.writeError(ctx.nettyCtx(), errorCommand, 3);
            return null;
        }

        Game game = player.getGame();
        if (!character.getId().equals(game.getHost().getId())) {
            Packets.writeError(ctx.nettyCtx(), errorCommand, 4);
            return null;
        }

        return game;
    }
}
