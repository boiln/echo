package mgo.echo.protocol.dispatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Lobby;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.dispatch.CommandRegistry.RegisteredHandler;

/**
 * Command dispatcher using the annotation-based registry.
 * 
 * Dispatches incoming packets to registered @Command handlers.
 */
public final class RegistryDispatcher {
    private static final Logger logger = LogManager.getLogger(RegistryDispatcher.class);

    private final CommandRegistry registry;

    public RegistryDispatcher(CommandRegistry registry) {
        this.registry = registry;
    }

    public boolean dispatch(ChannelHandlerContext ctx, Packet in, Lobby lobby) {
        int commandKey = in.getCommand() & 0xffff;

        RegisteredHandler handler = registry.get(commandKey);
        if (handler == null) {
            logger.warn("No handler for command 0x{}", Integer.toHexString(commandKey));
            return false;
        }

        CommandContext cmdCtx = new CommandContext(ctx, in, lobby);

        try {
            return handler.invoke(cmdCtx);
        } catch (Exception e) {
            logger.error("Exception handling command 0x{}", Integer.toHexString(commandKey), e);
            return false;
        }
    }

    public boolean hasHandler(int command) {
        return registry.has(command);
    }

    public int handlerCount() {
        return registry.size();
    }
}
