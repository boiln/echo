package mgo.echo.handler.game.packet;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.handler.game.dto.GameSettings;
import mgo.echo.handler.game.dto.RuleSettings;
import mgo.echo.handler.game.dto.WeaponRestrictions;
import mgo.echo.protocol.command.GamesCmd;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for game details response (0x4302).
 * Buffer size: 0x36d bytes
 */
public final class GameDetailsPacket {

    private static final int BUFFER_SIZE = 0x36d;

    private GameDetailsPacket() {
    }

    public static void write(ChannelHandlerContext ctx, Game game, Lobby lobby) {
        ByteBuf bo = ctx.alloc().directBuffer(BUFFER_SIZE);

        JsonObject common = Util.jsonDecode(game.getCommon());
        JsonObject rules = Util.jsonDecode(game.getRules());
        JsonArray jGames = Util.jsonDecodeArray(game.getGames());

        GameSettings settings = GameSettings.parse(common);
        RuleSettings ruleSettings = RuleSettings.parse(rules);
        WeaponRestrictions weapons = WeaponRestrictions.parse(common);

        List<Player> players = game.getPlayers();
        int numPlayers = players.size();
        int averageExperience = calculateAverageExperience(players);

        int commonA = settings.buildCommonA();
        int commonB = settings.buildCommonB();

        int hostOptionsExtraTimeFlags = ruleSettings.buildExtraTimeFlags();
        hostOptionsExtraTimeFlags |= settings.nonStat ? 0b10 : 0;

        byte[] wr = weapons.toBytes();

        // Header
        bo.writeInt(0).writeInt(game.getId());
        Util.writeString(game.getName(), 0x10, bo);
        Util.writeString(game.getComment(), 0x80, bo);
        bo.writeZero(2)
                .writeByte(lobby.getSubtype())
                .writeInt(averageExperience)
                .writeInt(game.getHost().getHostScore())
                .writeInt(game.getHost().getHostVotes())
                .writeByte(0x1);

        // Game rounds (map/rule/flags)
        for (JsonElement o : jGames) {
            JsonArray game0 = (JsonArray) o;
            int rule0 = game0.get(0).getAsInt();
            int map0 = game0.get(1).getAsInt();
            int flags0 = game0.get(2).getAsInt();
            bo.writeByte(rule0).writeByte(map0).writeByte(flags0);
        }

        Util.padTo(0xd5, bo);
        bo.writeZero(5)
                .writeBytes(wr)
                .writeByte(game.getMaxPlayers())
                .writeByte(numPlayers)
                .writeInt(settings.briefingTime)
                .writeZero(0x16)
                .writeByte(game.getStance())
                .writeByte(settings.levelLimitTolerance)
                .writeInt(0x16)
                .writeInt(ruleSettings.sneTime)
                .writeInt(ruleSettings.sneRounds)
                .writeInt(ruleSettings.capTime)
                .writeInt(ruleSettings.capRounds)
                .writeInt(ruleSettings.resTime)
                .writeInt(ruleSettings.resRounds)
                .writeInt(ruleSettings.tdmTime)
                .writeInt(ruleSettings.tdmRounds)
                .writeInt(ruleSettings.tdmTickets)
                .writeInt(ruleSettings.dmTime)
                .writeInt(ruleSettings.dmTickets)
                .writeInt(ruleSettings.baseTime)
                .writeInt(ruleSettings.baseRounds)
                .writeInt(ruleSettings.bombTime)
                .writeInt(ruleSettings.bombRounds)
                .writeInt(ruleSettings.tsneTime)
                .writeInt(ruleSettings.tsneRounds);

        // Uniques
        if (settings.uniquesRandom) {
            bo.writeByte(0x80 + settings.uniqueRed).writeByte(0x80 + settings.uniqueBlue);
        } else {
            bo.writeByte(settings.uniqueRed).writeByte(settings.uniqueBlue);
        }

        bo.writeZero(7)
                .writeByte(commonA)
                .writeByte(commonB)
                .writeZero(1)
                .writeShort(settings.idleKick)
                .writeShort(settings.teamKillKick)
                .writeInt(0x2e)
                .writeBoolean(ruleSettings.capExtraTime)
                .writeByte(ruleSettings.sneSnake)
                .writeByte(ruleSettings.sdmTime)
                .writeByte(ruleSettings.sdmRounds)
                .writeByte(ruleSettings.intTime)
                .writeByte(ruleSettings.dmRounds)
                .writeByte(ruleSettings.scapTime)
                .writeByte(ruleSettings.scapRounds)
                .writeByte(ruleSettings.raceTime)
                .writeByte(ruleSettings.raceRounds)
                .writeZero(1)
                .writeByte(hostOptionsExtraTimeFlags)
                .writeZero(4);

        // Host player info
        Player playerHost = players.stream().filter(e -> e.getCharacterId().equals(game.getHostId())).findAny()
                .orElse(null);
        if (playerHost != null) {
            writePlayerInfo(bo, playerHost);
        }

        // Other players
        for (Player player : players) {
            if (player == null || player == playerHost) {
                continue;
            }

            writePlayerInfo(bo, player);
        }

        Util.padTo(BUFFER_SIZE, bo);

        Packets.write(ctx, GamesCmd.GET_DETAILS_RESPONSE, bo);
    }

    private static void writePlayerInfo(ByteBuf bo, Player player) {
        bo.writeInt(player.getCharacterId());
        Util.writeString(player.getCharacter().getName(), 0x10, bo);
        bo.writeInt(player.getPing());

        Character pCharacter = player.getCharacter();
        User pUser = pCharacter.getUser();
        boolean isMain = pUser.getMainCharacterId() != null && pCharacter.getId().equals(pUser.getMainCharacterId());
        bo.writeInt(isMain ? pUser.getMainExp() : pUser.getAltExp());
    }

    private static int calculateAverageExperience(List<Player> players) {
        if (players.isEmpty()) {
            return 0;
        }

        int totalExperience = 0;
        for (Player player : players) {
            Character pCharacter = player.getCharacter();
            User pUser = pCharacter.getUser();
            boolean isMain = pUser.getMainCharacterId() != null
                    && pCharacter.getId().equals(pUser.getMainCharacterId());
            totalExperience += isMain ? pUser.getMainExp() : pUser.getAltExp();
        }

        return totalExperience / players.size();
    }
}
