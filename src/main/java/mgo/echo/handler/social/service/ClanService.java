package mgo.echo.handler.social.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.query.Query;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Clan;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.data.repository.DbManager;
import mgo.echo.util.Error;
import mgo.echo.util.Util;

/**
 * Business logic for clan operations.
 * No Netty dependencies - pure domain logic.
 */
public class ClanService {
    private static final Logger logger = LogManager.getLogger();

    // =========================================================================
    // Result Types
    // =========================================================================

    public static class ClanResult {
        public final boolean success;
        public final Error error;

        private ClanResult(boolean success, Error error) {
            this.success = success;
            this.error = error;
        }

        public static ClanResult success() {
            return new ClanResult(true, null);
        }

        public static ClanResult error(Error error) {
            return new ClanResult(false, error);
        }
    }

    // =========================================================================
    // Get Clan Data
    // =========================================================================

    public static Clan getClanById(int clanId) {
        return DbManager.tx(session -> session.get(Clan.class, clanId));
    }

    public static List<Clan> getAllClans() {
        return DbManager.tx(session -> {
            Query<Clan> query = session.createQuery(
                    "from Clan c join fetch c.leader l join fetch l.character", Clan.class);
            return query.list();
        });
    }

    public static List<Clan> searchClans(String name, boolean exactOnly) {
        String searchName = exactOnly ? name : "%" + name + "%";

        return DbManager.tx(session -> {
            Query<Clan> query = session.createQuery(
                    "from Clan c join fetch c.leader l join fetch l.character where c.name like :name", Clan.class);
            query.setParameter("name", searchName);
            return query.list();
        });
    }

    public static Clan getClanForInformation(int clanId) {
        return DbManager.tx(session -> {
            Query<Clan> query = session.createQuery(
                    "from Clan c join fetch c.leader l join fetch l.character join fetch c.members where c.id=:clanId",
                    Clan.class);
            query.setParameter("clanId", clanId);
            return query.uniqueResult();
        });
    }

    public static Clan getClanForMemberInformation(int clanId) {
        return DbManager.tx(session -> {
            Query<Clan> query = session.createQuery(
                    "from Clan c join fetch c.leader l join fetch l.character where c.id=:clanId", Clan.class);
            query.setParameter("clanId", clanId);

            Clan c = query.uniqueResult();
            if (c != null) {
                Hibernate.initialize(c.getEmblemEditor());
                Hibernate.initialize(c.getApplications());
                Hibernate.initialize(c.getNoticeWriter());
                if (c.getNoticeWriter() != null) {
                    Hibernate.initialize(c.getNoticeWriter().getCharacter());
                }
            }
            return c;
        });
    }

    public static Clan getClanForRoster(int clanId, ClanMember clanMember) {
        return DbManager.tx(session -> {
            Query<Clan> query = session.createQuery(
                    "from Clan c join fetch c.members m join fetch m.character where c.id = :clan", Clan.class);
            query.setParameter("clan", clanId);

            Clan c = query.uniqueResult();

            if (c != null && c.getLeader() != null && clanMember != null
                    && clanMember.getId().equals(c.getLeader().getId())) {
                Query<MessageClanApplication> queryM = session.createQuery(
                        "from MessageClanApplication m join fetch m.character where m.clan = :clan",
                        MessageClanApplication.class);
                queryM.setParameter("clan", c);
                c.setApplications(queryM.list());
            }
            return c;
        });
    }

    // =========================================================================
    // Create Clan
    // =========================================================================

    public static ClanResult createClan(Character character, String name, String comment) {
        ClanMember existingMember = Util.getFirstOrNull(character.getClanMember());
        if (existingMember != null) {
            logger.error("Error while creating clan: Currently in a clan.");
            return ClanResult.error(Error.CLAN_INACLAN);
        }

        Clan clan = new Clan();
        clan.setName(name);
        clan.setComment(comment);
        clan.setOpen(1);

        ClanMember newMember = new ClanMember();
        newMember.setCharacter(character);
        newMember.setClan(clan);
        clan.setLeader(newMember);

        DbManager.txVoid(session -> {
            session.save(clan);
            session.save(newMember);
        });

        List<ClanMember> clanMembers = new ArrayList<>();
        clanMembers.add(newMember);
        character.setClanMember(clanMembers);

        return ClanResult.success();
    }

    // =========================================================================
    // Update Clan
    // =========================================================================

    public static ClanResult updateComment(ClanMember clanMember, String comment) {
        if (clanMember == null) {
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (clan == null) {
            return ClanResult.error(Error.CLAN_DOESNOTEXIST);
        }

        DbManager.txVoid(session -> {
            Clan sClan = session.get(Clan.class, clan.getId());
            sClan.setComment(comment);
        });

        return ClanResult.success();
    }

    public static ClanResult updateNotice(ClanMember clanMember, String notice) {
        if (clanMember == null) {
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (clan == null) {
            return ClanResult.error(Error.CLAN_DOESNOTEXIST);
        }

        final ClanMember writer = clanMember;
        DbManager.txVoid(session -> {
            Clan sClan = session.get(Clan.class, clan.getId());
            sClan.setNotice(notice);
            sClan.setNoticeTime((int) Instant.now().getEpochSecond());
            sClan.setNoticeWriter(writer);
        });

        return ClanResult.success();
    }

    public static ClanResult setEmblem(ClanMember clanMember, byte[] emblem, boolean isWip) {
        if (clanMember == null) {
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        int clanId = clanMember.getClanId();

        DbManager.txVoid(session -> {
            Clan clan = session.get(Clan.class, clanId);
            if (isWip) {
                clan.setEmblemWip(emblem);
            } else {
                clan.setEmblem(emblem);
            }
        });

        return ClanResult.success();
    }

    // =========================================================================
    // Membership
    // =========================================================================

    public static ClanResult acceptJoin(ClanMember clanMember, int targetCharaId) {
        if (clanMember == null) {
            logger.error("Error while accepting join: Not in a clan.");
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
            logger.error("Error while accepting join: Not a clan leader.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        Character targetCharacter = DbManager.tx(session -> {
            Query<Character> queryC = session.createQuery(
                    "from Character c join fetch c.clanApplication where c.id = :character", Character.class);
            queryC.setParameter("character", targetCharaId);
            return queryC.uniqueResult();
        });

        if (targetCharacter == null) {
            logger.error("Error while accepting join: Target does not exist.");
            return ClanResult.error(Error.CHARACTER_DOESNOTEXIST);
        }

        MessageClanApplication application = Util.getFirstOrNull(targetCharacter.getClanApplication());
        if (application == null) {
            logger.error("Error while accepting join: Target has not applied.");
            return ClanResult.error(Error.CLAN_NOAPPLICATION);
        }

        if (!application.getClanId().equals(clan.getId())) {
            logger.error("Error while accepting join: Not the same clan.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        ClanMember targetClanMember = new ClanMember();
        targetClanMember.setCharacter(targetCharacter);
        targetClanMember.setClan(clan);

        DbManager.txVoid(session -> {
            session.remove(application);
            session.save(targetClanMember);
        });

        return ClanResult.success();
    }

    public static ClanResult declineJoin(ClanMember clanMember, int targetCharaId) {
        if (clanMember == null) {
            logger.error("Error while declining join: Not in a clan.");
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
            logger.error("Error while declining join: Not a clan leader.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        Character targetCharacter = DbManager.tx(session -> {
            Query<Character> queryC = session.createQuery(
                    "from Character c join fetch c.clanApplication where c.id = :character", Character.class);
            queryC.setParameter("character", targetCharaId);
            return queryC.uniqueResult();
        });

        if (targetCharacter == null) {
            logger.error("Error while declining join: Target does not exist.");
            return ClanResult.error(Error.CHARACTER_DOESNOTEXIST);
        }

        MessageClanApplication application = Util.getFirstOrNull(targetCharacter.getClanApplication());
        if (application == null) {
            logger.error("Error while declining join: Target has not applied.");
            return ClanResult.error(Error.CLAN_NOAPPLICATION);
        }

        if (!application.getClanId().equals(clan.getId())) {
            logger.error("Error while declining join: Not the same clan.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        DbManager.txVoid(session -> session.remove(application));

        return ClanResult.success();
    }

    public static ClanResult leave(Character character) {
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
        MessageClanApplication clanApplication = Util.getFirstOrNull(character.getClanApplication());

        DbManager.txVoid(session -> {
            if (clanApplication != null) {
                session.delete(clanApplication);
            }
            if (clanMember != null) {
                session.delete(clanMember);
            }
        });

        if (clanApplication != null) {
            character.getClanApplication().clear();
        }
        if (clanMember != null) {
            character.getClanMember().clear();
        }

        return ClanResult.success();
    }

    public static ClanResult banish(ClanMember clanMember, int targetCharaId) {
        if (clanMember == null) {
            logger.error("Error while banishing member: Not in a clan.");
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
            logger.error("Error while banishing member: Not a clan leader.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        ClanMember targetClanMember = DbManager.tx(session -> {
            Query<ClanMember> query = session.createQuery(
                    "from ClanMember m join fetch m.character c where c.id = :character", ClanMember.class);
            query.setParameter("character", targetCharaId);
            return query.uniqueResult();
        });

        if (targetClanMember == null) {
            logger.error("Error while banishing member: Target does not exist.");
            return ClanResult.error(Error.CHARACTER_DOESNOTEXIST);
        }

        if (!targetClanMember.getClanId().equals(clan.getId())) {
            logger.error("Error while banishing member: Not the same clan.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        DbManager.txVoid(session -> session.remove(targetClanMember));

        return ClanResult.success();
    }

    // =========================================================================
    // Disband / Leadership
    // =========================================================================

    public static ClanResult disband(Character character, ClanMember clanMember) {
        if (clanMember == null) {
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (!clan.getLeaderId().equals(clanMember.getId())) {
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        DbManager.txVoid(session -> session.delete(clan));

        character.getClanApplication().clear();

        return ClanResult.success();
    }

    public static ClanResult transferLeadership(ClanMember clanMember, int targetCharaId) {
        if (clanMember == null) {
            logger.error("Error while transferring leadership: Not in a clan.");
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
            logger.error("Error while transferring leadership: Not a clan leader.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        ClanMember targetClanMember = DbManager.tx(session -> {
            Query<ClanMember> query = session.createQuery(
                    "from ClanMember m join fetch m.character c where c.id = :character", ClanMember.class);
            query.setParameter("character", targetCharaId);
            return query.uniqueResult();
        });

        if (targetClanMember == null) {
            logger.error("Error while transferring leadership: Target does not exist.");
            return ClanResult.error(Error.CHARACTER_DOESNOTEXIST);
        }

        if (!targetClanMember.getClanId().equals(clan.getId())) {
            logger.error("Error while transferring leadership: Not the same clan.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        DbManager.txVoid(session -> {
            clan.setLeader(targetClanMember);
            session.update(clan);
        });

        return ClanResult.success();
    }

    public static ClanResult setEmblemEditor(ClanMember clanMember, int targetCharaId) {
        if (clanMember == null) {
            logger.error("Error while assigning emblem rights: Not in a clan.");
            return ClanResult.error(Error.CLAN_NOTAMEMBER);
        }

        Clan clan = clanMember.getClan();
        if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
            logger.error("Error while assigning emblem rights: Not a clan leader.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        ClanMember targetClanMember = DbManager.tx(session -> {
            Query<ClanMember> query = session.createQuery(
                    "from ClanMember m join fetch m.character c where c.id = :character", ClanMember.class);
            query.setParameter("character", targetCharaId);
            return query.uniqueResult();
        });

        if (targetClanMember == null) {
            logger.error("Error while assigning emblem rights: Target does not exist.");
            return ClanResult.error(Error.CHARACTER_DOESNOTEXIST);
        }

        if (!targetClanMember.getClanId().equals(clan.getId())) {
            logger.error("Error while assigning emblem rights: Not the same clan.");
            return ClanResult.error(Error.CLAN_NOTALEADER);
        }

        boolean isSelfAssign = clanMember.getCharacterId().equals(targetCharaId);
        DbManager.txVoid(session -> {
            if (isSelfAssign) {
                clan.setEmblemEditor(null);
            } else {
                clan.setEmblemEditor(targetClanMember);
            }
            session.update(clan);
        });

        return ClanResult.success();
    }
}
