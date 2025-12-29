package mgo.echo.handler.game.packet;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.handler.game.dto.GameSettings;
import mgo.echo.util.Util;

/**
 * Writes a single game list entry to a buffer.
 * Entry size: 0x37 bytes
 */
public final class GameListEntryPacket {

    private GameListEntryPacket() {
    }

    public static void write(ByteBuf bo, Game game, Character viewer) {
        JsonObject common = Util.jsonDecode(game.getCommon());
        List<Player> players = game.getPlayers();

        GameSettings settings = GameSettings.parse(common);
        int[] currentMapRule = getCurrentMapRule(game);

        int averageExperience = calculateAverageExperience(players);
        int friendBlockFlags = checkFriendsAndBlocked(viewer, players);

        int hostOptions = settings.buildHostOptions(game.getPassword() != null);
        int commonA = settings.buildCommonA();
        int commonB = settings.buildCommonB();

        bo.writeInt(game.getId());
        Util.writeString(game.getName(), 16, bo);
        bo.writeByte(hostOptions)
                .writeByte(0x8)
                .writeByte(currentMapRule[0])
                .writeByte(currentMapRule[1])
                .writeZero(1)
                .writeByte(game.getMaxPlayers())
                .writeByte(game.getStance())
                .writeByte(commonA)
                .writeByte(commonB)
                .writeByte(players.size())
                .writeInt(game.getPing())
                .writeByte(friendBlockFlags)
                .writeByte(settings.levelLimitTolerance)
                .writeInt(settings.levelLimitBase)
                .writeInt(averageExperience)
                .writeInt(game.getHost().getHostScore())
                .writeInt(game.getHost().getHostVotes())
                .writeZero(2)
                .writeByte(0x63);
    }

    private static int[] getCurrentMapRule(Game game) {
        JsonArray jGames = Util.jsonDecodeArray(game.getGames());
        int currentGame = game.getCurrentGame();

        if (currentGame < jGames.size()) {
            JsonArray jGame = jGames.get(currentGame).getAsJsonArray();
            return new int[] { jGame.get(0).getAsInt(), jGame.get(1).getAsInt() };
        }

        return new int[] { 0, 0 };
    }

    private static int calculateAverageExperience(List<Player> players) {
        if (players.isEmpty()) {
            return 0;
        }

        int totalExperience = 0;
        for (Player player : players) {
            totalExperience += getPlayerExperience(player);
        }

        return totalExperience / players.size();
    }

    private static int getPlayerExperience(Player player) {
        Character pCharacter = player.getCharacter();
        User pUser = pCharacter.getUser();

        if (pUser.getMainCharacterId() != null && pCharacter.getId().equals(pUser.getMainCharacterId())) {
            return pUser.getMainExp();
        }

        return pUser.getAltExp();
    }

    private static int checkFriendsAndBlocked(Character character, List<Player> players) {
        boolean hasFriend = false;
        boolean hasBlocked = false;

        for (Player player : players) {
            if (!hasFriend
                    && character.getFriends().stream()
                            .anyMatch(e -> e.getTargetId().equals(player.getCharacterId()))) {
                hasFriend = true;
            }
            if (!hasBlocked
                    && character.getBlocked().stream()
                            .anyMatch(e -> e.getTargetId().equals(player.getCharacterId()))) {
                hasBlocked = true;
            }
            if (hasFriend && hasBlocked) {
                break;
            }
        }

        int friendBlock = 0;
        friendBlock |= hasFriend ? 0b1 : 0;
        friendBlock |= hasBlocked ? 0b10 : 0;
        return friendBlock;
    }
}
