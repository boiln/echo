package mgo.echo.handler.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.google.gson.JsonArray;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.ConnectionInfo;
import mgo.echo.data.entity.EventConnectGame;
import mgo.echo.data.entity.EventDisconnectGame;
import mgo.echo.data.entity.EventEndGame;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.game.dto.JoinResult;
import mgo.echo.handler.social.ChatHandler;
import mgo.echo.session.ActiveGames;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Util;

/**
 * Business logic for game operations.
 * No Netty dependencies - pure domain logic.
 */
public class GameService {
    private static final Logger logger = LogManager.getLogger();

    // =========================================================================
    // Game List
    // =========================================================================

    public static List<Game> filterGames(Lobby lobby, int type) {
        Collection<Game> games = ActiveGames.getGames();
        ArrayList<Game> filtered = new ArrayList<>();

        for (Game game : games) {
            if (type == 0x200) {
                if (game.getName().startsWith("CLAN_ROOM_")) {
                    filtered.add(game);
                }
                continue;
            }

            if (game.getLobby() == lobby && !game.getName().startsWith("CLAN_ROOM_")) {
                filtered.add(game);
            }
        }

        return filtered;
    }

    // =========================================================================
    // Join Game
    // =========================================================================

    public static JoinResult joinGame(Character character, int gameId, String password) {
        Game game = ActiveGames.get(gameId);
        if (game == null) {
            logger.error("Error while joining game: No game.");
            return JoinResult.error(3);
        }

        Integer joinError = validateJoinRequest(character, game, password);
        if (joinError != null) {
            return JoinResult.error(joinError);
        }

        ConnectionInfo connectionInfo = Util.getFirstOrNull(game.getHost().getConnectionInfo());
        if (connectionInfo == null) {
            logger.error("Error while joining game: No connection info.");
            return JoinResult.error(4);
        }

        Player oldPlayer = Util.getFirstOrNull(character.getPlayer());
        if (oldPlayer != null) {
            gameRemovePlayer(game, character.getId(), false);
        }

        character.setGameJoining(game.getId());
        logger.info("{} ({}) is joining game: {}", character.getName(), character.getId(), character.getGameJoining());

        int[] currentMapRule = getCurrentMapRule(game);

        return JoinResult.success(
                connectionInfo.getPublicIp(),
                connectionInfo.getPublicPort(),
                connectionInfo.getPrivateIp(),
                connectionInfo.getPrivatePort(),
                currentMapRule[1],
                currentMapRule[0]);
    }

    private static Integer validateJoinRequest(Character character, Game game, String password) {
        Character host = game.getHost();
        boolean isBlocked = host.getBlocked().stream().anyMatch(e -> e.getTargetId().equals(character.getId()));

        if (isBlocked) {
            logger.error("Error while joining game: Blocked by host.");
            return 0x11;
        }

        if (game.getPassword() != null && !password.equals(game.getPassword())) {
            logger.error("Error while joining game: Bad password. User: {} Orig: {}", password, game.getPassword());
            return 3;
        }

        if (game.getPlayers().size() >= game.getMaxPlayers()) {
            logger.error("Error while joining game: Game is full.");
            return 0x10;
        }

        return null;
    }

    public static int[] getCurrentMapRule(Game game) {
        JsonArray jGames = Util.jsonDecodeArray(game.getGames());
        int currentGame = game.getCurrentGame();

        if (currentGame < jGames.size()) {
            JsonArray jGame = jGames.get(currentGame).getAsJsonArray();
            return new int[] { jGame.get(0).getAsInt(), jGame.get(1).getAsInt() };
        }

        return new int[] { 0, 0 };
    }

    // =========================================================================
    // Join Failed
    // =========================================================================

    public static void handleJoinFailed(Character character) {
        if (character.getGameJoining() == null) {
            return;
        }

        logger.info(
                "{} ({}) failed to join game: {}", character.getName(), character.getId(), character.getGameJoining());
        character.setGameJoining(null);
    }

    // =========================================================================
    // Quit Game
    // =========================================================================

    public static void quitGame(User user) {
        if (user == null) {
            logger.error("Error while quitting game: No user.");
            return;
        }

        Character character = user.getCurrentCharacter();
        Player player = Util.getFirstOrNull(character.getPlayer());
        if (player == null) {
            logger.error("Error while quitting game: Not in a game.");
            return;
        }

        Game game = player.getGame();
        boolean isHost = character.getId().equals(game.getHost().getId());

        if (isHost) {
            gameEnd(game);
            return;
        }

        gameRemovePlayer(game, character.getId(), true);
    }

    // =========================================================================
    // Cleanup
    // =========================================================================

    public static void cleanup() {
        ArrayList<Game> games = new ArrayList<>(ActiveGames.getGames());
        for (Game game : games) {
            int time = (int) Instant.now().getEpochSecond();
            int timeout = game.getLastUpdate() + 60;

            if (time <= timeout) {
                continue;
            }

            logger.info("Cleaning up Game {} ({}) - Host {}", game.getName(), game.getId(), game.getHostId());
            gameEnd(game);
        }
    }

    // =========================================================================
    // Player Management
    // =========================================================================

    public static int gameAddPlayer(Game game, int charaId, boolean checkJoining) {
        Session session = null;

        try {
            logger.info("GameAddPlayer {} ({}) - Char {}", game.getName(), game.getId(), charaId);

            synchronized (game.getPlayerLock()) {
                logger.info("GameAddPlayer {} ({}) - Char {} | Got lock.", game.getName(), game.getId(), charaId);

                User user = ActiveUsers.getByCharacterId(charaId);
                if (user == null) {
                    logger.error(
                            "GameAddPlayer {} ({}) - Char {} | User isn't online.",
                            game.getName(),
                            game.getId(),
                            charaId);
                    return -2;
                }

                Character character = user.getCurrentCharacter();
                if (character == null) {
                    logger.error(
                            "GameAddPlayer {} ({}) - Char {} | Character isn't online.",
                            game.getName(),
                            game.getId(),
                            charaId);
                    return -3;
                }

                if (checkJoining && !game.getId().equals(character.getGameJoining())) {
                    logger.error(
                            "GameAddPlayer {} ({}) - Char {} | Not joining this game.",
                            game.getName(),
                            game.getId(),
                            charaId);
                    return -4;
                }

                Player existingPlayer = Util.getFirstOrNull(character.getPlayer());
                int existingResult = handleExistingPlayer(game, character, existingPlayer, charaId);
                if (existingResult != 0) {
                    return existingResult;
                }

                Player newPlayer = createPlayer(character, game);
                savePlayer(newPlayer);

                game.addPlayer(newPlayer);
                character.getPlayer().add(newPlayer);
                character.setGameJoining(null);

                logConnectGameEvent(game.getId(), character.getId());
            }

            logger.info("GameAddPlayer {} ({}) - Char {} | Successfully added!", game.getName(), game.getId(), charaId);
            return 0;
        } catch (Exception e) {
            logger.error(
                    "GameAddPlayer {} ({}) - Char {} | Exception caught.", game.getName(), game.getId(), charaId, e);
            DbManager.rollbackAndClose(session);
            return -1;
        }
    }

    private static int handleExistingPlayer(Game game, Character character, Player existingPlayer, int charaId) {
        if (existingPlayer == null) {
            if (game.getPlayers().size() >= game.getMaxPlayers()) {
                logger.info("GameAddPlayer {} ({}) - Char {} | Game is full.", game.getName(), game.getId(), charaId);
                return -10;
            }
            return 0;
        }

        if (existingPlayer.getGame() == game) {
            logger.info("GameAddPlayer {} ({}) - Char {} | Already in game.", game.getName(), game.getId(), charaId);
            return 1;
        }

        logger.info(
                "GameAddPlayer {} ({}) - Char {} | Removing from old game: {} ({})",
                game.getName(),
                game.getId(),
                charaId,
                existingPlayer.getGame().getName(),
                existingPlayer.getGame().getId());

        removePlayerFromDb(existingPlayer);
        existingPlayer.getGame().removePlayer(existingPlayer);
        character.getPlayer().clear();

        return 0;
    }

    private static Player createPlayer(Character character, Game game) {
        Player player = new Player();
        player.setCharacter(character);
        player.setCharacterId(character.getId());
        player.setGame(game);
        player.setGameId(game.getId());
        player.setPing(0);
        player.setTeam(0);
        return player;
    }

    private static void savePlayer(Player player) {
        Session session = DbManager.getSession();
        session.beginTransaction();
        session.save(player);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }

    private static void removePlayerFromDb(Player player) {
        Session session = DbManager.getSession();
        session.beginTransaction();
        session.remove(player);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }

    private static void logConnectGameEvent(int gameId, int charaId) {
        EventConnectGame event = new EventConnectGame();
        event.setTime((int) Instant.now().getEpochSecond());
        event.setGameId(gameId);
        event.setCharaId(charaId);

        Session session = DbManager.getSession();
        session.beginTransaction();
        session.save(event);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }

    public static int gameRemovePlayer(Game game, int charaId, boolean checkGame) {
        Session session = null;

        try {
            logger.info("GameRemovePlayer {} ({}) - Char {}", game.getName(), game.getId(), charaId);

            synchronized (game.getPlayerLock()) {
                logger.info("GameRemovePlayer {} ({}) - Char {} | Got lock.", game.getName(), game.getId(), charaId);

                User user = ActiveUsers.getByCharacterId(charaId);
                if (user == null) {
                    logger.error(
                            "GameRemovePlayer {} ({}) - Char {} | User isn't online.",
                            game.getName(),
                            game.getId(),
                            charaId);
                    return -2;
                }

                Character character = user.getCurrentCharacter();
                if (character == null) {
                    logger.error(
                            "GameRemovePlayer {} ({}) - Char {} | Character isn't online.",
                            game.getName(),
                            game.getId(),
                            charaId);
                    return -3;
                }

                Player player = Util.getFirstOrNull(character.getPlayer());
                if (player == null) {
                    logger.error(
                            "GameRemovePlayer {} ({}) - Char {} | Character isn't in this game.",
                            game.getName(),
                            game.getId(),
                            charaId);
                    return 1;
                }

                if (checkGame && player.getGame() != game) {
                    logger.error(
                            "GameRemovePlayer {} ({}) - Char {} | Character isn't in this game.",
                            game.getName(),
                            game.getId(),
                            charaId);
                    return -4;
                }

                removePlayerFromDb(player);
                player.getGame().removePlayer(player);
                character.getPlayer().clear();

                logDisconnectGameEvent(game.getId(), charaId);
            }

            logger.info(
                    "GameRemovePlayer {} ({}) - Char {} | Successfully removed!",
                    game.getName(),
                    game.getId(),
                    charaId);
            return 0;
        } catch (Exception e) {
            logger.error(
                    "GameRemovePlayer {} ({}) - Char {} | Exception caught.", game.getName(), game.getId(), charaId, e);
            DbManager.rollbackAndClose(session);
            return -1;
        }
    }

    private static void logDisconnectGameEvent(int gameId, int charaId) {
        EventDisconnectGame event = new EventDisconnectGame();
        event.setTime((int) Instant.now().getEpochSecond());
        event.setGameId(gameId);
        event.setCharaId(charaId);

        Session session = DbManager.getSession();
        session.beginTransaction();
        session.save(event);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }

    // =========================================================================
    // Game End
    // =========================================================================

    public static int gameEnd(Game game) {
        Session session = null;

        try {
            logger.info("GameEnd {} ({})", game.getName(), game.getId());

            synchronized (game.getEndGameLock()) {
                logger.info("GameEnd {} ({}) | Got lock.", game.getName(), game.getId());

                if (!ActiveGames.exists(game)) {
                    return 0;
                }

                notifyPlayersOfGameEnd(game);
                removeAllPlayersFromGame(game);
                deleteGameFromDb(game);

                ActiveGames.remove(game);
                logEndGameEvent(game.getId());
            }

            logger.info("Ended Game {} ({}).", game.getName(), game.getId());
            return 0;
        } catch (Exception e) {
            logger.error("GameEnd {} ({}) | Exception caught.", game.getName(), game.getId(), e);
            DbManager.rollbackAndClose(session);
            return -1;
        }
    }

    private static void notifyPlayersOfGameEnd(Game game) {
        ChatHandler.sendServerMessageToGame("This game is being removed due to inactivity.", game);
        ChatHandler.sendServerMessageToGame("If this is in error, please report this to staff.", game);
    }

    private static void removeAllPlayersFromGame(Game game) {
        ArrayList<Player> playersCopy = new ArrayList<>(game.getPlayers());
        for (Player player : playersCopy) {
            gameRemovePlayer(game, player.getCharacterId(), true);
        }
    }

    private static void deleteGameFromDb(Game game) {
        Session session = DbManager.getSession();
        session.beginTransaction();
        session.remove(game);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }

    private static void logEndGameEvent(int gameId) {
        EventEndGame event = new EventEndGame();
        event.setTime((int) Instant.now().getEpochSecond());
        event.setGameId(gameId);

        Session session = DbManager.getSession();
        session.beginTransaction();
        session.save(event);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }
}
