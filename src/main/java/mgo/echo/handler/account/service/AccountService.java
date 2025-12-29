package mgo.echo.handler.account.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.security.CryptoProvider;
import mgo.echo.session.ActiveChannels;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Util;

/**
 * Account session and lobby connection service.
 */
public class AccountService {
    private static final Logger logger = LogManager.getLogger(AccountService.class);

    private static final byte[] XOR_SESSION_ID = new byte[] {
            (byte) 0x35, (byte) 0xd5, (byte) 0xc3, (byte) 0x8e,
            (byte) 0xd0, (byte) 0x11, (byte) 0x0e, (byte) 0xa8
    };

    private static final byte[] SPECIAL_SESSION_BYTES = {
            (byte) 0xE7, (byte) 0xBA, (byte) 0xB4, (byte) 0x26,
            (byte) 0xFE, (byte) 0x3F, (byte) 0x40, (byte) 0x73,
            (byte) 0xDB, (byte) 0x94, (byte) 0x36, (byte) 0xDF,
            (byte) 0x6D, (byte) 0xDB, (byte) 0xD3, (byte) 0x9C
    };

    private AccountService() {
    }

    // ========================================================================
    // Session Validation
    // ========================================================================

    public static String decodeSessionBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        logger.info("Session bytes: {}", sb.toString());

        if (Arrays.equals(bytes, SPECIAL_SESSION_BYTES)) {
            logger.info("Using special session: cafebabe");
            return "cafebabe";
        }

        byte[] mgo2SessionBytes = decryptSessionId(bytes);
        return new String(mgo2SessionBytes, StandardCharsets.ISO_8859_1);
    }

    private static byte[] decryptSessionId(byte[] full) {
        byte[] bytes = new byte[8];
        System.arraycopy(full, 0, bytes, 0, bytes.length);
        Util.xor(bytes, XOR_SESSION_ID);
        return CryptoProvider.instanceAuth().encrypt(bytes);
    }

    public static User findUserBySession(String session) {
        return DbManager.tx(dbSession -> {
            Query<User> query = dbSession.createQuery("from User where session=:session", User.class);
            query.setParameter("session", session);
            return query.uniqueResult();
        });
    }

    public static void clearCurrentCharacter(User user) {
        DbManager.txVoid(session -> {
            user.setCurrentCharacter(null);
            session.update(user);
        });
    }

    // ========================================================================
    // Lobby Connection
    // ========================================================================

    public static boolean connectToLobby(ChannelHandlerContext ctx, Lobby lobby, User user) {
        try {
            DbManager.txVoid(session -> {
                session.update(user);

                if (user.getCurrentCharacterId() != null && user.getCurrentCharacter() != null) {
                    initializeCharacterForLobby(session, user.getCurrentCharacter(), lobby);
                }
            });

            closePreviousSessions(ctx, user);

            user.setChannel(ctx.channel());
            ActiveChannels.add(ctx.channel());
            ActiveUsers.add(ctx.channel(), user);

            return true;
        } catch (Exception e) {
            logger.error("Exception while handling lobby connection.", e);
            return true;
        }
    }

    private static void initializeCharacterForLobby(Session session, Character character, Lobby lobby) {
        Hibernate.initialize(character);
        character.setLobby(lobby);
        character.setLobbyId(lobby.getId());

        Hibernate.initialize(character.getAppearance());
        Hibernate.initialize(character.getBlocked());
        Hibernate.initialize(character.getChatMacros());
        Hibernate.initialize(character.getClanApplication());
        Hibernate.initialize(character.getClanMember());

        ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
        if (clanMember != null) {
            Hibernate.initialize(clanMember.getClan());
        }

        Hibernate.initialize(character.getConnectionInfo());
        Hibernate.initialize(character.getFriends());
        Hibernate.initialize(character.getHostSettings());
        Hibernate.initialize(character.getPlayer());
        Hibernate.initialize(character.getSetsGear());
        Hibernate.initialize(character.getSetsSkills());
        Hibernate.initialize(character.getSkills());

        session.update(character);
    }

    private static void closePreviousSessions(ChannelHandlerContext ctx, User user) {
        try {
            List<User> duplicates = ActiveUsers
                    .get(u -> u != null && u.getId() != null && u.getId().equals(user.getId()));

            for (User online : duplicates) {
                Channel oldChannel = online.getChannel();
                if (oldChannel == null) {
                    continue;
                }
                if (oldChannel == ctx.channel()) {
                    continue;
                }
                if (!oldChannel.isOpen()) {
                    continue;
                }

                logger.info("Closing previous session for user {} on channel {}", user.getId(), oldChannel.id());
                oldChannel.close();
            }
        } catch (Exception e) {
            logger.warn("Failed to close previous sessions for user {}", user.getId(), e);
        }
    }
}
