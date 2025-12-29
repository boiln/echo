package mgo.echo.handler.character.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
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
        public final int level;
        public final String clanName;
        public final boolean hasClan;
        public final int playtimeSeconds;

        public CharacterCardData(int charaId, String name, String comment, int level,
                String clanName, boolean hasClan, int playtimeSeconds) {
            this.charaId = charaId;
            this.name = name;
            this.comment = comment;
            this.level = level;
            this.clanName = clanName;
            this.hasClan = hasClan;
            this.playtimeSeconds = playtimeSeconds;
        }
    }

    public static CharacterCardData getCharacterCard(int targetCharaId) {
        return DbManager.tx(session -> {
            Query<Character> query = session.createQuery(
                    "from Character c " +
                            "join fetch c.user u " +
                            "left join fetch c.clanMember cm " +
                            "left join fetch cm.clan " +
                            "where c.id = :charaId",
                    Character.class);
            query.setParameter("charaId", targetCharaId);
            Character targetChar = query.uniqueResult();

            if (targetChar == null) {
                return null;
            }

            int playtime = calculatePlaytime(session, targetCharaId);

            User targetUser = targetChar.getUser();
            String charName = targetChar.getName();
            String comment = targetChar.getComment() != null ? targetChar.getComment() : "";

            int exp = calculateExp(targetChar, targetUser);
            int level = calculateLevel(exp);

            String clanName = "";
            boolean hasClan = false;
            List<ClanMember> clanMembers = targetChar.getClanMember();
            if (clanMembers != null && !clanMembers.isEmpty()) {
                ClanMember clanMember = clanMembers.get(0);
                if (clanMember != null && clanMember.getClan() != null) {
                    clanName = clanMember.getClan().getName();
                    hasClan = true;
                }
            }

            return new CharacterCardData(targetCharaId, charName, comment, level, clanName, hasClan, playtime);
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
        if (user.getMainCharacterId() != null &&
                character.getId().equals(user.getMainCharacterId())) {
            return user.getMainExp() != null ? user.getMainExp() : 0;
        }
        return user.getAltExp() != null ? user.getAltExp() : 0;
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

        public PersonalStatsData(Character character, CharacterStats stats) {
            this.character = character;
            this.stats = stats;
        }
    }

    public static PersonalStatsData getPersonalStats(int targetCharaId) {
        return DbManager.tx(session -> {
            Character targetChar = session.get(Character.class, targetCharaId);
            if (targetChar == null) {
                return null;
            }

            CharacterStats stats = session.createQuery(
                    "FROM CharacterStats WHERE charaId = :charaId", CharacterStats.class)
                    .setParameter("charaId", targetCharaId)
                    .uniqueResultOptional()
                    .orElse(null);

            return new PersonalStatsData(targetChar, stats);
        });
    }
}
