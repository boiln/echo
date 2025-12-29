package mgo.echo.handler.character.packet;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterSetGear;
import mgo.echo.data.entity.CharacterSetSkills;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for skill sets (0x4140) and gear sets (0x4142).
 */
public final class SetsPacket {
    private static final Logger logger = LogManager.getLogger(SetsPacket.class);

    private static final int SKILL_SET_SIZE = 0x4d;
    private static final int GEAR_SET_SIZE = 0x57;
    private static final int SET_COUNT = 3;

    private SetsPacket() {
    }

    // ========================================================================
    // Skill Sets
    // ========================================================================

    public static void writeSkillSets(ChannelHandlerContext ctx, List<CharacterSetSkills> sets) {
        ByteBuf bo = null;

        try {
            bo = ctx.alloc().directBuffer(SKILL_SET_SIZE * sets.size());

            for (CharacterSetSkills set : sets) {
                bo.writeInt(set.getModes())
                        .writeByte(set.getSkill1()).writeByte(set.getSkill2())
                        .writeByte(set.getSkill3()).writeByte(set.getSkill4())
                        .writeZero(1)
                        .writeByte(set.getLevel1()).writeByte(set.getLevel2())
                        .writeByte(set.getLevel3()).writeByte(set.getLevel4())
                        .writeZero(1);
                Util.writeString(set.getName(), 63, bo, StandardCharsets.UTF_8);
            }

            Packets.write(ctx, CharactersCmd.GET_SKILL_SETS_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing skill sets packet.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.GET_SKILL_SETS_RESPONSE, Error.GENERAL);
        }
    }

    public static List<CharacterSetSkills> initializeSkillSets(Character character) {
        List<CharacterSetSkills> sets = character.getSetsSkills();

        for (int index = 0; index < SET_COUNT; index++) {
            CharacterSetSkills set = new CharacterSetSkills();
            set.setCharacterId(character.getId());
            set.setCharacter(character);
            set.setIndex(index);
            set.setName("");
            set.setModes(0);
            set.setSkill1(0);
            set.setSkill2(0);
            set.setSkill3(0);
            set.setSkill4(0);
            set.setLevel1(0);
            set.setLevel2(0);
            set.setLevel3(0);
            set.setLevel4(0);
            sets.add(set);
        }

        return sets;
    }

    public static void readSkillSetsUpdate(Packet in, List<CharacterSetSkills> sets) {
        ByteBuf bi = in.getPayload();

        for (int i = 0; i < SET_COUNT; i++) {
            CharacterSetSkills set = sets.get(i);

            int modes = bi.readInt();
            int skill1 = bi.readByte();
            int skill2 = bi.readByte();
            int skill3 = bi.readByte();
            int skill4 = bi.readByte();
            bi.skipBytes(1);
            int level1 = bi.readByte();
            int level2 = bi.readByte();
            int level3 = bi.readByte();
            int level4 = bi.readByte();
            bi.skipBytes(1);
            String name = Util.readString(bi, 63, StandardCharsets.UTF_8);

            set.setName(name);
            set.setModes(modes);
            set.setSkill1(skill1);
            set.setSkill2(skill2);
            set.setSkill3(skill3);
            set.setSkill4(skill4);
            set.setLevel1(level1);
            set.setLevel2(level2);
            set.setLevel3(level3);
            set.setLevel4(level4);
        }
    }

    // ========================================================================
    // Gear Sets
    // ========================================================================

    public static void writeGearSets(ChannelHandlerContext ctx, List<CharacterSetGear> sets) {
        ByteBuf bo = null;

        try {
            bo = ctx.alloc().directBuffer(GEAR_SET_SIZE * sets.size());

            for (CharacterSetGear set : sets) {
                bo.writeInt(set.getStages())
                        .writeByte(set.getFace()).writeByte(set.getHead())
                        .writeByte(set.getUpper()).writeByte(set.getLower())
                        .writeByte(set.getChest()).writeByte(set.getWaist())
                        .writeByte(set.getHands()).writeByte(set.getFeet())
                        .writeByte(set.getAccessory1()).writeByte(set.getAccessory2())
                        .writeByte(set.getHeadColor()).writeByte(set.getUpperColor())
                        .writeByte(set.getLowerColor()).writeByte(set.getChestColor())
                        .writeByte(set.getWaistColor()).writeByte(set.getHandsColor())
                        .writeByte(set.getFeetColor()).writeByte(set.getAccessory1Color())
                        .writeByte(set.getAccessory2Color()).writeByte(set.getFacePaint());
                Util.writeString(set.getName(), 63, bo, StandardCharsets.UTF_8);
            }

            Packets.write(ctx, CharactersCmd.GET_GEAR_SETS_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing gear sets packet.", e);
            Packets.writeError(ctx, CharactersCmd.GET_GEAR_SETS_RESPONSE, 1);
            Util.releaseBuffer(bo);
        }
    }

    public static List<CharacterSetGear> initializeGearSets(Character character) {
        List<CharacterSetGear> sets = character.getSetsGear();

        for (int index = 0; index < SET_COUNT; index++) {
            CharacterSetGear set = new CharacterSetGear();
            set.setCharacterId(character.getId());
            set.setCharacter(character);
            set.setIndex(index);
            set.setFace(0);
            set.setHead(0);
            set.setHeadColor(0);
            set.setUpper(0);
            set.setUpperColor(0);
            set.setLower(0);
            set.setLowerColor(0);
            set.setChest(0);
            set.setChestColor(0);
            set.setWaist(0);
            set.setWaistColor(0);
            set.setHands(0);
            set.setHandsColor(0);
            set.setFeet(0);
            set.setFeetColor(0);
            set.setAccessory1(0);
            set.setAccessory1Color(0);
            set.setAccessory2(0);
            set.setAccessory2Color(0);
            set.setFacePaint(0);
            sets.add(set);
        }

        return sets;
    }

    public static void readGearSetsUpdate(Packet in, List<CharacterSetGear> sets) {
        ByteBuf bi = in.getPayload();

        for (int i = 0; i < SET_COUNT; i++) {
            CharacterSetGear set = sets.get(i);

            int stages = bi.readInt();
            int face = bi.readUnsignedByte();
            int head = bi.readUnsignedByte();
            int upper = bi.readUnsignedByte();
            int lower = bi.readUnsignedByte();
            int chest = bi.readUnsignedByte();
            int waist = bi.readUnsignedByte();
            int hands = bi.readUnsignedByte();
            int feet = bi.readUnsignedByte();
            int accessory1 = bi.readUnsignedByte();
            int accessory2 = bi.readUnsignedByte();
            int headColor = bi.readUnsignedByte();
            int upperColor = bi.readUnsignedByte();
            int lowerColor = bi.readUnsignedByte();
            int chestColor = bi.readUnsignedByte();
            int waistColor = bi.readUnsignedByte();
            int handsColor = bi.readUnsignedByte();
            int feetColor = bi.readUnsignedByte();
            int accessory1Color = bi.readUnsignedByte();
            int accessory2Color = bi.readUnsignedByte();
            int facePaint = bi.readUnsignedByte();
            String name = Util.readString(bi, 63, StandardCharsets.UTF_8);

            set.setName(name);
            set.setStages(stages);
            set.setFace(face);
            set.setHead(head);
            set.setUpper(upper);
            set.setLower(lower);
            set.setChest(chest);
            set.setWaist(waist);
            set.setHands(hands);
            set.setFeet(feet);
            set.setAccessory1(accessory1);
            set.setAccessory2(accessory2);
            set.setHeadColor(headColor);
            set.setUpperColor(upperColor);
            set.setLowerColor(lowerColor);
            set.setChestColor(chestColor);
            set.setWaistColor(waistColor);
            set.setHandsColor(handsColor);
            set.setFeetColor(feetColor);
            set.setAccessory1Color(accessory1Color);
            set.setAccessory2Color(accessory2Color);
            set.setFacePaint(facePaint);
        }
    }
}
