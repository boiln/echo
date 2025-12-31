package mgo.echo.controller;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.News;
import mgo.echo.data.repository.DbManager;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.session.ActiveLobbies;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Gate lobby controller.
 *
 * Handles:
 * - Lobby List
 * - News
 */
public class GateController implements Controller {
    private static final Logger logger = LogManager.getLogger(GateController.class);

    @Command(0x2005)
    public boolean getLobbyList(CommandContext ctx) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();

        try {
            Collection<Lobby> lobbies = ActiveLobbies.get().values();
            Iterator<Lobby> iterator = lobbies.iterator();

            Packets.handleMutliElementPayload(ctx.nettyCtx(), lobbies.size(), 22, 0x2e, payloads, (i, bo) -> {
                Lobby lobby = iterator.next();

                boolean beginner = false;
                boolean expansion = false;
                boolean noHeadshot = false;

                int restriction = 0;
                restriction |= beginner ? 0b1 : 0;
                restriction |= expansion ? 0b1000 : 0;
                restriction |= noHeadshot ? 0b10000 : 0;

                bo.writeInt(i).writeInt(lobby.getType());
                Util.writeString(lobby.getName(), 16, bo);
                Util.writeString(lobby.getIp(), 15, bo);
                bo.writeShort(lobby.getPort()).writeShort(lobby.getPlayers()).writeShort(lobby.getId())
                        .writeByte(restriction);
            });

            Packets.write(ctx.nettyCtx(), 0x2002, 0);
            Packets.write(ctx.nettyCtx(), 0x2003, payloads);
            Packets.write(ctx.nettyCtx(), 0x2004, 0);
            return true;
        } catch (Exception e) {
            logger.error("Exception while getting lobby list.", e);
            Util.releaseBuffers(payloads);
            Packets.write(ctx.nettyCtx(), 0x2002, Error.GENERAL);
            return false;
        }
    }

    @Command(0x2008)
    public boolean getNews(CommandContext ctx) {
        ByteBuf[] bos = null;
        Session session = null;

        try {
            session = DbManager.getSession();
            session.beginTransaction();

            Query<News> query = session.createQuery("from News n order by n.id desc", News.class);
            List<News> news = query.list();

            session.getTransaction().commit();
            DbManager.closeSession(session);

            int newsItems = news.size();
            bos = new ByteBuf[newsItems];

            for (int i = 0; i < newsItems; i++) {
                News newsItem = news.get(i);
                String message = newsItem.getMessage();

                int length = Math.min(message.length(), Packet.MAX_PAYLOAD_LENGTH - 138);
                message = message.substring(0, length);

                bos[i] = ctx.nettyCtx().alloc().directBuffer(138 + length);
                ByteBuf bo = bos[i];

                bo.writeInt(newsItem.getId()).writeBoolean(newsItem.getImportant()).writeInt(newsItem.getTime());
                Util.writeString(newsItem.getTopic(), 128, bo);
                Util.writeString(message, length + 1, bo);
            }

            Packets.write(ctx.nettyCtx(), 0x2009, 0);
            Packets.write(ctx.nettyCtx(), 0x200a, bos);
            Packets.write(ctx.nettyCtx(), 0x200b, 0);
            return true;
        } catch (Exception e) {
            logger.error("Exception while getting news.", e);
            DbManager.rollbackAndClose(session);
            Util.releaseBuffers(bos);
            Packets.write(ctx.nettyCtx(), 0x2009, Error.GENERAL);
            return false;
        }
    }
}
