package mgo.echo.data.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.ServiceRegistry;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import mgo.echo.core.EchoApp;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterAppearance;
import mgo.echo.data.entity.CharacterBlocked;
import mgo.echo.data.entity.CharacterChatMacro;
import mgo.echo.data.entity.CharacterEquippedSkills;
import mgo.echo.data.entity.CharacterFriend;
import mgo.echo.data.entity.CharacterHostSettings;
import mgo.echo.data.entity.CharacterSetGear;
import mgo.echo.data.entity.CharacterSetSkills;
import mgo.echo.data.entity.CharacterStats;
import mgo.echo.data.entity.Clan;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.ConnectionInfo;
import mgo.echo.data.entity.EventConnectGame;
import mgo.echo.data.entity.EventCreateGame;
import mgo.echo.data.entity.EventDisconnectGame;
import mgo.echo.data.entity.EventEndGame;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.data.entity.News;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.plugin.PluginHandler;

public class DbManager {
    private static final Logger logger = LogManager.getLogger(DbManager.class);

    private static final Class<?>[] entityClasses = { Character.class, CharacterAppearance.class,
            CharacterBlocked.class, CharacterChatMacro.class, CharacterEquippedSkills.class, CharacterFriend.class,
            CharacterHostSettings.class, CharacterSetGear.class, CharacterSetSkills.class, CharacterStats.class,
            Clan.class,
            MessageClanApplication.class, ClanMember.class, ConnectionInfo.class, EventCreateGame.class,
            EventEndGame.class,
            EventConnectGame.class, EventDisconnectGame.class, Game.class, Lobby.class, News.class, Player.class,
            User.class };

    private static ComboPooledDataSource cpds;

    private static SessionFactory sessionFactory;

    public static HashMap<Session, String> sessionCheckouts = new HashMap<>();

    public static boolean initialize(String url, String user, String password, int minPoolSize, int maxPoolSize,
            int poolIncrement) {
        try {
            cpds = new ComboPooledDataSource();
            cpds.setDriverClass("com.mysql.cj.jdbc.Driver");
            cpds.setJdbcUrl(url);
            cpds.setUser(user);
            cpds.setPassword(password);

            cpds.setInitialPoolSize(minPoolSize);
            cpds.setMinPoolSize(minPoolSize);
            cpds.setAcquireIncrement(poolIncrement);
            cpds.setMaxPoolSize(maxPoolSize);

            cpds.setNumHelperThreads(EchoApp.DB_WORKERS);

            cpds.setTestConnectionOnCheckout(true);
        } catch (Exception e) {
            logger.error("Failed to initialize DbManager.", e);
            return false;
        }
        logger.debug("Initialized DbManager.");
        return true;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration();

            Properties props = new Properties();
            props.put("hibernate.current_session_context_class", "thread");
            props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            props.put("hibernate.show_sql", "false");
            props.put("hibernate.format_sql", "false");
            configuration.setProperties(props);

            for (Class<?> clazz : entityClasses) {
                configuration.addAnnotatedClass(clazz);
            }

            PluginHandler.get().getPlugin().addAnnotatedClass(configuration);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .addService(ConnectionProvider.class, new EchoConnectionProvider()).build();

            SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);

            return sessionFactory;
        } catch (Throwable ex) {
            logger.error("Failed to build session factory.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = buildSessionFactory();
        }

        return sessionFactory;
    }

    public static Connection get() {
        Connection conn = null;
        try {
            long time = System.currentTimeMillis();
            conn = cpds.getConnection();
            time = System.currentTimeMillis() - time;
            if (time >= 1000L) {
                logger.warn("Took a long time to get a connection: {} ms", time);
            }
        } catch (Exception e) {
            logger.error("Failed to get Connection from DbManager.", e);
        }

        return conn;
    }

    public static void close(Connection conn) {
        close(conn, (Statement) null, null);
    }

    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }

    public static void close(Connection conn, NamedParameterStatement stmt) {
        close(conn, stmt.getPrepareStatement(), null);
    }

    public static void close(Connection conn, NamedParameterStatement stmt, ResultSet rs) {
        close(conn, stmt.getPrepareStatement(), rs);
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                // Ignored
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                // Ignored
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    public static Session getSession() {
        String callerName = Thread.currentThread().getStackTrace()[2].getMethodName();
        Session session = getSessionFactory().getCurrentSession();
        if (session.getTransaction().isActive()) {
            logger.error("Transaction is active on checkout! Last checked out by: {}", sessionCheckouts.get(session));
        }

        sessionCheckouts.put(session, callerName);
        return session;
    }

    public static void closeSession(Session session) {
        if (session != null && session.isOpen()) {
            if (session.getTransaction().isActive()) {
                logger.error("Transaction is active on close! Last checked out by: {}", sessionCheckouts.get(session));
            }
            try {
                session.close();
            } catch (Exception e) {
                // Ignored
            }
        }

        sessionCheckouts.remove(session);
    }

    public static void rollbackAndClose(Session session) {
        if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
            try {
                session.getTransaction().rollback();
            } catch (Exception e) {
                // Ignored
            }
        }

        closeSession(session);
    }

    // ========================================================================
    // Transaction Wrappers - eliminate manual session/transaction boilerplate
    // ========================================================================

    /**
     * Execute a function within a transaction and return its result.
     * Handles session lifecycle, commit, and rollback automatically.
     * 
     * Usage:
     * 
     * <pre>
     * User user = DbManager.tx(session -> {
     *     return session.get(User.class, userId);
     * });
     * </pre>
     * 
     * @param <T>  Return type
     * @param work Function that receives Session and returns a result
     * @return Result from the work function, or null if an exception occurred
     */
    public static <T> T tx(Function<Session, T> work) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();

            T result = work.apply(session);

            session.getTransaction().commit();
            closeSession(session);
            return result;
        } catch (Exception e) {
            logger.error("Transaction failed.", e);
            rollbackAndClose(session);
            return null;
        }
    }

    /**
     * Execute a function within a transaction and return its result.
     * Throws exception on failure instead of returning null.
     * 
     * @param <T>  Return type
     * @param work Function that receives Session and returns a result
     * @return Result from the work function
     * @throws RuntimeException if transaction fails
     */
    public static <T> T txOrThrow(Function<Session, T> work) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();

            T result = work.apply(session);

            session.getTransaction().commit();
            closeSession(session);
            return result;
        } catch (Exception e) {
            logger.error("Transaction failed.", e);
            rollbackAndClose(session);
            throw new RuntimeException("Transaction failed", e);
        }
    }

    /**
     * Execute a void operation within a transaction.
     * Handles session lifecycle, commit, and rollback automatically.
     * 
     * Usage:
     * 
     * <pre>
     * DbManager.txVoid(session -> {
     *     User user = session.get(User.class, userId);
     *     user.setName("NewName");
     *     session.update(user);
     * });
     * </pre>
     * 
     * @param work Consumer that receives Session
     * @return true if successful, false if exception occurred
     */
    public static boolean txVoid(Consumer<Session> work) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();

            work.accept(session);

            session.getTransaction().commit();
            closeSession(session);
            return true;
        } catch (Exception e) {
            logger.error("Transaction failed.", e);
            rollbackAndClose(session);
            return false;
        }
    }

    /**
     * Execute a void operation within a transaction.
     * Throws exception on failure.
     * 
     * @param work Consumer that receives Session
     * @throws RuntimeException if transaction fails
     */
    public static void txVoidOrThrow(Consumer<Session> work) {
        Session session = null;
        try {
            session = getSession();
            session.beginTransaction();

            work.accept(session);

            session.getTransaction().commit();
            closeSession(session);
        } catch (Exception e) {
            logger.error("Transaction failed.", e);
            rollbackAndClose(session);
            throw new RuntimeException("Transaction failed", e);
        }
    }
}
