package mgo.echo.handler.account;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.game.service.GameService;
import mgo.echo.session.ActiveUsers;

/**
 * Shared account operations used across multiple handlers.
 *
 * Note: Command-specific logic has been moved to AccountController and
 * AccountLobbyController.
 */
public class AccountHandler {
    private static final Logger logger = LogManager.getLogger(AccountHandler.class);

    private AccountHandler() {
    }

    // ========================================================================
    // Lobby Disconnect (used by GameLobby and AccountLobby)
    // ========================================================================

    public static void onLobbyDisconnected(ChannelHandlerContext ctx, Lobby lobby) {
        try {
            User user = ActiveUsers.get(ctx.channel());
            if (user == null) {
                return;
            }

            Character character = user.getCurrentCharacter();
            logger.debug("Disconnecting from lobby {}: Character - {}", user.getId(), character);

            if (user.getCurrentCharacterId() == null || character == null) {
                ActiveUsers.remove(ctx.channel());
                return;
            }

            handlePlayerQuitIfNeeded(ctx, user, character);
            character.setLobby(null);

            DbManager.txVoid(session -> session.update(character));

            ActiveUsers.remove(ctx.channel());
        } catch (Exception e) {
            logger.error("Exception while disconnecting from lobby.", e);
        }
    }

    private static void handlePlayerQuitIfNeeded(ChannelHandlerContext ctx, User user, Character character) {
        boolean hasPlayer = Hibernate.isInitialized(character.getPlayer())
                && character.getPlayer() != null
                && !character.getPlayer().isEmpty();

        Player player = hasPlayer ? character.getPlayer().get(0) : null;
        logger.debug("Disconnecting from lobby {}: Player - {}", user.getId(), player);

        if (player == null) {
            return;
        }

        logger.debug("Disconnecting from lobby {}: Quitting game.", user.getId());
        GameService.quitGame(user);
    }

    // ========================================================================
    // Clan Updates (used by ClanHandler and MessageHandler)
    // ========================================================================

    public static void updateUserClan(ChannelHandlerContext ctx) {
        try {
            User user = ActiveUsers.get(ctx.channel());
            if (user == null) {
                return;
            }

            Character character = user.getCurrentCharacter();
            if (user.getCurrentCharacterId() == null || character == null) {
                return;
            }

            ClanData clanData = DbManager.tx(session -> {
                MessageClanApplication application = fetchClanApplication(session, character);
                ClanMember member = fetchClanMember(session, character);
                return new ClanData(application, member);
            });

            List<MessageClanApplication> applications = new ArrayList<>();
            applications.add(clanData.application);
            character.setClanApplication(applications);

            List<ClanMember> members = new ArrayList<>();
            members.add(clanData.member);
            character.setClanMember(members);
        } catch (Exception e) {
            logger.error("Exception while updating user clan.", e);
        }
    }

    private static class ClanData {
        final MessageClanApplication application;
        final ClanMember member;

        ClanData(MessageClanApplication application, ClanMember member) {
            this.application = application;
            this.member = member;
        }
    }

    private static MessageClanApplication fetchClanApplication(Session session, Character character) {
        Query<MessageClanApplication> query = session.createQuery(
                "from MessageClanApplication a join fetch a.clan where a.character = :character",
                MessageClanApplication.class);
        query.setParameter("character", character);
        return query.uniqueResult();
    }

    private static ClanMember fetchClanMember(Session session, Character character) {
        Query<ClanMember> query = session.createQuery(
                "from ClanMember m join fetch m.clan where m.character = :character", ClanMember.class);
        query.setParameter("character", character);
        return query.uniqueResult();
    }
}
