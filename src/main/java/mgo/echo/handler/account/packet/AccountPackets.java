package mgo.echo.handler.account.packet;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterAppearance;
import mgo.echo.data.entity.User;
import mgo.echo.protocol.command.UsersCmd;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Account/Character list packet writers.
 */
public class AccountPackets {
    private static final byte[] CHARACTER_LIST_FOOTER = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x03,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    private AccountPackets() {
    }

    // ========================================================================
    // Character List
    // ========================================================================

    public static void writeCharacterList(ChannelHandlerContext ctx, User user, List<Character> characters) {
        ByteBuf bo = ctx.alloc().directBuffer(0x1d7);
        bo.writeInt(0).writeByte(user.getSlots()).writeByte(characters.size()).writeZero(1);

        for (int i = 0; i < characters.size(); i++) {
            writeCharacterEntry(bo, characters.get(i), user.getMainCharacterId(), i);
        }

        bo.writeZero(0x1b4 - bo.writerIndex());
        bo.writeBytes(CHARACTER_LIST_FOOTER);
        Packets.write(ctx, UsersCmd.GET_CHARACTER_LIST_RESPONSE, bo);
    }

    private static void writeCharacterEntry(ByteBuf bo, Character character, Integer mainCharacterId, int index) {
        CharacterAppearance appearance = Util.getFirstOrNull(character.getAppearance());

        boolean isMain = mainCharacterId != null && character.getId().equals(mainCharacterId);
        String name = isMain ? "*" + character.getName() : character.getName();

        if (index == 0) {
            Util.writeString(name, 16, bo);
            bo.writeZero(1);
        } else {
            bo.writeInt(index);
        }

        bo.writeInt(character.getId());
        Util.writeString(name, 16, bo);

        writeAppearance(bo, appearance);
    }

    private static void writeAppearance(ByteBuf bo, CharacterAppearance appearance) {
        bo.writeByte(appearance.getGender()).writeByte(appearance.getFace())
                .writeByte(appearance.getUpper()).writeByte(appearance.getLower())
                .writeByte(appearance.getFacePaint()).writeByte(appearance.getUpperColor())
                .writeByte(appearance.getLowerColor()).writeByte(appearance.getVoice())
                .writeByte(appearance.getPitch()).writeZero(4)
                .writeByte(appearance.getHead()).writeByte(appearance.getChest())
                .writeByte(appearance.getHands()).writeByte(appearance.getWaist())
                .writeByte(appearance.getFeet()).writeByte(appearance.getAccessory1())
                .writeByte(appearance.getAccessory2()).writeByte(appearance.getHeadColor())
                .writeByte(appearance.getChestColor()).writeByte(appearance.getHandsColor())
                .writeByte(appearance.getWaistColor()).writeByte(appearance.getFeetColor())
                .writeByte(appearance.getAccessory1Color()).writeByte(appearance.getAccessory2Color())
                .writeZero(1);
    }

    // ========================================================================
    // Character Create Response
    // ========================================================================

    public static void writeCreateResponse(ChannelHandlerContext ctx, Character character) {
        ByteBuf bo = ctx.alloc().directBuffer(8);
        bo.writeInt(0).writeInt(character.getId());
        Packets.write(ctx, UsersCmd.CREATE_CHARACTER_RESPONSE, bo);
    }

    // ========================================================================
    // Appearance Reading
    // ========================================================================

    public static CharacterAppearance readAppearance(ByteBuf bi) {
        int gender = bi.readByte();
        int face = bi.readByte();
        int upper = bi.readByte();
        int lower = 0;
        bi.skipBytes(1);
        int facePaint = bi.readByte();
        int upperColor = bi.readByte();
        int lowerColor = bi.readByte();
        int voice = bi.readByte();
        int pitch = bi.readByte();
        bi.skipBytes(4);
        int head = bi.readByte();
        int chest = bi.readByte();
        int hands = bi.readByte();
        int waist = bi.readByte();
        int feet = bi.readByte();
        int accessory1 = bi.readByte();
        int accessory2 = bi.readByte();
        int headColor = bi.readByte();
        int chestColor = bi.readByte();
        int handsColor = 0;
        bi.skipBytes(1);
        int waistColor = bi.readByte();
        int feetColor = bi.readByte();
        int accessory1Color = bi.readByte();
        int accessory2Color = bi.readByte();

        CharacterAppearance appearance = new CharacterAppearance();
        appearance.setGender(gender);
        appearance.setFace(face);
        appearance.setVoice(voice);
        appearance.setPitch(pitch);
        appearance.setHead(head);
        appearance.setHeadColor(headColor);
        appearance.setUpper(upper);
        appearance.setUpperColor(upperColor);
        appearance.setLower(lower);
        appearance.setLowerColor(lowerColor);
        appearance.setChest(chest);
        appearance.setChestColor(chestColor);
        appearance.setWaist(waist);
        appearance.setWaistColor(waistColor);
        appearance.setHands(hands);
        appearance.setHandsColor(handsColor);
        appearance.setFeet(feet);
        appearance.setFeetColor(feetColor);
        appearance.setAccessory1(accessory1);
        appearance.setAccessory1Color(accessory1Color);
        appearance.setAccessory2(accessory2);
        appearance.setAccessory2Color(accessory2Color);
        appearance.setFacePaint(facePaint);

        return appearance;
    }
}
