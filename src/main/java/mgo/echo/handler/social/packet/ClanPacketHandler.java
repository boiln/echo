package mgo.echo.handler.social.packet;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.query.Query;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Clan;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.account.service.AccountService;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.command.ClansCmd;
import mgo.echo.session.ActiveGames;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

public class ClanPacketHandler {
    private static final Logger logger = LogManager.getLogger();

    public static void getEmblem(ChannelHandlerContext ctx, Packet in, int command, boolean getWip) {
        ByteBuf bo = null;

        try {
            ByteBuf bi = in.getPayload();
            int clanId = bi.readInt();

            Clan clan = DbManager.tx(session -> session.get(Clan.class, clanId));

            if (clan == null) {
                Packets.write(ctx, command, Error.CLAN_DOESNOTEXIST);
                return;
            }

            bo = ctx.alloc().directBuffer(4 + 565);
            bo.writeInt(0);
            writeEmblemData(bo, clan, getWip);

            Packets.write(ctx, command, bo);
        } catch (Exception e) {
            logger.error("Exception while getting clan emblem.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, command, Error.GENERAL);
        }
    }

    private static void writeEmblemData(ByteBuf bo, Clan clan, boolean getWip) {
        if (getWip && clan.getEmblemWip() != null) {
            bo.writeBytes(clan.getEmblemWip());
            return;
        }

        if (clan.getEmblem() != null) {
            bo.writeBytes(clan.getEmblem());
            return;
        }

        bo.writeZero(565);
    }

    public static void getList(ChannelHandlerContext ctx, Packet in) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();

        try {
            List<Clan> clans = DbManager.tx(session -> {
                Query<Clan> query = session.createQuery("from Clan c join fetch c.leader l join fetch l.character",
                        Clan.class);
                return query.list();
            });

            Packets.handleMutliElementPayload(ctx, clans.size(), 15, 48, payloads, (i, bo) -> {
                Clan clan = clans.get(i);
                writeClanListEntry(bo, clan);
            });

            Packets.write(ctx, ClansCmd.GET_LIST_START, 0);
            Packets.write(ctx, ClansCmd.GET_LIST_DATA, payloads);
            Packets.write(ctx, ClansCmd.GET_LIST_END, 0);
        } catch (Exception e) {
            logger.error("Exception while getting clan list.", e);
            Util.releaseBuffers(payloads);
            Packets.write(ctx, ClansCmd.GET_LIST_START, Error.GENERAL);
        }
    }

    private static void writeClanListEntry(ByteBuf bo, Clan clan) {
        boolean isNew = false;
        int time = (int) Instant.now().getEpochSecond();

        bo.writeInt(clan.getId());
        Util.writeString(clan.getName(), 16, bo);
        bo.writeInt(clan.getLeader().getCharacterId());
        Util.writeString(clan.getLeader().getCharacter().getName(), 16, bo);
        bo.writeBoolean(isNew).writeByte(0).writeByte(0).writeByte(0).writeInt(time);
    }

    public static void search(ChannelHandlerContext ctx, Packet in) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();

        try {
            ByteBuf bi = in.getPayload();
            boolean exactOnly = bi.readBoolean();
            @SuppressWarnings("unused")
            boolean caseSensitive = bi.readBoolean();
            String name = Util.readString(bi, 0x10);

            String searchName = exactOnly ? name : "%" + name + "%";

            List<Clan> clans = DbManager.tx(session -> {
                Query<Clan> query = session.createQuery(
                        "from Clan c join fetch c.leader l join fetch l.character where c.name like :name", Clan.class);
                query.setParameter("name", searchName);
                return query.list();
            });

            Packets.handleMutliElementPayload(ctx, clans.size(), 15, 48, payloads, (i, bo) -> {
                Clan clan = clans.get(i);
                writeClanListEntry(bo, clan);
            });

            Packets.write(ctx, ClansCmd.SEARCH_START, 0);
            Packets.write(ctx, ClansCmd.SEARCH_DATA, payloads);
            Packets.write(ctx, ClansCmd.SEARCH_END, 0);
        } catch (Exception e) {
            logger.error("Exception while searching for clan.", e);
            Util.releaseBuffers(payloads);
            Packets.write(ctx, ClansCmd.SEARCH_START, Error.GENERAL);
        }
    }

    public static void getInformationMember(ChannelHandlerContext ctx, Packet in) {
        ByteBuf bo = null;

        try {
            User user = ActiveUsers.get(ctx.channel());
            if (user == null) {
                logger.error("Error while getting clan information (member): No User.");
                Packets.write(ctx, ClansCmd.GET_INFORMATION_MEMBER_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            AccountService.updateUserClan(ctx);

            Character character = user.getCurrentCharacter();
            ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

            ByteBuf bi = in.getPayload();
            int clanId = bi.readInt();

            Clan clan = DbManager.tx(session -> {
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

            if (clan == null) {
                Packets.write(ctx, ClansCmd.GET_INFORMATION_MEMBER_RESPONSE, Error.CLAN_DOESNOTEXIST);
                return;
            }

            int gradePoints = clan.getId();

            bo = ctx.alloc().directBuffer(777);
            bo.writeInt(0).writeInt(clan.getId());
            Util.writeString(clan.getName(), 16, bo);
            bo.writeByte(0);

            // Write leader info
            if (clan.getLeader() != null) {
                bo.writeInt(clan.getLeader().getCharacterId());
                Util.writeString(clan.getLeader().getCharacter().getName(), 16, bo);
            } else {
                bo.writeInt(0);
                Util.writeString("", 16, bo);
            }

            bo.writeInt(0);
            Util.writeString("", 16, bo);
            bo.writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0)
                    .writeByte(0).writeByte(0);

            // Emblem status
            bo.writeByte(clan.getEmblemWip() != null ? 2 : 0);
            bo.writeByte(clan.getEmblem() != null ? 3 : 0);

            // Comment
            String comment = clan.getComment() != null ? clan.getComment() : "No comment.";
            Util.writeString(comment, 128, bo);

            // Emblem editor
            if (clanMember != null) {
                if (clan.getEmblemEditor() != null) {
                    bo.writeInt(clan.getEmblemEditor().getCharacterId());
                } else if (clan.getLeader() != null) {
                    bo.writeInt(clan.getLeader().getCharacterId());
                } else {
                    bo.writeInt(0);
                }
            } else {
                bo.writeInt(0);
            }

            // Notice
            if (clan.getNotice() != null) {
                Util.writeString(clan.getNotice(), 512, bo);
                bo.writeInt(clan.getNoticeTime());
                String writerName = clan.getNoticeWriter() != null
                        ? clan.getNoticeWriter().getCharacter().getName()
                        : "[Deleted]";
                Util.writeString(writerName, 16, bo);
            } else {
                Util.writeString("", 512, bo);
                bo.writeInt(0);
                Util.writeString("", 16, bo);
            }

            bo.writeInt(0).writeInt(0).writeInt(gradePoints);

            Packets.write(ctx, ClansCmd.GET_INFORMATION_MEMBER_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while getting clan information (member).", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, ClansCmd.GET_INFORMATION_MEMBER_RESPONSE, Error.GENERAL);
        }
    }

    public static void getInformation(ChannelHandlerContext ctx, Packet in) {
        ByteBuf bo = null;

        try {
            User user = ActiveUsers.get(ctx.channel());
            if (user == null) {
                logger.error("Error while getting clan information: No User.");
                Packets.write(ctx, ClansCmd.GET_INFORMATION_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            AccountService.updateUserClan(ctx);

            ByteBuf bi = in.getPayload();
            int clanId = bi.readInt();

            Clan clan = DbManager.tx(session -> {
                Query<Clan> query = session.createQuery(
                        "from Clan c join fetch c.leader l join fetch l.character join fetch c.members where c.id=:clanId",
                        Clan.class);
                query.setParameter("clanId", clanId);
                return query.uniqueResult();
            });

            if (clan == null) {
                Packets.write(ctx, ClansCmd.GET_INFORMATION_RESPONSE, Error.CLAN_DOESNOTEXIST);
                return;
            }

            List<ClanMember> members = clan.getMembers();
            int totalReward = clan.getId();

            bo = ctx.alloc().directBuffer(217);
            bo.writeInt(0).writeInt(clan.getId());
            Util.writeString(clan.getName(), 16, bo);
            bo.writeInt(clan.getLeader().getCharacter().getId());
            Util.writeString(clan.getLeader().getCharacter().getName(), 16, bo);
            bo.writeByte(clan.getEmblem() != null ? 3 : 0);

            String comment = clan.getComment() != null ? clan.getComment() : "No comment.";
            Util.writeString(comment, 128, bo);

            bo.writeInt(0).writeInt(members.size()).writeInt(totalReward)
                    .writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0)
                    .writeInt(0).writeInt(0).writeInt(0);

            Packets.write(ctx, ClansCmd.GET_INFORMATION_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while getting clan information.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, ClansCmd.GET_INFORMATION_RESPONSE, Error.GENERAL);
        }
    }

    public static void getRoster(ChannelHandlerContext ctx, Packet in) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
        AtomicReference<ByteBuf[]> payloads2 = new AtomicReference<>();

        try {
            User user = ActiveUsers.get(ctx.channel());
            if (user == null) {
                logger.error("Error while getting clan roster: No User.");
                Packets.write(ctx, ClansCmd.GET_ROSTER_START, Error.INVALID_SESSION);
                return;
            }

            AccountService.updateUserClan(ctx);

            Character character = user.getCurrentCharacter();
            ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

            ByteBuf bi = in.getPayload();
            int clanId = bi.readInt();

            Clan clan = DbManager.tx(session -> {
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

            if (clan == null) {
                Packets.write(ctx, ClansCmd.GET_ROSTER_START, Error.CLAN_DOESNOTEXIST);
                return;
            }

            List<ClanMember> members = clan.getMembers();

            Packets.handleMutliElementPayload(ctx, members.size(), 15, 68, payloads, (i, bo) -> {
                ClanMember member = members.get(i);
                Character cCharacter = member.getCharacter();

                Game game = ActiveGames.get((g) -> {
                    for (Player player : g.getPlayers()) {
                        if (player.getCharacterId().equals(cCharacter.getId())) {
                            return true;
                        }
                    }
                    return false;
                });

                boolean isMember = true;
                int rewards = cCharacter.getId();

                bo.writeInt(cCharacter.getId());
                Util.writeString(cCharacter.getName(), 16, bo);
                bo.writeBoolean(isMember).writeInt(0).writeInt(rewards);

                if (game != null) {
                    bo.writeShort(game.getLobbyId());
                    Util.writeString(game.getLobby().getName(), 16, bo);
                    bo.writeInt(game.getId());
                    Util.writeString(game.getHost().getName(), 16, bo);
                    bo.writeByte(game.getLobby().getSubtype());
                } else {
                    bo.writeShort(0);
                    Util.writeString("", 16, bo);
                    bo.writeInt(0);
                    Util.writeString("", 16, bo);
                    bo.writeByte(0);
                }
            });

            if (clanMember != null && clan.getLeader() != null && clanMember.getId().equals(clan.getLeader().getId())) {
                List<MessageClanApplication> applications = clan.getApplications();

                Packets.handleMutliElementPayload(ctx, applications.size(), 15, 68, payloads2, (i, bo) -> {
                    MessageClanApplication application = applications.get(i);
                    Character cCharacter = application.getCharacter();

                    Game game = ActiveGames.get((g) -> {
                        for (Player player : g.getPlayers()) {
                            if (player.getCharacterId().equals(cCharacter.getId())) {
                                return true;
                            }
                        }
                        return false;
                    });

                    boolean isMember = false;
                    int rewards = cCharacter.getId();

                    bo.writeInt(cCharacter.getId());
                    Util.writeString(cCharacter.getName(), 16, bo);
                    bo.writeBoolean(isMember).writeInt(0).writeInt(rewards);

                    if (game != null) {
                        bo.writeShort(game.getLobbyId());
                        Util.writeString(game.getLobby().getName(), 16, bo);
                        bo.writeInt(game.getId());
                        Util.writeString(game.getHost().getName(), 16, bo);
                        bo.writeByte(game.getLobby().getSubtype());
                    } else {
                        bo.writeShort(0);
                        Util.writeString("", 16, bo);
                        bo.writeInt(0);
                        Util.writeString("", 16, bo);
                        bo.writeByte(0);
                    }
                });
            }

            Packets.write(ctx, ClansCmd.GET_ROSTER_START, 0);
            Packets.write(ctx, ClansCmd.GET_ROSTER_DATA, payloads);
            Packets.write(ctx, ClansCmd.GET_ROSTER_DATA, payloads2);
            Packets.write(ctx, ClansCmd.GET_ROSTER_END, 0);
        } catch (Exception e) {
            logger.error("Exception while getting clan roster.", e);
            Util.releaseBuffers(payloads);
            Util.releaseBuffers(payloads2);
            Packets.write(ctx, ClansCmd.GET_ROSTER_START, Error.GENERAL);
        }
    }

    public static void updateState(ChannelHandlerContext ctx, Packet in) {
        ByteBuf[] payloads = null;

        try {
            User user = ActiveUsers.get(ctx.channel());
            if (user == null) {
                logger.error("Error while getting clan info: No User.");
                Packets.write(ctx, ClansCmd.UPDATE_STATE_RESPONSE, Error.INVALID_SESSION);
                return;
            }

            AccountService.updateUserClan(ctx);

            Character character = user.getCurrentCharacter();
            MessageClanApplication clanApplication = Util.getFirstOrNull(character.getClanApplication());
            ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

            // Case 1: Character is a clan member
            if (clanMember != null) {
                Clan clan = clanMember.getClan();
                int notifications = 0;

                // Leader gets notifications count
                if (clanMember.getId().equals(clan.getLeaderId())) {
                    List<MessageClanApplication> applications = DbManager.tx(session -> {
                        Query<MessageClanApplication> query = session.createQuery(
                                "from MessageClanApplication a where a.clan = :clan", MessageClanApplication.class);
                        query.setParameter("clan", clan);
                        return query.list();
                    });
                    notifications = applications.isEmpty() ? 0 : 0b100000000;
                }

                payloads = new ByteBuf[2];
                for (int i = 0; i < payloads.length; i++) {
                    ByteBuf bo = ctx.alloc().directBuffer(28);
                    bo.writeInt(0).writeInt(clan.getId());
                    bo.writeByte(clanMember.getId().equals(clan.getLeaderId()) ? 2 : 1);
                    bo.writeShort(notifications);
                    bo.writeByte(clan.getEmblem() != null ? 3 : 0);
                    Util.writeString(clan.getName(), 16, bo);
                    payloads[i] = bo;
                }
                Packets.write(ctx, ClansCmd.UPDATE_STATE_RESPONSE, payloads);
                return;
            }

            // Case 2: Character has a pending application
            if (clanApplication != null) {
                Clan clan = clanApplication.getClan();

                payloads = new ByteBuf[2];
                for (int i = 0; i < payloads.length; i++) {
                    ByteBuf bo = ctx.alloc().directBuffer(28);
                    bo.writeInt(0).writeInt(clan.getId());
                    bo.writeByte(0);
                    bo.writeShort(0);
                    bo.writeByte(clan.getEmblem() != null ? 3 : 0);
                    Util.writeString(clan.getName(), 16, bo);
                    payloads[i] = bo;
                }
                Packets.write(ctx, ClansCmd.UPDATE_STATE_RESPONSE, payloads);
                return;
            }

            // Case 3: Not in any clan
            payloads = new ByteBuf[1];
            ByteBuf bo = ctx.alloc().directBuffer(28);
            bo.writeInt(0).writeInt(0);
            bo.writeByte(0xff);
            bo.writeShort(0);
            bo.writeByte(0);
            Util.writeString("", 16, bo);
            payloads[0] = bo;

            Packets.write(ctx, ClansCmd.UPDATE_STATE_RESPONSE, payloads);
        } catch (Exception e) {
            logger.error("Exception while updating clan state.", e);
            Util.releaseBuffers(payloads);
            Packets.write(ctx, ClansCmd.UPDATE_STATE_RESPONSE, Error.GENERAL);
        }
    }

    public static void getStats(ChannelHandlerContext ctx, Packet in) {
        try {
            ByteBuf bo1 = Util.readFile(Util.assetFile("4b71.bin"));
            ByteBuf bo2 = Util.readFile(Util.assetFile("4b72.bin"));
            Packets.write(ctx, ClansCmd.GET_STATS_RESPONSE, bo1);
            Packets.write(ctx, ClansCmd.GET_STATS_RESPONSE + 1, bo2);
        } catch (Exception e) {
            logger.error("Exception while getting clan stats.", e);
            Packets.write(ctx, ClansCmd.GET_STATS_RESPONSE, Error.GENERAL);
        }
    }
}
