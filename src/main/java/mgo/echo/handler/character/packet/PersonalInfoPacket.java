package mgo.echo.handler.character.packet;

import java.time.Instant;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterAppearance;
import mgo.echo.data.entity.CharacterEquippedSkills;
import mgo.echo.data.entity.Clan;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.handler.character.service.CharacterService;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for personal info response (0x4122).
 * Buffer size: 0xf5 bytes (get), 0xba bytes (update response)
 */
public final class PersonalInfoPacket {
    private static final Logger logger = LogManager.getLogger(PersonalInfoPacket.class);

    private static final int BUFFER_SIZE = 0xf5;
    private static final int UPDATE_RESPONSE_SIZE = 0xba;
    private static final int SKILL_EXP = 0x600000;

    private static final byte[] BYTES_1 = {
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };

    private static final byte[] BYTES_3 = { (byte) 0x00, (byte) 0xA7, (byte) 0x00, (byte) 0x0D };

    private PersonalInfoPacket() {
    }

    public static void write(ChannelHandlerContext ctx, Character character) {
        ByteBuf bo = null;

        try {
            CharacterAppearance appearance = character.getAppearance().get(0);
            CharacterEquippedSkills skills = getOrCreateSkills(character);

            ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
            Clan clan = clanMember != null ? clanMember.getClan() : null;

            int time1 = (int) Instant.now().getEpochSecond();
            int rwd = character.getId();

            bo = ctx.alloc().directBuffer(BUFFER_SIZE);

            writeClanInfo(bo, clan);
            bo.writeBytes(BYTES_1).writeInt(time1);
            writeAppearance(bo, appearance);
            writeSkills(bo, skills);
            writeSkillExp(bo);
            bo.writeInt(rwd);
            writeComment(bo, character);
            bo.writeByte(character.getRank());
            writeClanEmblemFlag(bo, clan);
            bo.writeBytes(BYTES_3);

            Packets.write(ctx, CharactersCmd.GET_PERSONAL_INFO_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing personal info packet.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.GET_PERSONAL_INFO_RESPONSE, Error.GENERAL);
        }
    }

    private static CharacterEquippedSkills getOrCreateSkills(Character character) {
        List<CharacterEquippedSkills> skillsList = character.getSkills();

        if (skillsList.size() <= 0) {
            CharacterEquippedSkills skills = new CharacterEquippedSkills();
            skills.setCharacter(character);
            skillsList.add(skills);
        }

        return skillsList.get(0);
    }

    private static void writeClanInfo(ByteBuf bo, Clan clan) {
        if (clan != null) {
            bo.writeInt(clan.getId());
            Util.writeString(clan.getName(), 16, bo);
            return;
        }
        bo.writeInt(0);
        Util.writeString("", 16, bo);
    }

    private static void writeAppearance(ByteBuf bo, CharacterAppearance appearance) {
        bo.writeByte(appearance.getGender()).writeByte(appearance.getFace()).writeByte(appearance.getUpper())
                .writeByte(appearance.getLower()).writeByte(appearance.getFacePaint())
                .writeByte(appearance.getUpperColor()).writeByte(appearance.getLowerColor())
                .writeByte(appearance.getVoice()).writeByte(appearance.getPitch()).writeZero(4)
                .writeByte(appearance.getHead()).writeByte(appearance.getChest()).writeByte(appearance.getHands())
                .writeByte(appearance.getWaist()).writeByte(appearance.getFeet())
                .writeByte(appearance.getAccessory1()).writeByte(appearance.getAccessory2())
                .writeByte(appearance.getHeadColor()).writeByte(appearance.getChestColor())
                .writeByte(appearance.getHandsColor()).writeByte(appearance.getWaistColor())
                .writeByte(appearance.getFeetColor()).writeByte(appearance.getAccessory1Color())
                .writeByte(appearance.getAccessory2Color());
    }

    private static void writeSkills(ByteBuf bo, CharacterEquippedSkills skills) {
        bo.writeByte(skills.getSkill1()).writeByte(skills.getSkill2()).writeByte(skills.getSkill3())
                .writeByte(skills.getSkill4()).writeZero(1).writeByte(skills.getLevel1())
                .writeByte(skills.getLevel2()).writeByte(skills.getLevel3()).writeByte(skills.getLevel4())
                .writeZero(1);
    }

    private static void writeSkillExp(ByteBuf bo) {
        bo.writeInt(SKILL_EXP).writeInt(SKILL_EXP).writeInt(SKILL_EXP).writeInt(SKILL_EXP).writeZero(5);
    }

    private static void writeComment(ByteBuf bo, Character character) {
        if (character.getComment() != null) {
            Util.writeString(character.getComment(), 128, bo);
            return;
        }
        bo.writeZero(128);
    }

    private static void writeClanEmblemFlag(ByteBuf bo, Clan clan) {
        boolean hasEmblem = clan != null && clan.getEmblem() != null;
        bo.writeByte(hasEmblem ? 3 : 0);
    }

    /**
     * Read personal info update from packet
     */
    public static CharacterService.PersonalInfoUpdate readUpdate(Packet in) {
        ByteBuf bi = in.getPayload();
        CharacterService.PersonalInfoUpdate update = new CharacterService.PersonalInfoUpdate();

        update.upper = bi.readUnsignedByte();
        update.lower = bi.readUnsignedByte();
        update.facePaint = bi.readUnsignedByte();
        update.upperColor = bi.readUnsignedByte();
        update.lowerColor = bi.readUnsignedByte();
        update.head = bi.readUnsignedByte();
        update.chest = bi.readUnsignedByte();
        update.hands = bi.readUnsignedByte();
        update.waist = bi.readUnsignedByte();
        update.feet = bi.readUnsignedByte();
        update.accessory1 = bi.readUnsignedByte();
        update.accessory2 = bi.readUnsignedByte();
        update.headColor = bi.readUnsignedByte();
        update.chestColor = bi.readUnsignedByte();
        update.handsColor = bi.readUnsignedByte();
        update.waistColor = bi.readUnsignedByte();
        update.feetColor = bi.readUnsignedByte();
        update.accessory1Color = bi.readUnsignedByte();
        update.accessory2Color = bi.readUnsignedByte();

        update.skill1 = bi.readByte();
        update.skill2 = bi.readByte();
        update.skill3 = bi.readByte();
        update.skill4 = bi.readByte();
        bi.skipBytes(1);
        update.level1 = bi.readByte();
        update.level2 = bi.readByte();
        update.level3 = bi.readByte();
        update.level4 = bi.readByte();

        bi.skipBytes(2);
        update.comment = Util.readString(bi, 128);

        return update;
    }

    /**
     * Write personal info update response
     */
    public static void writeUpdateResponse(ChannelHandlerContext ctx, CharacterService.PersonalInfoUpdate update) {
        ByteBuf bo = null;

        try {
            bo = ctx.alloc().directBuffer(UPDATE_RESPONSE_SIZE);

            bo.writeInt(0);
            bo.writeByte(update.upper).writeByte(update.lower).writeByte(update.facePaint)
                    .writeByte(update.upperColor).writeByte(update.lowerColor)
                    .writeByte(update.head).writeByte(update.chest).writeByte(update.hands)
                    .writeByte(update.waist).writeByte(update.feet)
                    .writeByte(update.accessory1).writeByte(update.accessory2)
                    .writeByte(update.headColor).writeByte(update.chestColor)
                    .writeByte(update.handsColor).writeByte(update.waistColor).writeByte(update.feetColor)
                    .writeByte(update.accessory1Color).writeByte(update.accessory2Color)
                    .writeByte(update.skill1).writeByte(update.skill2).writeByte(update.skill3).writeByte(update.skill4)
                    .writeZero(1)
                    .writeByte(update.level1).writeByte(update.level2).writeByte(update.level3).writeByte(update.level4)
                    .writeZero(1);

            for (int i = 0; i < 4; i++) {
                bo.writeInt(SKILL_EXP);
            }
            bo.writeZero(5);

            Util.writeString(update.comment, 128, bo);

            Packets.write(ctx, CharactersCmd.UPDATE_PERSONAL_INFO_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing personal info update response.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.UPDATE_PERSONAL_INFO_RESPONSE, Error.GENERAL);
        }
    }
}
