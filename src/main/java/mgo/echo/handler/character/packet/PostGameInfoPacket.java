package mgo.echo.handler.character.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Clan;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.User;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for post-game info (0x4129).
 * Buffer size: 0x8b bytes
 */
public final class PostGameInfoPacket {
    private static final Logger logger = LogManager.getLogger(PostGameInfoPacket.class);

    private static final int BUFFER_SIZE = 0x8b;
    private static final int NUM_SKILLS = 25;

    private PostGameInfoPacket() {
    }

    public static void write(ChannelHandlerContext ctx, User user, Character character) {
        ByteBuf bo = null;

        try {
            ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
            Clan clan = clanMember != null ? clanMember.getClan() : null;

            int experience = getCharacterExperience(user, character);
            int rwd = character.getId();
            int gradePoints = experience;

            bo = ctx.alloc().directBuffer(BUFFER_SIZE);

            bo.writeInt(0).writeByte(character.getRank());
            bo.writeInt(experience);
            bo.writeZero(1);

            writeSkillsData(bo);

            bo.writeInt(0).writeInt(gradePoints).writeInt(0xffffff);
            bo.writeInt(clan != null ? clan.getId() : 0);
            bo.writeShort(0).writeByte(1);

            boolean hasEmblem = clan != null && clan.getEmblem() != null;
            bo.writeByte(hasEmblem ? 3 : 0);

            bo.writeInt(rwd).writeZero(1);

            Packets.write(ctx, CharactersCmd.GET_POST_GAME_INFO_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing post-game info packet.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.GET_POST_GAME_INFO_RESPONSE, Error.GENERAL);
        }
    }

    private static int getCharacterExperience(User user, Character character) {
        boolean isMainCharacter = user.getMainCharacterId() != null
                && character.getId().equals(user.getMainCharacterId());
        return isMainCharacter ? user.getMainExp() : user.getAltExp();
    }

    private static void writeSkillsData(ByteBuf bo) {
        bo.writeInt(NUM_SKILLS);

        for (int i = 1; i <= NUM_SKILLS; i++) {
            int exp = (i == 17 || i == 20 || i == 22) ? 0x2000 : 0x6000;
            bo.writeByte(i).writeShort(exp).writeZero(1);
        }
    }
}
