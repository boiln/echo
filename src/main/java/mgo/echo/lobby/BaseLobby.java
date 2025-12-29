package mgo.echo.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import mgo.echo.data.entity.Lobby;
import mgo.echo.protocol.Packet;
import mgo.echo.util.Packets;

public abstract class BaseLobby extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(BaseLobby.class);

    private Lobby lobby;

    public BaseLobby(Lobby lobby) {
        this.lobby = lobby;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public abstract boolean handlePacket(ChannelHandlerContext ctx, Packet in);

    public void onChannelActive(ChannelHandlerContext ctx) {

    }

    public void onChannelInactive(ChannelHandlerContext ctx) {

    }

    private final int handleCommonPacket(ChannelHandlerContext ctx, Packet in) {
        int command = in.getCommand();

        switch (command) {

            case 0x0003:
                ctx.close();
                return 0;

            case 0x0005:
                Packets.write(ctx, 0x0005);
                onPing(ctx);
                break;

            default:
                return -1;
        }

        return 1;
    }

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) {
        Packet in = (Packet) msg;

        boolean wrote = false;
        try {
            int result = handleCommonPacket(ctx, in);
            if (result == 1) {
                wrote = true;
            } else if (result < 0) {
                wrote = handlePacket(ctx, in);
            }
        } catch (Exception e) {
            logger.error("Exception while handling packet.", e);
        } finally {
            in.release();
        }

        if (wrote) {
            ctx.flush();
        }
    }

    public void onPing(ChannelHandlerContext ctx) {

    }

    @Override
    public final void channelActive(ChannelHandlerContext ctx) {
        logger.debug("Connection opened.");
        onChannelActive(ctx);
    }

    @Override
    public final void channelInactive(ChannelHandlerContext ctx) {
        logger.debug("Connection closed.");
        onChannelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            logger.debug("Connection timed out.");
            onChannelInactive(ctx);
        } else {
            logger.debug("Exception caught.", cause);
        }
    }
}
