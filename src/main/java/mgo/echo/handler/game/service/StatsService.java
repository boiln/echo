package mgo.echo.handler.game.service;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterStats;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.User;
import mgo.echo.util.Util;

/**
 * Service for handling game statistics parsing and persistence
 */
public class StatsService {
    private static final Logger logger = LogManager.getLogger();

    /**
     * Data class to hold parsed stats from the 0x4390 packet
     */
    public static class RoundStats {
        public int kills;
        public int deaths;
        public int stuns;
        public int stunsReceived;
        public int stunsFriendly;
        public int headshotKills;
        public int headshotDeaths;
        public int headshotStuns;
        public int headshotStunsReceived;
        public int lockKills;
        public int lockDeaths;
        public int lockStuns;
        public int lockStunsReceived;
        public int consecutiveKills;
        public int consecutiveDeaths;
        public int consecutiveHeadshots;
        public int suicides;
        public int score;
        public int experience;
        public int rolls;
        public int cqcGiven;
        public int cqcTaken;
        public int knifeKills;
        public int knifeStuns;
        public int teamKills;
        public int melee;
        public int meleeRec;
        public int radio;
        public int chat;
        public int salutes;
        public int spotted;
        public int selfSpotted;
        public int basesCaptured;
        public int basesDestroyed;
        public int bombDisarms;
        public int gakoSaved;
        public int gakoDefended;
        public int gakoFirst;
        public int raceCheckpoints;
        public int boxUses;
        public int boxTime;
        public int time;
        public int pointsAssist;
        public int pointsBase;
        public int wakeups;
        public int boosts;
        public int scans;
        public int evgTime;
        public int wins;
        public boolean aborted;
        public int gameMode;
    }

    /**
     * Get game mode (rule) from the game's JSON config
     * games is a JSON array like [[rule, map, flags], ...]
     * currentGame is the index into that array
     */
    public static int getGameModeFromGame(Game game) {
        try {
            String gamesJson = game.getGames();
            int currentIndex = game.getCurrentGame();

            if (gamesJson == null || gamesJson.isEmpty()) {
                return 0;
            }

            JsonArray games = Util.jsonDecodeArray(gamesJson);
            if (currentIndex < 0 || currentIndex >= games.size()) {
                return 0;
            }

            JsonArray currentGame = games.get(currentIndex).getAsJsonArray();
            return currentGame.get(0).getAsInt(); // rule is first element
        } catch (Exception e) {
            logger.warn("Failed to get game mode from game config: {}", e.getMessage());
        }
        return 0; // default to DM
    }

    /**
     * Parse the 0x4390 stats packet payload
     */
    public static RoundStats parseStatsPacket(ByteBuf bi, int gameMode) {
        RoundStats stats = new RoundStats();
        stats.gameMode = gameMode;

        int readable = bi.readableBytes();
        if (readable < 30) {
            logger.warn("Stats packet too short: {} bytes", readable);
            return stats;
        }

        // Log non-zero shorts for debugging
        if (logger.isDebugEnabled()) {
            StringBuilder nonZero = new StringBuilder("Non-zero LE shorts: ");
            for (int i = 0; i < Math.min(readable, 100); i += 2) {
                int val = (bi.getByte(i) & 0xFF) | ((bi.getByte(i + 1) & 0xFF) << 8);
                if (val != 0) {
                    nonZero.append(String.format("[%d]=%d ", i, val));
                }
            }
            logger.debug(nonZero.toString());
        }

        stats.wins = bi.getByte(4) & 0xFF;
        stats.kills = bi.getByte(6) & 0xFF;
        stats.deaths = bi.getByte(8) & 0xFF;
        stats.stunsReceived = bi.getByte(10) & 0xFF;
        stats.score = bi.getByte(12) & 0xFF;
        stats.stuns = bi.getByte(14) & 0xFF;
        stats.headshotKills = bi.getByte(18) & 0xFF;
        stats.headshotDeaths = bi.getByte(20) & 0xFF;
        stats.headshotStuns = bi.getByte(22) & 0xFF;
        stats.headshotStunsReceived = bi.getByte(24) & 0xFF;
        stats.lockKills = bi.getByte(40) & 0xFF;
        stats.lockDeaths = bi.getByte(42) & 0xFF;
        stats.lockStuns = bi.getByte(44) & 0xFF;
        stats.lockStunsReceived = bi.getByte(46) & 0xFF;

        if (readable > 38) {
            stats.rolls = bi.getByte(32) & 0xFF;
            stats.time = bi.getByte(38) & 0xFF;
        }
        if (readable > 48) {
            stats.consecutiveKills = bi.getByte(48) & 0xFF;
        }

        bi.readerIndex(bi.readerIndex() + readable);
        return stats;
    }

    /**
     * Get current experience for a character
     */
    public static int getCurrentExperience(User user, Character character) {
        if (user.getMainCharacterId() != null && character.getId().equals(user.getMainCharacterId())) {
            return user.getMainExp();
        }

        return user.getAltExp();
    }

    /**
     * Set experience for a character
     */
    public static void setExperience(User user, Character character, int experience) {
        boolean isMain = user.getMainCharacterId() != null && character.getId().equals(user.getMainCharacterId());

        if (isMain) {
            user.setMainExp(experience);
            return;
        }

        user.setAltExp(experience);
    }

    /**
     * Calculate final experience after a round
     */
    public static int calculateFinalExperience(int currentExp, int experience, boolean aborted) {
        return aborted ? Math.max(0, currentExp - 60) : experience;
    }

    /**
     * Update player experience in database
     */
    public static void updatePlayerExperience(Session session, User targetUser, Character targetCharacter,
            int experience, boolean aborted) {
        int currentExp = getCurrentExperience(targetUser, targetCharacter);
        int finalExp = calculateFinalExperience(currentExp, experience, aborted);

        User aUser = session.get(User.class, targetUser.getId());
        setExperience(aUser, targetCharacter, finalExp);
        setExperience(targetUser, targetCharacter, finalExp);
    }

    /**
     * Update character stats in database
     */
    public static void updateCharacterStats(Session session, Character targetCharacter, RoundStats roundStats) {
        CharacterStats stats = session.createQuery(
                "FROM CharacterStats WHERE charaId = :charaId", CharacterStats.class)
                .setParameter("charaId", targetCharacter.getId())
                .uniqueResultOptional()
                .orElse(null);

        if (stats == null) {
            stats = new CharacterStats();
            stats.setCharacter(targetCharacter);
        }

        // Add round stats to totals
        addCombatStats(stats, roundStats);
        addCommunicationStats(stats, roundStats);
        addGameModeStats(stats, roundStats);
        addEquipmentStats(stats, roundStats);
        addPointsAndTime(stats, roundStats);

        // Update per-mode JSON stats
        updateModeStats(stats, roundStats);

        // Update timestamp
        stats.setLastUpdated((int) Instant.now().getEpochSecond());

        session.saveOrUpdate(stats);

        logger.info("Updated stats for character {}: +{} kills, +{} deaths, +{} score (mode={})",
                targetCharacter.getName(), roundStats.kills, roundStats.deaths, roundStats.score, roundStats.gameMode);
    }

    /**
     * Add combat-related stats (kills, deaths, stuns, headshots, etc.)
     */
    private static void addCombatStats(CharacterStats stats, RoundStats roundStats) {
        stats.addKills(roundStats.kills);
        stats.addDeaths(roundStats.deaths);
        stats.addStuns(roundStats.stuns);
        stats.addStunsReceived(roundStats.stunsReceived);
        stats.setStunsFriendly(stats.getStunsFriendly() + roundStats.stunsFriendly);
        stats.addHeadshotKills(roundStats.headshotKills);
        stats.addHeadshotDeaths(roundStats.headshotDeaths);
        stats.setHeadshotStuns(stats.getHeadshotStuns() + roundStats.headshotStuns);
        stats.setHeadshotStunsReceived(stats.getHeadshotStunsReceived() + roundStats.headshotStunsReceived);
        stats.setLockKills(stats.getLockKills() + roundStats.lockKills);
        stats.setLockDeaths(stats.getLockDeaths() + roundStats.lockDeaths);
        stats.setLockStuns(stats.getLockStuns() + roundStats.lockStuns);
        stats.setLockStunsReceived(stats.getLockStunsReceived() + roundStats.lockStunsReceived);

        stats.updateConsecutiveKills(roundStats.consecutiveKills);
        stats.updateConsecutiveDeaths(roundStats.consecutiveDeaths);
        stats.updateConsecutiveHeadshots(roundStats.consecutiveHeadshots);

        stats.setSuicides(stats.getSuicides() + roundStats.suicides);
        stats.setTeamKills(stats.getTeamKills() + roundStats.teamKills);
        stats.setRolls(stats.getRolls() + roundStats.rolls);
        stats.setCqcGiven(stats.getCqcGiven() + roundStats.cqcGiven);
        stats.setCqcTaken(stats.getCqcTaken() + roundStats.cqcTaken);
        stats.setKnifeKills(stats.getKnifeKills() + roundStats.knifeKills);
        stats.setKnifeStuns(stats.getKnifeStuns() + roundStats.knifeStuns);
        stats.setMelee(stats.getMelee() + roundStats.melee);
        stats.setMeleeRec(stats.getMeleeRec() + roundStats.meleeRec);
    }

    /**
     * Add communication stats (radio, chat, salutes, spotting)
     */
    private static void addCommunicationStats(CharacterStats stats, RoundStats roundStats) {
        stats.setRadio(stats.getRadio() + roundStats.radio);
        stats.setChat(stats.getChat() + roundStats.chat);
        stats.setSalutes(stats.getSalutes() + roundStats.salutes);
        stats.setSpotted(stats.getSpotted() + roundStats.spotted);
        stats.setSelfSpotted(stats.getSelfSpotted() + roundStats.selfSpotted);
    }

    /**
     * Add game mode specific stats (bases, bombs, gako, race)
     */
    private static void addGameModeStats(CharacterStats stats, RoundStats roundStats) {
        stats.setBasesCaptured(stats.getBasesCaptured() + roundStats.basesCaptured);
        stats.setBasesDestroyed(stats.getBasesDestroyed() + roundStats.basesDestroyed);
        stats.setBombDisarms(stats.getBombDisarms() + roundStats.bombDisarms);
        stats.setGakoSaved(stats.getGakoSaved() + roundStats.gakoSaved);
        stats.setGakoDefended(stats.getGakoDefended() + roundStats.gakoDefended);
        stats.setGakoFirst(stats.getGakoFirst() + roundStats.gakoFirst);
        stats.setRaceCheckpoints(stats.getRaceCheckpoints() + roundStats.raceCheckpoints);
    }

    /**
     * Add equipment usage stats (box, boosts, scans, etc.)
     */
    private static void addEquipmentStats(CharacterStats stats, RoundStats roundStats) {
        stats.setBoxUses(stats.getBoxUses() + roundStats.boxUses);
        stats.setBoxTime(stats.getBoxTime() + roundStats.boxTime);
        stats.setBoosts(stats.getBoosts() + roundStats.boosts);
        stats.setScans(stats.getScans() + roundStats.scans);
        stats.setEvgTime(stats.getEvgTime() + roundStats.evgTime);
        stats.setWakeups(stats.getWakeups() + roundStats.wakeups);
    }

    /**
     * Add points and time stats
     */
    private static void addPointsAndTime(CharacterStats stats, RoundStats roundStats) {
        stats.setPointsAssist(stats.getPointsAssist() + roundStats.pointsAssist);
        stats.setPointsBase(stats.getPointsBase() + roundStats.pointsBase);
        stats.addScore(roundStats.score);
        stats.addTime(roundStats.time);
        stats.addRounds(1);
        stats.addWins(roundStats.wins);
    }

    /**
     * Update per-game-mode JSON stats
     */
    private static void updateModeStats(CharacterStats stats, RoundStats roundStats) {
        try {
            String currentJson = stats.getStatsByMode(roundStats.gameMode);
            JsonObject modeStats = createOrLoadModeStats(currentJson);

            modeStats.addProperty("wins", getJsonInt(modeStats, "wins") + roundStats.wins);
            modeStats.addProperty("rounds", getJsonInt(modeStats, "rounds") + 1);
            modeStats.addProperty("score", getJsonInt(modeStats, "score") + roundStats.score);
            modeStats.addProperty("time", getJsonInt(modeStats, "time") + roundStats.time);
            modeStats.addProperty("kills", getJsonInt(modeStats, "kills") + roundStats.kills);
            modeStats.addProperty("deaths", getJsonInt(modeStats, "deaths") + roundStats.deaths);
            modeStats.addProperty("stuns", getJsonInt(modeStats, "stuns") + roundStats.stuns);
            modeStats.addProperty("stunsRec", getJsonInt(modeStats, "stunsRec") + roundStats.stunsReceived);
            modeStats.addProperty("hsKills", getJsonInt(modeStats, "hsKills") + roundStats.headshotKills);
            modeStats.addProperty("hsDeaths", getJsonInt(modeStats, "hsDeaths") + roundStats.headshotDeaths);
            modeStats.addProperty("hsStuns", getJsonInt(modeStats, "hsStuns") + roundStats.headshotStuns);
            modeStats.addProperty("hsStunsRec", getJsonInt(modeStats, "hsStunsRec") + roundStats.headshotStunsReceived);
            modeStats.addProperty("lockKills", getJsonInt(modeStats, "lockKills") + roundStats.lockKills);
            modeStats.addProperty("lockDeaths", getJsonInt(modeStats, "lockDeaths") + roundStats.lockDeaths);
            modeStats.addProperty("lockStuns", getJsonInt(modeStats, "lockStuns") + roundStats.lockStuns);
            modeStats.addProperty("lockStunsRec", getJsonInt(modeStats, "lockStunsRec") + roundStats.lockStunsReceived);

            stats.setStatsByMode(roundStats.gameMode, Util.jsonEncode(modeStats));

            logger.debug("Updated mode {} stats: kills={}, deaths={}, score={}",
                    roundStats.gameMode, getJsonInt(modeStats, "kills"),
                    getJsonInt(modeStats, "deaths"), getJsonInt(modeStats, "score"));
        } catch (Exception e) {
            logger.warn("Failed to update mode stats: {}", e.getMessage());
        }
    }

    /**
     * Create or load mode-specific stats from JSON
     */
    private static JsonObject createOrLoadModeStats(String currentJson) {
        if (currentJson != null && !currentJson.isEmpty()) {
            return Util.jsonDecode(currentJson);
        }

        JsonObject modeStats = new JsonObject();
        modeStats.addProperty("wins", 0);
        modeStats.addProperty("rounds", 0);
        modeStats.addProperty("score", 0);
        modeStats.addProperty("time", 0);
        modeStats.addProperty("kills", 0);
        modeStats.addProperty("deaths", 0);
        modeStats.addProperty("stuns", 0);
        modeStats.addProperty("stunsRec", 0);
        modeStats.addProperty("hsKills", 0);
        modeStats.addProperty("hsDeaths", 0);
        modeStats.addProperty("hsStuns", 0);
        modeStats.addProperty("hsStunsRec", 0);
        modeStats.addProperty("lockKills", 0);
        modeStats.addProperty("lockDeaths", 0);
        modeStats.addProperty("lockStuns", 0);
        modeStats.addProperty("lockStunsRec", 0);

        return modeStats;
    }

    /**
     * Get an integer value from JSON, defaulting to 0 if not found
     */
    private static int getJsonInt(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return 0;
        }

        return json.get(key).getAsInt();
    }
}
