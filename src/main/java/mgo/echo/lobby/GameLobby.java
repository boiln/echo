package mgo.echo.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.controller.Controllers;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.handler.account.AccountHandler;
import mgo.echo.handler.game.service.HostService;
import mgo.echo.plugin.PluginHandler;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.dispatch.RegistryDispatcher;
import mgo.echo.session.ActiveUsers;

@Sharable
public class GameLobby extends BaseLobby {
    private static final Logger logger = LogManager.getLogger(GameLobby.class);

    private final RegistryDispatcher dispatcher;

    public GameLobby(Lobby lobby) {
        super(lobby);
        this.dispatcher = Controllers.createGameLobbyDispatcher();
    }

    @Override
    public boolean handlePacket(ChannelHandlerContext ctx, Packet in) {
        int result = PluginHandler.get().getPlugin().handleGameLobbyCommand(ctx, in);
        if (result == 1) {
            return true;
        }
        if (result == 0) {
            return false;
        }

        boolean handled = dispatcher.dispatch(ctx, in, getLobby());
        if (!handled) {
            logger.error("Couldn't handle command {}", Integer.toHexString(in.getCommand()));
        }
        return handled;
    }

    @Override
    public void onPing(ChannelHandlerContext ctx) {
        User user = ActiveUsers.get(ctx.channel());
        if (user == null) {
            return;
        }

        Character character = user.getCurrentCharacter();
        if (character == null) {
            return;
        }

        Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
        if (player == null) {
            return;
        }

        Game game = player.getGame();
        if (!character.getId().equals(game.getHostId())) {
            return;
        }

        HostService.onPing(game);
    }

    @Override
    public void onChannelInactive(ChannelHandlerContext ctx) {
        AccountHandler.onLobbyDisconnected(ctx, getLobby());
    }
}
