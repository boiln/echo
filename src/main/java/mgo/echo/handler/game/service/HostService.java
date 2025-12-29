package mgo.echo.handler.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterHostSettings;
import mgo.echo.data.entity.EventCreateGame;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.plugin.PluginHandler;
import mgo.echo.session.ActiveGames;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Util;

/**
 * Business logic for host operations.
 * No Netty dependencies - pure domain logic.
 */
public class HostService {
    private static final Logger logger = LogManager.getLogger();

    // =========================================================================
    // Create Game
    // =========================================================================

    /** Result of game creation. */
    public static class CreateGameResult {
        public final boolean success;
        public final Integer gameId;
        public final Integer errorCode;

        private CreateGameResult(boolean success, Integer gameId, Integer errorCode) {
            this.success = success;
            this.gameId = gameId;
            this.errorCode = errorCode;
        }

        public static CreateGameResult success(int gameId) {
            return new CreateGameResult(true, gameId, null);
        }

        public static CreateGameResult error(int errorCode) {
            return new CreateGameResult(false, null, errorCode);
        }
    }

    public static CreateGameResult createGame(User user, Character character, Lobby lobby) {
        List<CharacterHostSettings> settingsList = character.getHostSettings();
        if (settingsList == null) {
            settingsList = new ArrayList<>();
            character.setHostSettings(settingsList);
        }

        String sessionHostSettings = user.getSessionHostSettings();
        JsonObject settings = Util.jsonDecode(sessionHostSettings);

        String name = settings.get("name").getAsString();
        String password = settings.get("password") != null && !settings.get("password").isJsonNull()
                ? settings.get("password").getAsString()
                : null;
        int stance = settings.get("stance").getAsInt();
        String comment = settings.get("comment").getAsString();

        JsonArray games = settings.get("games").getAsJsonArray();
        JsonObject common = settings.get("common").getAsJsonObject();
        JsonObject ruleSettings = settings.get("ruleSettings").getAsJsonObject();

        int maxPlayers = common.get("maxPlayers").getAsInt();
        String jsonGames = Util.jsonEncode(games);
        String jsonCommon = Util.jsonEncode(common);
        String jsonRuleSettings = Util.jsonEncode(ruleSettings);

        Game game = buildNewGame(
                character, lobby, name, password, comment, stance, maxPlayers, jsonGames, jsonCommon, jsonRuleSettings);

        DbManager.txVoid(session -> session.save(game));

        game.initPlayers();
        GameService.gameAddPlayer(game, character.getId(), false);
        ActiveGames.add(game);

        logGameCreationEvent(character, lobby, game, name);

        logger.info("Created Game {} ({}).", game.getName(), game.getId());
        return CreateGameResult.success(game.getId());
    }

    private static Game buildNewGame(
            Character character,
            Lobby lobby,
            String name,
            String password,
            String comment,
            int stance,
            int maxPlayers,
            String jsonGames,
            String jsonCommon,
            String jsonRuleSettings) {
        Game game = new Game();
        game.setHostId(character.getId());
        game.setHost(character);
        game.setLobbyId(lobby.getId());
        game.setLobby(lobby);
        game.setName(name);
        game.setPassword(password);
        game.setComment(comment);
        game.setMaxPlayers(maxPlayers);
        game.setGames(jsonGames);
        game.setCommon(jsonCommon);
        game.setRules(jsonRuleSettings);
        game.setStance(stance);
        game.setCurrentGame(0);
        game.setLastUpdate((int) Instant.now().getEpochSecond());
        return game;
    }

    private static void logGameCreationEvent(Character character, Lobby lobby, Game game, String name) {
        EventCreateGame event = new EventCreateGame();
        event.setTime((int) Instant.now().getEpochSecond());
        event.setHostId(character.getId());
        event.setLobbyId(lobby.getId());
        event.setGameId(game.getId());
        event.setName(name);

        DbManager.txVoid(session -> session.save(event));
    }

    // =========================================================================
    // Set Player Team
    // =========================================================================

    public static int setPlayerTeam(Game game, int targetId, int team) {
        Player targetPlayer = game.getPlayerByCharacterId(targetId);
        if (targetPlayer == null) {
            logger.error("Error while setting player team: Couldn't find player.");
            return 1;
        }

        targetPlayer.setTeam(team);
        DbManager.txVoid(session -> session.update(targetPlayer));
        return 0;
    }

    // =========================================================================
    // Update Pings
    // =========================================================================

    public static void updatePings(Game game, int hostPing, ByteBuf bi) {
        game.setLastUpdate((int) Instant.now().getEpochSecond());

        while (bi.readableBytes() >= 8) {
            int targetId = bi.readInt();
            int targetPing = bi.readInt();

            if (targetId == 0) {
                continue;
            }

            Player target = game.getPlayerByCharacterId(targetId);
            if (target != null) {
                target.setPing(targetPing);
            }
        }

        game.setPing(hostPing);
    }

    // =========================================================================
    // Update Stats
    // =========================================================================

    public static int updateStats(Game game, ByteBuf bi) {
        int gameMode = StatsService.getGameModeFromGame(game);
        logger.debug(
                "UpdateStats: game={}, currentGame={}, gameMode={}", game.getName(), game.getCurrentGame(), gameMode);

        // DEBUG: Log raw stats packet for analysis
        if (logger.isDebugEnabled()) {
            int readable = bi.readableBytes();
            byte[] rawBytes = new byte[readable];
            bi.getBytes(bi.readerIndex(), rawBytes);
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < rawBytes.length; i++) {
                hex.append(String.format("%02x", rawBytes[i]));
                if ((i + 1) % 32 == 0) {
                    hex.append("\n");
                } else if ((i + 1) % 4 == 0) {
                    hex.append(" ");
                }
            }
            logger.debug("0x4390 UpdateStats raw packet ({} bytes):\n{}", readable, hex.toString());
        }

        int targetId = bi.readInt();
        StatsService.RoundStats roundStats = StatsService.parseStatsPacket(bi, gameMode);

        logger.debug(
                "Parsed stats for target {}: kills={}, deaths={}, stuns={}, stunsRec={}, "
                        + "hsKills={}, hsDeaths={}, hsStuns={}, hsStunsRec={}, consKills={}, score={}",
                targetId,
                roundStats.kills,
                roundStats.deaths,
                roundStats.stuns,
                roundStats.stunsReceived,
                roundStats.headshotKills,
                roundStats.headshotDeaths,
                roundStats.headshotStuns,
                roundStats.headshotStunsReceived,
                roundStats.consecutiveKills,
                roundStats.score);

        // Try to find player in game
        Player targetPlayer = game.getPlayerByCharacterId(targetId);
        if (targetPlayer != null) {
            return updateStatsForPlayer(targetPlayer.getCharacter(), roundStats);
        }

        // Check if player was in last round
        List<Integer> playersLastRound = game.getPlayersLastRound();
        if (!playersLastRound.contains(targetId)) {
            logger.error("Error while updating stats: Player didn't play this round.");
            return 3;
        }

        // Try to find online user
        User targetUser = ActiveUsers.getByCharacterId(targetId);
        if (targetUser != null) {
            return updateStatsForPlayer(targetUser.getCurrentCharacter(), roundStats);
        }

        // Fallback to database lookup
        return updateStatsFromDb(targetId, roundStats);
    }

    private static int updateStatsForPlayer(Character targetCharacter, StatsService.RoundStats roundStats) {
        User targetUser = targetCharacter.getUser();

        Session session = DbManager.getSession();
        session.beginTransaction();

        StatsService.updatePlayerExperience(session, targetUser, targetCharacter, roundStats.experience,
                roundStats.aborted);
        StatsService.updateCharacterStats(session, targetCharacter, roundStats);

        session.getTransaction().commit();
        DbManager.closeSession(session);
        return 0;
    }

    private static int updateStatsFromDb(int targetId, StatsService.RoundStats roundStats) {
        Session session = DbManager.getSession();
        session.beginTransaction();

        Character targetCharacter = session.get(Character.class, targetId);
        if (targetCharacter == null) {
            logger.error("Error while updating stats: Character doesn't exist.");
            session.getTransaction().commit();
            DbManager.closeSession(session);
            return 0;
        }

        User targetUser = targetCharacter.getUser();
        int currentExp = StatsService.getCurrentExperience(targetUser, targetCharacter);
        int finalExp = StatsService.calculateFinalExperience(currentExp, roundStats.experience, roundStats.aborted);

        User aUser = session.get(User.class, targetUser.getId());
        StatsService.setExperience(aUser, targetCharacter, finalExp);

        StatsService.updateCharacterStats(session, targetCharacter, roundStats);

        session.getTransaction().commit();
        DbManager.closeSession(session);
        return 0;
    }

    // =========================================================================
    // Set Game (current round index)
    // =========================================================================

    public static void setGame(Game game, int index) {
        game.setCurrentGame(index);

        Session session = DbManager.getSession();
        session.beginTransaction();
        session.update(game);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }

    // =========================================================================
    // Pass Host
    // =========================================================================

    public static boolean passHost(Game game, Character currentHost, int targetId) {
        Player targetPlayer = game.getPlayerByCharacterId(targetId);
        if (targetPlayer == null) {
            logger.error("Error while passing game: Couldn't find player.");
            return false;
        }

        Character target = targetPlayer.getCharacter();
        game.setHostId(target.getId());
        game.setHost(target);

        GameService.gameRemovePlayer(game, currentHost.getId(), false);

        Session session = DbManager.getSession();
        session.beginTransaction();
        session.update(game);
        session.getTransaction().commit();
        DbManager.closeSession(session);
        return true;
    }

    // =========================================================================
    // Start Round
    // =========================================================================

    public static void startRound(Game game) {
        List<Integer> playersLastRound = game.getPlayersLastRound();
        playersLastRound.clear();

        for (Player targetPlayer : game.getPlayers()) {
            playersLastRound.add(targetPlayer.getCharacterId());
        }
    }

    // =========================================================================
    // On Ping (N-Check)
    // =========================================================================

    public static void onPing(Game game) {
        int time = (int) Instant.now().getEpochSecond();

        if (time < game.getLastNCheck() + 60) {
            return;
        }

        PluginHandler.get().getPlugin().gameNCheck(game);
        game.setLastNCheck(time);
    }
}
