package mgo.echo.handler.character.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.character.dto.GameplayOptionsDto;
import mgo.echo.protocol.Packet;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet reader/writer for gameplay options and UI settings (0x4120).
 * Buffer size: 0x150 bytes
 */
public final class GameplayOptionsPacket {
    private static final Logger logger = LogManager.getLogger(GameplayOptionsPacket.class);

    private static final int BUFFER_SIZE = 0x150;

    private static final byte[] BYTES_GAMEPLAY_UI_SETTINGS = {
            (byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x11, (byte) 0x10, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    private static final String DEFAULT_JSON = "{\"onlineStatusMode\":0,\"emailFriendsOnly\":false,\"receiveNotices\":true,"
            +
            "\"receiveInvites\":true,\"normalViewVerticalInvert\":false,\"normalViewHorizontalInvert\":false," +
            "\"normalViewSpeed\":5,\"shoulderViewVerticalInvert\":false,\"shoulderViewHorizontalInvert\":false," +
            "\"shoulderViewSpeed\":5,\"firstViewVerticalInvert\":false,\"firstViewHorizontalInvert\":false," +
            "\"firstViewSpeed\":5,\"firstViewPlayerDirection\":true,\"viewChangeSpeed\":5,\"firstViewMemory\":false," +
            "\"radarLockNorth\":false,\"radarFloorHide\":false,\"hudDisplaySize\":0,\"hudHideNameTags\":false," +
            "\"lockOnEnabled\":false,\"weaponSwitchMode\":2,\"weaponSwitchA\":0,\"weaponSwitchB\":1," +
            "\"weaponSwitchC\":2,\"weaponSwitchNow\":0,\"weaponSwitchBefore\":1,\"itemSwitchMode\":2," +
            "\"codec1Name\":\"\",\"codec1a\":1,\"codec1b\":3,\"codec1c\":4,\"codec1d\":2," +
            "\"codec2Name\":\"\",\"codec2a\":10,\"codec2b\":12,\"codec2c\":13,\"codec2d\":11," +
            "\"codec3Name\":\"\",\"codec3a\":14,\"codec3b\":16,\"codec3c\":17,\"codec3d\":15," +
            "\"codec4Name\":\"\",\"codec4a\":5,\"codec4b\":7,\"codec4c\":8,\"codec4d\":6," +
            "\"voiceChatRecognitionLevel\":5,\"voiceChatVolume\":5,\"headsetVolume\":5,\"bgmVolume\":10}";

    private GameplayOptionsPacket() {
    }

    public static void write(ChannelHandlerContext ctx, Character character) {
        ByteBuf bo = null;

        try {
            String json = character.getGameplayOptions();
            if (json == null) {
                json = DEFAULT_JSON;
            }
            GameplayOptionsDto dto = GameplayOptionsDto.fromJson(json);

            int viewChangeSpeed = dto.viewChangeSpeed - 1;

            int privacyA = 1;
            privacyA |= (dto.onlineStatusMode & 0b11) << 4;
            privacyA |= dto.emailFriendsOnly ? 0b01000000 : 0;

            int privacyB = 0;
            privacyB |= dto.receiveNotices ? 0b1 : 0;
            privacyB |= dto.receiveInvites ? 0b10000 : 0;

            int normalView = 0;
            normalView |= dto.normalViewVerticalInvert ? 0b1 : 0;
            normalView |= dto.normalViewHorizontalInvert ? 0b10 : 0;
            int normalViewSpeed = dto.normalViewSpeed - 1;
            normalView |= (normalViewSpeed & 0b1111) << 4;

            int shoulderView = 0;
            shoulderView |= dto.shoulderViewVerticalInvert ? 0b1 : 0;
            shoulderView |= dto.shoulderViewHorizontalInvert ? 0b10 : 0;
            int shoulderViewSpeed = dto.shoulderViewSpeed - 1;
            shoulderView |= (shoulderViewSpeed & 0b1111) << 4;

            int firstView = 0;
            firstView |= dto.firstViewVerticalInvert ? 0b1 : 0;
            firstView |= dto.firstViewHorizontalInvert ? 0b10 : 0;
            int firstViewSpeed = dto.firstViewSpeed - 1;
            firstView |= (firstViewSpeed & 0b1111) << 4;
            firstView |= dto.firstViewPlayerDirection ? 0b100 : 0;

            byte _firstViewMemory = 0;
            _firstViewMemory |= dto.firstViewMemory ? 0b10 : 0;

            int radar = 0;
            radar |= dto.radarLockNorth ? 0b1 : 0;
            radar |= dto.radarFloorHide ? 0b10000 : 0;

            int hudDisplay = 0;
            hudDisplay |= dto.hudDisplaySize & 0b11;
            hudDisplay |= dto.hudHideNameTags ? 0b10000 : 0;

            int lockOnAndBGM = 0;
            lockOnAndBGM |= dto.lockOnEnabled ? 0b1 : 0;
            int bgmVolume = dto.bgmVolume + 1;
            lockOnAndBGM |= (bgmVolume & 0b1111) << 4;

            int _weaponSwitchA = 0;
            _weaponSwitchA |= dto.weaponSwitchA & 0b1111;
            _weaponSwitchA |= (dto.weaponSwitchB & 0b1111) << 4;

            int _weaponSwitchB = 0;
            _weaponSwitchB |= dto.weaponSwitchC & 0b1111;

            int weaponSwitchRecall = 0;
            weaponSwitchRecall |= dto.weaponSwitchBefore & 0b1111;
            weaponSwitchRecall |= (dto.weaponSwitchNow & 0b1111) << 4;

            int switchModes = 0;
            switchModes |= dto.weaponSwitchMode & 0b1111;
            switchModes |= (dto.itemSwitchMode & 0b1111) << 4;

            int voiceChatA = 1;
            voiceChatA |= (dto.voiceChatRecognitionLevel & 0b1111) << 4;

            int voiceChatB = 0;
            voiceChatB |= dto.voiceChatVolume & 0b1111;
            voiceChatB |= (dto.headsetVolume & 0b1111) << 4;

            bo = ctx.alloc().directBuffer(BUFFER_SIZE);

            bo.writeByte(privacyA).writeByte(normalView).writeByte(shoulderView).writeByte(firstView)
                    .writeByte(viewChangeSpeed).writeZero(6).writeByte(switchModes).writeZero(1).writeByte(voiceChatA)
                    .writeByte(voiceChatB).writeByte(_weaponSwitchA).writeByte(_weaponSwitchB)
                    .writeByte(weaponSwitchRecall).writeByte(_firstViewMemory).writeByte(privacyB)
                    .writeByte(lockOnAndBGM).writeByte(radar).writeByte(hudDisplay).writeZero(9).writeByte(dto.codec1a)
                    .writeByte(dto.codec1b).writeByte(dto.codec1c).writeByte(dto.codec1d).writeByte(dto.codec2a)
                    .writeByte(dto.codec2b).writeByte(dto.codec2c).writeByte(dto.codec2d).writeByte(dto.codec3a)
                    .writeByte(dto.codec3b).writeByte(dto.codec3c).writeByte(dto.codec3d).writeByte(dto.codec4a)
                    .writeByte(dto.codec4b).writeByte(dto.codec4c).writeByte(dto.codec4d);
            Util.writeString(dto.codec1Name, 64, bo);
            Util.writeString(dto.codec2Name, 64, bo);
            Util.writeString(dto.codec3Name, 64, bo);
            Util.writeString(dto.codec4Name, 64, bo);
            bo.writeBytes(BYTES_GAMEPLAY_UI_SETTINGS);

            Packets.write(ctx, CharactersCmd.GET_GAMEPLAY_OPTIONS_UI_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing gameplay options packet.", e);
            Util.releaseBuffer(bo);
            Packets.write(ctx, CharactersCmd.GET_GAMEPLAY_OPTIONS_UI_RESPONSE, Error.GENERAL);
        }
    }

    public static GameplayOptionsDto read(Packet in) {
        ByteBuf bi = in.getPayload();

        byte privacyA = bi.readByte();
        byte normalView = bi.readByte();
        byte shoulderView = bi.readByte();
        byte firstView = bi.readByte();
        byte viewChangeSpeed = bi.readByte();
        bi.skipBytes(6);
        byte switchModes = bi.readByte();
        bi.skipBytes(1);
        byte voiceChatA = bi.readByte();
        byte voiceChatB = bi.readByte();
        byte _weaponSwitchA = bi.readByte();
        byte _weaponSwitchB = bi.readByte();
        byte weaponSwitchRecall = bi.readByte();
        byte _firstViewMemory = bi.readByte();
        byte privacyB = bi.readByte();
        byte lockOnAndBGM = bi.readByte();
        byte radar = bi.readByte();
        byte hudDisplay = bi.readByte();
        bi.skipBytes(9);
        byte codec1a = bi.readByte();
        byte codec1b = bi.readByte();
        byte codec1c = bi.readByte();
        byte codec1d = bi.readByte();
        byte codec2a = bi.readByte();
        byte codec2b = bi.readByte();
        byte codec2c = bi.readByte();
        byte codec2d = bi.readByte();
        byte codec3a = bi.readByte();
        byte codec3b = bi.readByte();
        byte codec3c = bi.readByte();
        byte codec3d = bi.readByte();
        byte codec4a = bi.readByte();
        byte codec4b = bi.readByte();
        byte codec4c = bi.readByte();
        byte codec4d = bi.readByte();
        String codec1Name = Util.readString(bi, 64);
        String codec2Name = Util.readString(bi, 64);
        String codec3Name = Util.readString(bi, 64);
        String codec4Name = Util.readString(bi, 64);

        GameplayOptionsDto dto = new GameplayOptionsDto();

        dto.onlineStatusMode = (privacyA >> 4) & 0b11;
        dto.emailFriendsOnly = (privacyA & 0b01000000) == 0b01000000;
        dto.receiveNotices = (privacyB & 0b1) == 0b1;
        dto.receiveInvites = (privacyB & 0b10000) == 0b10000;

        dto.normalViewVerticalInvert = (normalView & 0b1) == 0b1;
        dto.normalViewHorizontalInvert = (normalView & 0b10) == 0b10;
        dto.normalViewSpeed = ((normalView >> 4) & 0b1111) + 1;

        dto.shoulderViewVerticalInvert = (shoulderView & 0b1) == 0b1;
        dto.shoulderViewHorizontalInvert = (shoulderView & 0b10) == 0b10;
        dto.shoulderViewSpeed = ((shoulderView >> 4) & 0b1111) + 1;

        dto.firstViewVerticalInvert = (firstView & 0b1) == 0b1;
        dto.firstViewHorizontalInvert = (firstView & 0b10) == 0b10;
        dto.firstViewSpeed = ((firstView >> 4) & 0b1111) + 1;
        dto.firstViewPlayerDirection = (firstView & 0b100) == 0b100;
        dto.firstViewMemory = (_firstViewMemory & 0b10) == 0b10;

        dto.viewChangeSpeed = viewChangeSpeed + 1;

        dto.radarLockNorth = (radar & 0b1) == 0b1;
        dto.radarFloorHide = (radar & 0b10000) == 0b10000;

        dto.hudDisplaySize = hudDisplay & 0b11;
        dto.hudHideNameTags = (hudDisplay & 0b10000) == 0b10000;

        dto.lockOnEnabled = (lockOnAndBGM & 0b1) == 0b1;
        dto.bgmVolume = ((lockOnAndBGM >> 4) & 0b1111) - 1;

        dto.weaponSwitchA = _weaponSwitchA & 0b1111;
        dto.weaponSwitchB = (_weaponSwitchA >> 4) & 0b1111;
        dto.weaponSwitchC = _weaponSwitchB & 0b1111;
        dto.weaponSwitchBefore = weaponSwitchRecall & 0b1111;
        dto.weaponSwitchNow = (weaponSwitchRecall >> 4) & 0b1111;
        dto.weaponSwitchMode = switchModes & 0b1111;
        dto.itemSwitchMode = (switchModes >> 4) & 0b1111;

        dto.voiceChatRecognitionLevel = (voiceChatA >> 4) & 0b1111;
        dto.voiceChatVolume = voiceChatB & 0b1111;
        dto.headsetVolume = (voiceChatB >> 4) & 0b1111;

        dto.codec1Name = codec1Name;
        dto.codec1a = codec1a;
        dto.codec1b = codec1b;
        dto.codec1c = codec1c;
        dto.codec1d = codec1d;

        dto.codec2Name = codec2Name;
        dto.codec2a = codec2a;
        dto.codec2b = codec2b;
        dto.codec2c = codec2c;
        dto.codec2d = codec2d;

        dto.codec3Name = codec3Name;
        dto.codec3a = codec3a;
        dto.codec3b = codec3b;
        dto.codec3c = codec3c;
        dto.codec3d = codec3d;

        dto.codec4Name = codec4Name;
        dto.codec4a = codec4a;
        dto.codec4b = codec4b;
        dto.codec4c = codec4c;
        dto.codec4d = codec4d;

        return dto;
    }

    public static void saveToDb(Character character, GameplayOptionsDto dto) {
        String json = dto.toJson();
        DbManager.txVoid(session -> {
            character.setGameplayOptions(json);
            session.update(character);
        });
    }
}
