package mgo.echo.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import mgo.echo.lobby.BaseLobby;
import mgo.echo.protocol.PacketDecoder;
import mgo.echo.protocol.PacketEncoder;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final PacketEncoder HANDLER_ENCODER = new PacketEncoder();
    private static final PacketDecoder HANDLER_DECODER = new PacketDecoder();

    private final BaseLobby lobby;
    private final EventExecutorGroup executorGroup;

    public ServerInitializer(BaseLobby lobby, EventExecutorGroup executorGroup) {
        this.lobby = lobby;
        this.executorGroup = executorGroup;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("encoder", HANDLER_ENCODER);
        pipeline.addLast("decoder", HANDLER_DECODER);
        pipeline.addLast("timeout", new ReadTimeoutHandler(60 * 2));
        pipeline.addLast(executorGroup, "lobby", lobby);
    }
}
