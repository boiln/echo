package mgo.echo.protocol.dispatch;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.User;
import mgo.echo.protocol.Packet;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;

/**
 * Context passed to command handlers.
 * Provides access to channel, packet, lobby, and user data.
 */
public final class CommandContext {
    private final ChannelHandlerContext nettyCtx;
    private final Packet packet;
    private final Lobby lobby;

    public CommandContext(ChannelHandlerContext nettyCtx, Packet packet, Lobby lobby) {
        this.nettyCtx = nettyCtx;
        this.packet = packet;
        this.lobby = lobby;
    }

    // ========================================================================
    // Netty Access
    // ========================================================================

    public ChannelHandlerContext nettyCtx() {
        return nettyCtx;
    }

    public Channel channel() {
        return nettyCtx.channel();
    }

    // ========================================================================
    // Packet Access
    // ========================================================================

    public Packet packet() {
        return packet;
    }

    public int command() {
        return packet.getCommand() & 0xffff;
    }

    public ByteBuf payload() {
        return packet.getPayload();
    }

    // ========================================================================
    // Lobby Access
    // ========================================================================

    public Lobby lobby() {
        return lobby;
    }

    // ========================================================================
    // User/Character Access
    // ========================================================================

    public User user() {
        return ActiveUsers.get(channel());
    }

    public Character character() {
        User user = user();
        return user != null ? user.getCurrentCharacter() : null;
    }

    public boolean hasUser() {
        return user() != null;
    }

    public boolean hasCharacter() {
        return character() != null;
    }

    // ========================================================================
    // Buffer Allocation
    // ========================================================================

    public ByteBuf alloc(int capacity) {
        return nettyCtx.alloc().directBuffer(capacity);
    }

    // ========================================================================
    // Response Writing
    // ========================================================================

    public void write(int command) {
        Packets.write(nettyCtx, command);
    }

    public void write(int command, int result) {
        Packets.write(nettyCtx, command, result);
    }

    public void write(int command, Error error) {
        Packets.write(nettyCtx, command, error);
    }

    public void write(int command, ByteBuf payload) {
        Packets.write(nettyCtx, command, payload);
    }

    public void flush() {
        nettyCtx.flush();
    }
}
