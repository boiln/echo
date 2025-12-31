package mgo.echo.controller;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mgo.echo.data.entity.Lobby;
import mgo.echo.protocol.command.HubCmd;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.session.ActiveLobbies;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Hub/Lobby operations controller.
 * 
 * Handles:
 * - Lobby Info
 * - Game Entry Info
 * - Lobby Disconnect
 * - Training Connect
 */
public class HubController implements Controller {
    private static final Logger logger = LogManager.getLogger(HubController.class);

    private static final byte[] TRAINING_BYTES = new byte[] {
            (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x15,
            (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x08,
            (byte) 0x00, (byte) 0x61
    };

    // ========================================================================
    // Lobby Info (0x4900)
    // ========================================================================

    @Command(0x4900)
    public boolean getGameLobbyInfo(CommandContext ctx) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();

        try {
            Collection<Lobby> lobbies = ActiveLobbies.get().values();
            Iterator<Lobby> iterator = lobbies.iterator();

            Packets.handleMutliElementPayload(ctx.nettyCtx(), lobbies.size(), 8, 0x23, payloads, (i, bo) -> {
                Lobby lobby = iterator.next();

                int unk1 = 0;
                int attributes = unk1 | ((lobby.getSubtype() & 0xff) << 24);
                int openTime = 0;
                int closeTime = 0;
                int isOpen = 1;

                bo.writeInt(i).writeInt(attributes).writeShort(lobby.getId());
                Util.writeString(lobby.getName(), 16, bo);
                bo.writeInt(openTime).writeInt(closeTime).writeByte(isOpen);
            });

            Packets.write(ctx.nettyCtx(), 0x4901, 0);
            Packets.write(ctx.nettyCtx(), 0x4902, payloads);
            Packets.write(ctx.nettyCtx(), 0x4903, 0);
        } catch (Exception e) {
            logger.error("Exception while getting game lobby info.", e);
            Util.releaseBuffers(payloads);
            Packets.write(ctx.nettyCtx(), 0x4901, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Game Entry Info (0x4990)
    // ========================================================================

    @Command(0x4990)
    public boolean getGameEntryInfo(CommandContext ctx) {
        ByteBuf bo = null;

        try {
            bo = ctx.alloc(0xac);
            bo.writeInt(0).writeInt(1).writeZero(0xa4);
            Packets.write(ctx.nettyCtx(), 0x4991, bo);
        } catch (Exception e) {
            logger.error("Exception while getting game entry info.", e);
            Packets.write(ctx.nettyCtx(), 0x4991, Error.GENERAL);
            Util.releaseBuffer(bo);
        }

        return true;
    }

    // ========================================================================
    // Lobby Disconnect (0x4150)
    // ========================================================================

    @Command(0x4150)
    public boolean onLobbyDisconnect(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), HubCmd.LOBBY_DISCONNECT_RESPONSE);
        return true;
    }

    // ========================================================================
    // Training Connect (0x43d0)
    // ========================================================================

    @Command(0x43d0)
    public boolean onTrainingConnect(CommandContext ctx) {
        ByteBuf bo = Unpooled.wrappedBuffer(TRAINING_BYTES);
        Packets.write(ctx.nettyCtx(), HubCmd.TRAINING_CONNECT_RESPONSE, bo);
        return true;
    }
}
