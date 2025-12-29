package mgo.echo.handler.social.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Clan;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.social.dto.MessageRecipientError;
import mgo.echo.util.Error;
import mgo.echo.util.Util;

/**
 * Business logic for message operations.
 * No Netty dependencies - pure domain logic.
 */
public class MessageService {
    private static final Logger logger = LogManager.getLogger();

    // =========================================================================
    // Get Clan Application Messages
    // =========================================================================

    public static List<MessageClanApplication> getClanApplicationMessages(Character character) {
        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
        if (clanMember == null) {
            return null;
        }

        Clan clan = clanMember.getClan();

        Session session = DbManager.getSession();
        session.beginTransaction();

        Query<MessageClanApplication> query = session.createQuery(
                "from MessageClanApplication m join fetch m.character where m.clan = :clan",
                MessageClanApplication.class);
        query.setParameter("clan", clan);
        List<MessageClanApplication> messages = query.list();

        session.getTransaction().commit();
        DbManager.closeSession(session);

        return messages;
    }

    // =========================================================================
    // Send Clan Application
    // =========================================================================

    public static List<MessageRecipientError> sendClanApplication(User user, Character character, String clanName,
            String comment) {
        List<MessageRecipientError> errors = new ArrayList<>();

        MessageClanApplication existingApplication = Util.getFirstOrNull(character.getClanApplication());
        if (existingApplication != null) {
            errors.add(new MessageRecipientError("Already applied!", Error.CLAN_HASAPPLICATION));
            return errors;
        }

        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
        if (clanMember != null) {
            errors.add(new MessageRecipientError("Already in clan!", Error.CLAN_INACLAN));
            return errors;
        }

        Clan clan = findClanByName(clanName);
        if (clan == null) {
            errors.add(new MessageRecipientError("Bad clan name!", Error.CLAN_DOESNOTEXIST));
            return errors;
        }

        createClanApplication(character, clan, comment);
        return errors;
    }

    private static Clan findClanByName(String name) {
        Session session = DbManager.getSession();
        session.beginTransaction();

        Query<Clan> query = session.createQuery("from Clan c where c.name = :name", Clan.class);
        query.setParameter("name", name);
        Clan clan = query.uniqueResult();

        session.getTransaction().commit();
        DbManager.closeSession(session);

        return clan;
    }

    private static void createClanApplication(Character character, Clan clan, String comment) {
        MessageClanApplication message = new MessageClanApplication();
        message.setCharacter(character);
        message.setClan(clan);
        message.setComment(comment);
        message.setTime((int) Instant.now().getEpochSecond());

        Session session = DbManager.getSession();
        session.beginTransaction();
        session.save(message);
        session.getTransaction().commit();
        DbManager.closeSession(session);
    }
}
