package mgo.echo.handler.character.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Util;

/**
 * Packet serialization for friends/blocked list entries.
 * 
 * Entry format (0x2b bytes per entry):
 * - int32: targetId (4 bytes)
 * - string: targetName (16 bytes)
 * - int16: lobbyId (2 bytes)
 * - int32: gameId (4 bytes)
 * - string: gameHostName (16 bytes)
 * - byte: gameType (1 byte)
 * Total: 43 bytes (0x2b)
 * 
 * Add response format (0x19 bytes):
 * - int32: result (4 bytes)
 * - int32: targetId (4 bytes)
 * - byte: type (1 byte)
 * - string: targetName (16 bytes)
 * Total: 25 bytes (0x19)
 * 
 * Remove response format (0x9 bytes):
 * - int32: result (4 bytes)
 * - byte: type (1 byte)
 * - int32: targetId (4 bytes)
 * Total: 9 bytes (0x9)
 * 
 * Search result format (0x3b bytes per entry):
 * - int32: characterId (4 bytes)
 * - string: characterName (16 bytes)
 * - int16: lobbyId (2 bytes)
 * - string: lobbyName (16 bytes)
 * - int32: gameId (4 bytes)
 * - string: gameHostName (16 bytes)
 * - byte: gameType (1 byte)
 * Total: 59 bytes (0x3b)
 */
public class FriendsBlockedPacket {
    public static final int ENTRY_SIZE = 0x2b;
    public static final int ADD_RESPONSE_SIZE = 0x19;
    public static final int REMOVE_RESPONSE_SIZE = 0x9;
    public static final int SEARCH_ENTRY_SIZE = 0x3b;

    /**
     * Write a single friend/blocked list entry
     */
    public static void writeEntry(ByteBuf bo, int targetId, Character target) {
        GameInfo gameInfo = getGameInfo(targetId);

        bo.writeInt(targetId);
        Util.writeString(target.getName(), 16, bo);
        bo.writeShort(target.getLobby() != null ? target.getLobbyId() : 0);
        bo.writeInt(gameInfo.gameId);
        Util.writeString(gameInfo.hostName, 16, bo);
        bo.writeByte(gameInfo.gameType);
    }

    /**
     * Write add friends/blocked response
     */
    public static void writeAddResponse(ByteBuf bo, int targetId, int type, String targetName) {
        bo.writeInt(0);
        bo.writeInt(targetId);
        bo.writeByte(type);
        Util.writeString(targetName, 16, bo);
    }

    /**
     * Allocate and write add response buffer
     */
    public static ByteBuf createAddResponse(ChannelHandlerContext ctx, int targetId, int type, String targetName) {
        ByteBuf bo = ctx.alloc().directBuffer(ADD_RESPONSE_SIZE);
        writeAddResponse(bo, targetId, type, targetName);
        return bo;
    }

    /**
     * Write remove friends/blocked response
     */
    public static void writeRemoveResponse(ByteBuf bo, int type, int targetId) {
        bo.writeInt(0);
        bo.writeByte(type);
        bo.writeInt(targetId);
    }

    /**
     * Allocate and write remove response buffer
     */
    public static ByteBuf createRemoveResponse(ChannelHandlerContext ctx, int type, int targetId) {
        ByteBuf bo = ctx.alloc().directBuffer(REMOVE_RESPONSE_SIZE);
        writeRemoveResponse(bo, type, targetId);
        return bo;
    }

    /**
     * Write a search result entry
     */
    public static void writeSearchEntry(ByteBuf bo, Character character) {
        bo.writeInt(character.getId());
        Util.writeString(character.getName(), 16, bo);

        int lobbyId = 0;
        String lobbyName = "";
        if (character.getLobby() != null) {
            lobbyId = character.getLobbyId();
            lobbyName = character.getLobby().getName();
        }

        GameInfo gameInfo = getGameInfo(character.getId());

        bo.writeShort(lobbyId);
        Util.writeString(lobbyName, 16, bo);

        bo.writeInt(gameInfo.gameId);
        Util.writeString(gameInfo.hostName, 16, bo);
        bo.writeByte(gameInfo.gameType);
    }

    /**
     * Get game info for a character (if they're in a game)
     */
    private static GameInfo getGameInfo(int characterId) {
        User userTarget = ActiveUsers.getByCharacterId(characterId);
        if (userTarget == null) {
            return GameInfo.EMPTY;
        }

        Character characterTarget = userTarget.getCurrentCharacter();
        if (characterTarget == null) {
            return GameInfo.EMPTY;
        }

        Player player = Util.getFirstOrNull(characterTarget.getPlayer());
        if (player == null) {
            return GameInfo.EMPTY;
        }

        Game game = player.getGame();
        return new GameInfo(game.getId(), game.getHost().getName(), game.getLobby().getSubtype());
    }

    /**
     * Simple holder for game info
     */
    private static class GameInfo {
        static final GameInfo EMPTY = new GameInfo(0, "", 0);

        final int gameId;
        final String hostName;
        final int gameType;

        GameInfo(int gameId, String hostName, int gameType) {
            this.gameId = gameId;
            this.hostName = hostName;
            this.gameType = gameType;
        }
    }
}
