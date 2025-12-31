package mgo.echo.handler.lobby;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.session.ActiveLobbies;
import mgo.echo.session.ActiveUsers;

/**
 * Lobby lifecycle service.
 * 
 * Handles lobby initialization and periodic updates.
 * Called from EchoApp on startup and via scheduled tasks.
 */
public class LobbyService {
    private static final Logger logger = LogManager.getLogger(LobbyService.class);

    private LobbyService() {
    }

    public static void initializeLobbies() {
        Session session = null;

        try {
            Collection<Lobby> lobbies = ActiveLobbies.get().values();

            session = DbManager.getSession();
            session.beginTransaction();

            for (Lobby lobby : lobbies) {
                Query<?> query = session.createQuery("delete Game where lobby=:lobby");
                query.setParameter("lobby", lobby);
                query.executeUpdate();
            }

            session.getTransaction().commit();
            DbManager.closeSession(session);
        } catch (Exception e) {
            logger.error("Exception while initializing lobbies.", e);
        }
    }

    public static void updateLobbies() {
        Session session = null;

        try {
            Collection<Lobby> lobbies = ActiveLobbies.get().values();

            for (Lobby lobby : lobbies) {
                List<User> users = ActiveUsers.get((user) -> {
                    try {
                        Character character = user.getCurrentCharacter();
                        if (character == null || character.getLobbyId() == null) {
                            return false;
                        }
                        return character.getLobbyId() == lobby.getId();
                    } catch (Exception ex) {
                        logger.error("Exception while updating lobby counts.", ex);
                        return false;
                    }
                });
                lobby.setPlayers(users.size());
            }

            session = DbManager.getSession();
            session.beginTransaction();

            for (Lobby lobby : lobbies) {
                session.update(lobby);
            }

            session.getTransaction().commit();
            DbManager.closeSession(session);
        } catch (Exception e) {
            logger.error("Exception while updating lobby count.", e);
        }
    }
}
