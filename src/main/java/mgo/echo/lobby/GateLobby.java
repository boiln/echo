package mgo.echo.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.controller.Controllers;
import mgo.echo.data.entity.Lobby;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.dispatch.RegistryDispatcher;

@Sharable
public class GateLobby extends BaseLobby {
    private static final Logger logger = LogManager.getLogger(GateLobby.class);

    private final RegistryDispatcher dispatcher;

    public GateLobby(Lobby lobby) {
        super(lobby);
        this.dispatcher = Controllers.createGateLobbyDispatcher();
    }

    @Override
    public boolean handlePacket(ChannelHandlerContext ctx, Packet in) {
        boolean handled = dispatcher.dispatch(ctx, in, getLobby());
        if (!handled) {
            logger.error("Couldn't handle command {}", Integer.toHexString(in.getCommand()));
        }
        return handled;
    }
}
