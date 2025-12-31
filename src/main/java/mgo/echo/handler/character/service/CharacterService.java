package mgo.echo.handler.character.service;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterAppearance;
import mgo.echo.data.entity.CharacterEquippedSkills;
import mgo.echo.data.entity.CharacterStats;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.ConnectionInfo;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;

/**
 * Service for character business logic.
 * Handles appearance, skills, and connection info.
 */
public class CharacterService {
    /**
     * DTO for personal info update data
     */
    public static class PersonalInfoUpdate {
        public int upper, lower, facePaint, upperColor, lowerColor;
        public int head, chest, hands, waist, feet;
        public int accessory1, accessory2;
        public int headColor, chestColor, handsColor, waistColor, feetColor;
        public int accessory1Color, accessory2Color;
        public int skill1, skill2, skill3, skill4;
        public int level1, level2, level3, level4;
        public String comment;

        /**
         * Clamp all values to valid unsigned byte range (0-255)
         */
        public void validate() {
            upper = clamp(upper);
            lower = clamp(lower);
            facePaint = clamp(facePaint);
            upperColor = clamp(upperColor);
            lowerColor = clamp(lowerColor);
            head = clamp(head);
            chest = clamp(chest);
            hands = clamp(hands);
            waist = clamp(waist);
            feet = clamp(feet);
            accessory1 = clamp(accessory1);
            accessory2 = clamp(accessory2);
            headColor = clamp(headColor);
            chestColor = clamp(chestColor);
            handsColor = clamp(handsColor);
            waistColor = clamp(waistColor);
            feetColor = clamp(feetColor);
            accessory1Color = clamp(accessory1Color);
            accessory2Color = clamp(accessory2Color);
        }

        /**
         * Clamp value to 0-255 range
         */
        private static int clamp(int value) {
            return Math.max(0, Math.min(255, value));
        }
    }

    /**
     * Get or create equipped skills for a character
     */
    public static CharacterEquippedSkills getOrCreateSkills(Character character) {
        List<CharacterEquippedSkills> skillsList = character.getSkills();

        if (skillsList.isEmpty()) {
            CharacterEquippedSkills skills = new CharacterEquippedSkills();
            skills.setCharacter(character);
            skillsList.add(skills);
            return skills;
        }

        return skillsList.get(0);
    }

    /**
     * Get or create connection info for a character
     */
    public static ConnectionInfo getOrCreateConnectionInfo(Character character) {
        List<ConnectionInfo> connectionInfos = character.getConnectionInfo();

        if (connectionInfos.isEmpty()) {
            ConnectionInfo info = new ConnectionInfo();
            info.setCharacter(character);
            connectionInfos.add(info);
            return info;
        }

        return connectionInfos.get(0);
    }

    /**
     * Update personal info (appearance, skills, comment)
     */
    public static void updatePersonalInfo(Character character, PersonalInfoUpdate update) {
        update.validate();

        CharacterAppearance appearance = character.getAppearance().get(0);
        CharacterEquippedSkills skills = getOrCreateSkills(character);

        character.setComment(update.comment);

        appearance.setUpper(update.upper);
        appearance.setLower(update.lower);
        appearance.setHead(update.head);
        appearance.setChest(update.chest);
        appearance.setHands(update.hands);
        appearance.setWaist(update.waist);
        appearance.setFeet(update.feet);
        appearance.setAccessory1(update.accessory1);
        appearance.setAccessory2(update.accessory2);
        appearance.setUpperColor(update.upperColor);
        appearance.setLowerColor(update.lowerColor);
        appearance.setHeadColor(update.headColor);
        appearance.setChestColor(update.chestColor);
        appearance.setHandsColor(update.handsColor);
        appearance.setWaistColor(update.waistColor);
        appearance.setFeetColor(update.feetColor);
        appearance.setAccessory1Color(update.accessory1Color);
        appearance.setAccessory2Color(update.accessory2Color);
        appearance.setFacePaint(update.facePaint);

        skills.setSkill1(update.skill1);
        skills.setSkill2(update.skill2);
        skills.setSkill3(update.skill3);
        skills.setSkill4(update.skill4);
        skills.setLevel1(update.level1);
        skills.setLevel2(update.level2);
        skills.setLevel3(update.level3);
        skills.setLevel4(update.level4);

        DbManager.txVoid(session -> {
            session.update(character);
            session.update(appearance);
            session.saveOrUpdate(skills);
        });
    }

    /**
     * Update connection info
     */
    public static void updateConnectionInfo(Character character, String publicIp, int publicPort,
            String privateIp, int privatePort) {
        ConnectionInfo info = getOrCreateConnectionInfo(character);

        info.setPublicIp(publicIp);
        info.setPublicPort(publicPort);
        info.setPrivateIp(privateIp);
        info.setPrivatePort(privatePort);

        DbManager.txVoid(session -> session.saveOrUpdate(info));
    }

    // ========================================================================
    // Character Card
    // ========================================================================

    public static class CharacterCardData {
        public final int charaId;
        public final String name;
        public final String comment;
        public final int animalRank;
        public final int points;
        public final String clanName;
        public final int clanId;
        public final boolean hasClan;
        public final boolean hasEmblem;
        public final boolean isFemale;
        public final int totalReward;
        public final int playtimeSeconds;

        public CharacterCardData(int charaId, String name, String comment, int animalRank, int points,
                String clanName, int clanId, boolean hasClan, boolean hasEmblem, boolean isFemale,
                int totalReward, int playtimeSeconds) {
            this.charaId = charaId;
            this.name = name;
            this.comment = comment;
            this.animalRank = animalRank;
            this.points = points;
            this.clanName = clanName;
            this.clanId = clanId;
            this.hasClan = hasClan;
            this.hasEmblem = hasEmblem;
            this.isFemale = isFemale;
            this.totalReward = totalReward;
            this.playtimeSeconds = playtimeSeconds;
        }
    }

    public static CharacterCardData getCharacterCard(int targetCharaId) {
        return DbManager.tx(session -> {
            Character targetChar = session.createQuery(
                    "select c from Character c " +
                            "join fetch c.user u " +
                            "where c.id = :charaId",
                    Character.class)
                    .setParameter("charaId", targetCharaId)
                    .uniqueResult();
            if (targetChar == null) {
                return null;
            }

            int playtime = calculatePlaytime(session, targetCharaId);

            CharacterStats stats = session.createQuery(
                    "FROM CharacterStats WHERE charaId = :charaId", CharacterStats.class)
                    .setParameter("charaId", targetCharaId)
                    .uniqueResultOptional()
                    .orElse(null);

            User targetUser = targetChar.getUser();
            String charName = targetChar.getName();
            String comment = targetChar.getComment() != null ? targetChar.getComment() : "";

            int exp = calculateExp(targetChar, targetUser);
            int totalReward = stats != null ? stats.getScore() : 0;

            // Use stored animal rank - it should be pre-calculated or manually set
            int animalRank = targetChar.getRank() != null ? targetChar.getRank() : 0;

            int points = exp;

            boolean isFemale = false;
            CharacterAppearance appearance = session.createQuery(
                    "select ap from CharacterAppearance ap " +
                            "where ap.character.id = :charaId " +
                            "order by ap.id asc",
                    CharacterAppearance.class)
                    .setParameter("charaId", targetCharaId)
                    .setMaxResults(1)
                    .uniqueResultOptional()
                    .orElse(null);
            if (appearance != null && appearance.getGender() != null) {
                isFemale = appearance.getGender() != 0;
            }

            String clanName = "";
            int clanId = 0;
            boolean hasClan = false;
            boolean hasEmblem = false;
            ClanMember clanMember = session.createQuery(
                    "select cm from ClanMember cm " +
                            "join fetch cm.clan cl " +
                            "where cm.character.id = :charaId " +
                            "order by cm.id asc",
                    ClanMember.class)
                    .setParameter("charaId", targetCharaId)
                    .setMaxResults(1)
                    .uniqueResultOptional()
                    .orElse(null);
            if (clanMember != null && clanMember.getClan() != null) {
                clanName = clanMember.getClan().getName();
                clanId = clanMember.getClan().getId();
                hasClan = true;
                hasEmblem = clanMember.getClan().getEmblem() != null;
            }

            return new CharacterCardData(
                    targetCharaId,
                    charName,
                    comment,
                    animalRank,
                    points,
                    clanName,
                    clanId,
                    hasClan,
                    hasEmblem,
                    isFemale,
                    totalReward,
                    playtime);
        });
    }

    @SuppressWarnings("unchecked")
    private static int calculatePlaytime(org.hibernate.Session session, int charaId) {
        try {
            NativeQuery<Object[]> playtimeQuery = session.createNativeQuery(
                    "SELECT SUM(d_time - c_time) as total FROM (" +
                            "  SELECT c.time as c_time, " +
                            "    (SELECT MIN(d.time) FROM mgo2_event_disconnectgame d " +
                            "     WHERE d.game = c.game AND d.chara = c.chara AND d.time >= c.time) as d_time " +
                            "  FROM mgo2_event_connectgame c WHERE c.chara = :charaId" +
                            ") as sessions WHERE d_time IS NOT NULL");
            playtimeQuery.setParameter("charaId", charaId);
            Object result = playtimeQuery.uniqueResult();
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (Exception e) {
            // Playtime calculation failed, return 0
        }
        return 0;
    }

    private static int calculateExp(Character character, User user) {
        return character.getExp() != null ? character.getExp() : 0;
    }

    public static int calculateLevel(int experience) {
        if (experience < 125)
            return 0;
        if (experience < 250)
            return 1;
        if (experience < 375)
            return 2;
        if (experience < 500)
            return 3;
        if (experience < 650)
            return 4;
        if (experience < 800)
            return 5;
        if (experience < 950)
            return 6;
        if (experience < 1100)
            return 7;
        if (experience < 1250)
            return 8;
        if (experience < 1400)
            return 9;
        if (experience < 1550)
            return 10;
        if (experience < 1700)
            return 11;
        if (experience < 1850)
            return 12;
        if (experience < 2000)
            return 13;
        if (experience < 2175)
            return 14;
        if (experience < 2350)
            return 15;
        if (experience < 2525)
            return 16;
        if (experience < 2725)
            return 17;
        if (experience < 2925)
            return 18;
        if (experience < 3275)
            return 19;
        return 20;
    }

    // ========================================================================
    // Animal Rank Calculation
    // ========================================================================

    /**
     * Calculate animal rank for a character.
     * Uses the previous week's stats from mgo2_characters_stats_weekly table.
     * Falls back to current stats if no weekly stats exist.
     */
    @SuppressWarnings("unchecked")
    private static int calculateAnimalRank(Session session, int charaId, CharacterStats currentStats) {
        // First try to get previous week's stats
        CharacterStats weeklyStats = getWeeklyStats(session, charaId);

        // Use weekly stats if available, otherwise use current stats
        CharacterStats statsToUse = weeklyStats != null ? weeklyStats : currentStats;

        if (statsToUse == null) {
            return 0;
        }

        // For Tsuchinoko, we need days since last login
        // This would require tracking login times - for now assume 0
        int daysSinceLastLogin = 0;

        return AnimalRankService.calculateAnimalRank(statsToUse, daysSinceLastLogin);
    }

    /**
     * Query the previous week's stats from the weekly stats table.
     * Returns null if no weekly stats exist.
     */
    @SuppressWarnings("unchecked")
    private static CharacterStats getWeeklyStats(Session session, int charaId) {
        try {
            // Query weekly stats and map to CharacterStats object
            NativeQuery<Object[]> query = session.createNativeQuery(
                    "SELECT * FROM mgo2_characters_stats_weekly WHERE chara = :charaId LIMIT 1");
            query.setParameter("charaId", charaId);

            List<Object[]> results = query.list();
            if (results.isEmpty()) {
                return null;
            }

            Object[] row = results.get(0);

            // Map the result to a CharacterStats object
            CharacterStats stats = new CharacterStats();
            stats.setKills(getIntValue(row, 3));
            stats.setDeaths(getIntValue(row, 4));
            stats.setWins(getIntValue(row, 5));
            stats.setScore(getIntValue(row, 6));
            stats.setRounds(getIntValue(row, 7));
            stats.setStuns(getIntValue(row, 8));
            stats.setStunsReceived(getIntValue(row, 9));
            stats.setStunsFriendly(getIntValue(row, 10));
            stats.setHeadshotKills(getIntValue(row, 11));
            stats.setHeadshotDeaths(getIntValue(row, 12));
            stats.setHeadshotStuns(getIntValue(row, 13));
            stats.setHeadshotStunsReceived(getIntValue(row, 14));
            stats.setLockKills(getIntValue(row, 15));
            stats.setLockDeaths(getIntValue(row, 16));
            stats.setLockStuns(getIntValue(row, 17));
            stats.setLockStunsReceived(getIntValue(row, 18));
            stats.setConsecutiveKills(getIntValue(row, 19));
            stats.setConsecutiveDeaths(getIntValue(row, 20));
            stats.setConsecutiveHeadshots(getIntValue(row, 21));
            stats.setConsecutiveTdm(getIntValue(row, 22));
            stats.setSpotted(getIntValue(row, 23));
            stats.setSelfSpotted(getIntValue(row, 24));
            stats.setSnakeSpotted(getIntValue(row, 25));
            stats.setSnakeSelfSpotted(getIntValue(row, 26));
            stats.setSuicides(getIntValue(row, 27));
            stats.setSalutes(getIntValue(row, 28));
            stats.setRadio(getIntValue(row, 29));
            stats.setChat(getIntValue(row, 30));
            stats.setCqcGiven(getIntValue(row, 31));
            stats.setCqcTaken(getIntValue(row, 32));
            stats.setRolls(getIntValue(row, 33));
            stats.setCatapult(getIntValue(row, 34));
            stats.setFalls(getIntValue(row, 35));
            stats.setTrapped(getIntValue(row, 36));
            stats.setMelee(getIntValue(row, 37));
            stats.setMeleeRec(getIntValue(row, 38));
            stats.setBoxTime(getIntValue(row, 39));
            stats.setBoxUses(getIntValue(row, 40));
            stats.setBasesCaptured(getIntValue(row, 41));
            stats.setBasesDestroyed(getIntValue(row, 42));
            stats.setSopDestab(getIntValue(row, 43));
            stats.setGakoSaved(getIntValue(row, 44));
            stats.setGakoDefended(getIntValue(row, 45));
            stats.setGakoFirst(getIntValue(row, 46));
            stats.setResDefend(getIntValue(row, 47));
            stats.setResGakoTime(getIntValue(row, 48));
            stats.setResFirstGrab(getIntValue(row, 49));
            stats.setBombDisarms(getIntValue(row, 50));
            stats.setSdmSurvivals(getIntValue(row, 51));
            stats.setRaceCheckpoints(getIntValue(row, 52));
            stats.setWinsSnake(getIntValue(row, 53));
            stats.setKillsSnake(getIntValue(row, 54));
            stats.setSnakeHoldups(getIntValue(row, 55));
            stats.setSnakeTagsSpawned(getIntValue(row, 56));
            stats.setSnakeTagsTaken(getIntValue(row, 57));
            stats.setSnakeInjured(getIntValue(row, 58));
            stats.setTsneGrab1(getIntValue(row, 59));
            stats.setTsneGrab2(getIntValue(row, 60));
            stats.setKnifeKills(getIntValue(row, 61));
            stats.setKnifeStuns(getIntValue(row, 62));
            stats.setBoosts(getIntValue(row, 63));
            stats.setScans(getIntValue(row, 64));
            stats.setEvgTime(getIntValue(row, 65));
            stats.setWakeups(getIntValue(row, 66));
            stats.setTeamKills(getIntValue(row, 67));
            stats.setWithdrawals(getIntValue(row, 68));
            stats.setPointsAssist(getIntValue(row, 69));
            stats.setPointsBase(getIntValue(row, 70));
            stats.setTrainedSoldiers(getIntValue(row, 71));
            stats.setTimeTraining(getIntValue(row, 72));
            stats.setTimeInstructor(getIntValue(row, 73));
            stats.setTimeStudent(getIntValue(row, 74));
            stats.setTime(getIntValue(row, 75));
            stats.setTimeSnake(getIntValue(row, 76));
            stats.setTimeDedi(getIntValue(row, 77));
            stats.setStatsDm(getStringValue(row, 78));
            stats.setStatsTdm(getStringValue(row, 79));
            stats.setStatsRes(getStringValue(row, 80));
            stats.setStatsCap(getStringValue(row, 81));
            stats.setStatsBase(getStringValue(row, 82));
            stats.setStatsBomb(getStringValue(row, 83));
            stats.setStatsSne(getStringValue(row, 84));
            stats.setStatsTsne(getStringValue(row, 85));
            stats.setStatsSdm(getStringValue(row, 86));
            stats.setStatsInt(getStringValue(row, 87));
            stats.setStatsScap(getStringValue(row, 88));
            stats.setStatsRace(getStringValue(row, 89));

            return stats;
        } catch (Exception e) {
            // Log error and return null
            return null;
        }
    }

    private static int getIntValue(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return 0;
        }
        if (row[index] instanceof Number) {
            return ((Number) row[index]).intValue();
        }
        return 0;
    }

    private static String getStringValue(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return null;
        }
        return row[index].toString();
    }

    // ========================================================================
    // Search
    // ========================================================================

    public static List<Character> searchCharacters(String name, boolean exactOnly) {
        String searchPattern = exactOnly ? name : "%" + name + "%";

        return DbManager.tx(session -> {
            Query<Character> query = session.createQuery("from Character where name like :name", Character.class);
            query.setParameter("name", searchPattern);
            List<Character> results = query.list();

            for (Character character : results) {
                Hibernate.initialize(character.getLobby());
            }
            return results;
        });
    }

    // ========================================================================
    // Personal Stats
    // ========================================================================

    public static class PersonalStatsData {
        public final Character character;
        public final CharacterStats stats;
        public final int playtimeSeconds;
        public final String clanName;
        public final boolean hasClan;

        public PersonalStatsData(Character character, CharacterStats stats, int playtimeSeconds, String clanName,
                boolean hasClan) {
            this.character = character;
            this.stats = stats;
            this.playtimeSeconds = playtimeSeconds;
            this.clanName = clanName;
            this.hasClan = hasClan;
        }
    }

    public static PersonalStatsData getPersonalStats(int targetCharaId) {
        return DbManager.tx(session -> {
            Character targetChar = session.createQuery(
                    "select distinct c from Character c " +
                            "left join fetch c.clanMember cm " +
                            "left join fetch cm.clan " +
                            "where c.id = :charaId",
                    Character.class)
                    .setParameter("charaId", targetCharaId)
                    .uniqueResult();
            if (targetChar == null) {
                return null;
            }

            int playtime = calculatePlaytime(session, targetCharaId);

            String clanName = "";
            boolean hasClan = false;
            ClanMember clanMember = session.createQuery(
                    "select cm from ClanMember cm " +
                            "join fetch cm.clan cl " +
                            "where cm.character.id = :charaId " +
                            "order by cm.id asc",
                    ClanMember.class)
                    .setParameter("charaId", targetCharaId)
                    .setMaxResults(1)
                    .uniqueResultOptional()
                    .orElse(null);
            if (clanMember != null && clanMember.getClan() != null) {
                clanName = clanMember.getClan().getName();
                hasClan = true;
            }

            CharacterStats stats = session.createQuery(
                    "FROM CharacterStats WHERE charaId = :charaId", CharacterStats.class)
                    .setParameter("charaId", targetCharaId)
                    .uniqueResultOptional()
                    .orElse(null);

            return new PersonalStatsData(targetChar, stats, playtime, clanName, hasClan);
        });
    }
}
