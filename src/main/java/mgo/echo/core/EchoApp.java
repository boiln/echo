package mgo.echo.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import mgo.echo.data.entity.Clan;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.game.service.GameService;
import mgo.echo.handler.lobby.LobbyService;
import mgo.echo.lobby.AccountLobby;
import mgo.echo.lobby.BaseLobby;
import mgo.echo.lobby.GameLobby;
import mgo.echo.lobby.GateLobby;
import mgo.echo.plugin.PluginHandler;
import mgo.echo.session.ActiveLobbies;

public class EchoApp {
    private static final Logger logger = LogManager.getLogger();

    private EventLoopGroup bossGroup, workerGroup;

    public static boolean BIND_ON_ALL = true;
    public static int DB_WORKERS = 10;
    public static int SERVER_WORKERS = 10;

    public void test() {
        Session session = null;
        try {
            int clanId = 1;
            boolean isLeader = true;

            session = DbManager.getSession();
            session.beginTransaction();

            Query<Clan> query = session.createQuery(
                    "from Clan c join fetch c.members m join fetch m.character where c.id = :clan", Clan.class);
            query.setParameter("clan", clanId);

            Clan clan = query.uniqueResult();

            if (clan != null && isLeader) {
                Query<MessageClanApplication> queryM = session
                        .createQuery("from MessageClanApplication m join fetch m.character where m.clan = :clan",
                                MessageClanApplication.class);
                queryM.setParameter("clan", clan);
                clan.setApplications(queryM.list());
            }

            if (isLeader) {
                logger.debug("{} applications (2).", clan.getApplications().size());
            }

            session.getTransaction().commit();
            DbManager.closeSession(session);
        } catch (Exception e) {
            logger.error("Exception occurred!", e);
            DbManager.rollbackAndClose(session);
        }

        logger.debug("DONE!");
    }

    public EchoApp() {
        Properties properties = new Properties();
        String key = "";
        String dbUrl = null, dbUser = null, dbPassword = null;
        int dbPoolMin = 0, dbPoolMax = 0, dbPoolIncrement = 0;
        String plugin = null;
        ArrayList<Integer> lobbyIds = new ArrayList<>();

        try {
            properties.load(new FileInputStream(new File("echo.properties")));
            key = properties.getProperty("apikey");
            dbUrl = properties.getProperty("dbUrl");
            dbUser = properties.getProperty("dbUser");
            dbPassword = properties.getProperty("dbPassword");
            plugin = properties.getProperty("plugin");
            DB_WORKERS = Integer.parseInt(properties.getProperty("dbWorkers"));
            dbPoolMin = Integer.parseInt(properties.getProperty("dbPoolMin"));
            dbPoolMax = Integer.parseInt(properties.getProperty("dbPoolMax"));
            dbPoolIncrement = Integer.parseInt(properties.getProperty("dbPoolIncrement"));
            SERVER_WORKERS = Integer.parseInt(properties.getProperty("serverWorkers"));

            String strLobbies = properties.getProperty("lobbies");
            String[] strsLobbies = strLobbies.split(",");
            for (String lobStr : strsLobbies) {
                int id = Integer.parseInt(lobStr);
                lobbyIds.add(id);
            }
        } catch (Exception e) {
            logger.error("Error while reading properties file.", e);
            return;
        }

        if (plugin != null) {
            try {
                PluginHandler.get().loadPlugin(plugin);
            } catch (Exception e) {
                logger.error("Error while loading plugin.");
            }
        }

        PluginHandler.get().getPlugin().initialize();

        DbManager.initialize(dbUrl, dbUser, dbPassword, dbPoolMin, dbPoolMax, dbPoolIncrement);

        PluginHandler.get().getPlugin().onStart();

        logger.info("Starting server ..");

        Session session = null;
        try {
            ArrayList<Lobby> lobbies = new ArrayList<>();

            session = DbManager.getSession();
            session.beginTransaction();

            for (Integer lobbyId : lobbyIds) {
                Lobby lobby = session.get(Lobby.class, lobbyId);
                lobbies.add(lobby);
            }

            session.getTransaction().commit();
            DbManager.closeSession(session);

            for (Lobby lobby : lobbies) {
                if (lobby.getType() < 0 || lobby.getType() > 2) {
                    continue;
                }
                ActiveLobbies.add(lobby);
            }
            LobbyService.initializeLobbies();

            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(SERVER_WORKERS, new ThreadFactory() {

                private int counter = 1;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Executor " + counter++);
                }
            });

            ArrayList<EchoServer> servers = new ArrayList<>();
            for (Lobby lobby : lobbies) {
                BaseLobby nLobby = null;
                if (lobby.getType() == 0) {
                    nLobby = new GateLobby(lobby);
                } else if (lobby.getType() == 1) {
                    nLobby = new AccountLobby(lobby);
                } else if (lobby.getType() == 2) {
                    nLobby = new GameLobby(lobby);
                }
                EchoServer nServer = new EchoServer(nLobby, bossGroup, workerGroup, executorGroup);
                servers.add(nServer);
            }

            for (EchoServer server : servers) {
                server.start();
            }

            logger.info("Started server.");

            PluginHandler.get().getPlugin().onStart();

            EchoService service = new EchoService(() -> {
                LobbyService.updateLobbies();
                GameService.cleanup();
                return true;
            }, 60);
            service.start();

            ChannelFuture future = servers.get(0).getFuture();

            try {
                future.sync();

                future.channel().closeFuture().sync();
            } catch (Exception e) {
                logger.error(e);
            }
        } catch (Exception e) {
            logger.error("Exception while starting server.", e);
            DbManager.rollbackAndClose(session);
        } finally {
            logger.info("Shutting down server ..");

            if (bossGroup != null && workerGroup != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }

            logger.info("Shut down server.");
        }
    }

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public static void main(String[] args) {
        try {
            new EchoApp();
        } catch (Exception e) {
            logger.error("Failed to start EchoApp.", e);
        }
    }
}
