package mgo.echo.handler.character.dto;

import com.google.gson.JsonObject;

import mgo.echo.util.Util;

/**
 * DTO for gameplay options data.
 * Used for serialization/deserialization of gameplay settings.
 */
public class GameplayOptionsDto {
    // Privacy settings
    public int onlineStatusMode;
    public boolean emailFriendsOnly;
    public boolean receiveNotices;
    public boolean receiveInvites;

    // Normal view settings
    public boolean normalViewVerticalInvert;
    public boolean normalViewHorizontalInvert;
    public int normalViewSpeed;

    // Shoulder view settings
    public boolean shoulderViewVerticalInvert;
    public boolean shoulderViewHorizontalInvert;
    public int shoulderViewSpeed;

    // First person view settings
    public boolean firstViewVerticalInvert;
    public boolean firstViewHorizontalInvert;
    public int firstViewSpeed;
    public boolean firstViewPlayerDirection;
    public boolean firstViewMemory;
    public int viewChangeSpeed;

    // HUD settings
    public boolean radarLockNorth;
    public boolean radarFloorHide;
    public int hudDisplaySize;
    public boolean hudHideNameTags;
    public boolean lockOnEnabled;

    // Weapon switch settings
    public int weaponSwitchMode;
    public int weaponSwitchA;
    public int weaponSwitchB;
    public int weaponSwitchC;
    public int weaponSwitchNow;
    public int weaponSwitchBefore;
    public int itemSwitchMode;

    // Codec settings
    public String codec1Name;
    public int codec1a;
    public int codec1b;
    public int codec1c;
    public int codec1d;

    public String codec2Name;
    public int codec2a;
    public int codec2b;
    public int codec2c;
    public int codec2d;

    public String codec3Name;
    public int codec3a;
    public int codec3b;
    public int codec3c;
    public int codec3d;

    public String codec4Name;
    public int codec4a;
    public int codec4b;
    public int codec4c;
    public int codec4d;

    // Voice chat settings
    public int voiceChatRecognitionLevel;
    public int voiceChatVolume;
    public int headsetVolume;
    public int bgmVolume;

    public static GameplayOptionsDto fromJson(String json) {
        JsonObject data = Util.jsonDecode(json);
        GameplayOptionsDto dto = new GameplayOptionsDto();

        dto.onlineStatusMode = data.get("onlineStatusMode").getAsInt();
        dto.emailFriendsOnly = data.get("emailFriendsOnly").getAsBoolean();
        dto.receiveNotices = data.get("receiveNotices").getAsBoolean();
        dto.receiveInvites = data.get("receiveInvites").getAsBoolean();

        dto.normalViewVerticalInvert = data.get("normalViewVerticalInvert").getAsBoolean();
        dto.normalViewHorizontalInvert = data.get("normalViewHorizontalInvert").getAsBoolean();
        dto.normalViewSpeed = data.get("normalViewSpeed").getAsInt();
        dto.shoulderViewVerticalInvert = data.get("shoulderViewVerticalInvert").getAsBoolean();
        dto.shoulderViewHorizontalInvert = data.get("shoulderViewHorizontalInvert").getAsBoolean();
        dto.shoulderViewSpeed = data.get("shoulderViewSpeed").getAsInt();
        dto.firstViewVerticalInvert = data.get("firstViewVerticalInvert").getAsBoolean();
        dto.firstViewHorizontalInvert = data.get("firstViewHorizontalInvert").getAsBoolean();
        dto.firstViewSpeed = data.get("firstViewSpeed").getAsInt();

        dto.firstViewPlayerDirection = data.get("firstViewPlayerDirection").getAsBoolean();
        dto.viewChangeSpeed = data.get("viewChangeSpeed").getAsInt();
        dto.firstViewMemory = data.get("firstViewMemory").getAsBoolean();
        dto.radarLockNorth = data.get("radarLockNorth").getAsBoolean();
        dto.radarFloorHide = data.get("radarFloorHide").getAsBoolean();
        dto.hudDisplaySize = data.get("hudDisplaySize").getAsInt();
        dto.hudHideNameTags = data.get("hudHideNameTags").getAsBoolean();
        dto.lockOnEnabled = data.get("lockOnEnabled").getAsBoolean();

        dto.weaponSwitchMode = data.get("weaponSwitchMode").getAsInt();
        dto.weaponSwitchA = data.get("weaponSwitchA").getAsInt();
        dto.weaponSwitchB = data.get("weaponSwitchB").getAsInt();
        dto.weaponSwitchC = data.get("weaponSwitchC").getAsInt();
        dto.weaponSwitchNow = data.get("weaponSwitchNow").getAsInt();
        dto.weaponSwitchBefore = data.get("weaponSwitchBefore").getAsInt();
        dto.itemSwitchMode = data.get("itemSwitchMode").getAsInt();

        dto.codec1Name = data.get("codec1Name").getAsString();
        dto.codec1a = data.get("codec1a").getAsInt();
        dto.codec1b = data.get("codec1b").getAsInt();
        dto.codec1c = data.get("codec1c").getAsInt();
        dto.codec1d = data.get("codec1d").getAsInt();

        dto.codec2Name = data.get("codec2Name").getAsString();
        dto.codec2a = data.get("codec2a").getAsInt();
        dto.codec2b = data.get("codec2b").getAsInt();
        dto.codec2c = data.get("codec2c").getAsInt();
        dto.codec2d = data.get("codec2d").getAsInt();

        dto.codec3Name = data.get("codec3Name").getAsString();
        dto.codec3a = data.get("codec3a").getAsInt();
        dto.codec3b = data.get("codec3b").getAsInt();
        dto.codec3c = data.get("codec3c").getAsInt();
        dto.codec3d = data.get("codec3d").getAsInt();

        dto.codec4Name = data.get("codec4Name").getAsString();
        dto.codec4a = data.get("codec4a").getAsInt();
        dto.codec4b = data.get("codec4b").getAsInt();
        dto.codec4c = data.get("codec4c").getAsInt();
        dto.codec4d = data.get("codec4d").getAsInt();

        dto.voiceChatRecognitionLevel = data.get("voiceChatRecognitionLevel").getAsInt();
        dto.voiceChatVolume = data.get("voiceChatVolume").getAsInt();
        dto.headsetVolume = data.get("headsetVolume").getAsInt();
        dto.bgmVolume = data.get("bgmVolume").getAsInt();

        return dto;
    }

    public String toJson() {
        JsonObject data = new JsonObject();

        data.addProperty("onlineStatusMode", onlineStatusMode);
        data.addProperty("emailFriendsOnly", emailFriendsOnly);
        data.addProperty("receiveNotices", receiveNotices);
        data.addProperty("receiveInvites", receiveInvites);

        data.addProperty("normalViewVerticalInvert", normalViewVerticalInvert);
        data.addProperty("normalViewHorizontalInvert", normalViewHorizontalInvert);
        data.addProperty("normalViewSpeed", normalViewSpeed);
        data.addProperty("shoulderViewVerticalInvert", shoulderViewVerticalInvert);
        data.addProperty("shoulderViewHorizontalInvert", shoulderViewHorizontalInvert);
        data.addProperty("shoulderViewSpeed", shoulderViewSpeed);
        data.addProperty("firstViewVerticalInvert", firstViewVerticalInvert);
        data.addProperty("firstViewHorizontalInvert", firstViewHorizontalInvert);
        data.addProperty("firstViewSpeed", firstViewSpeed);

        data.addProperty("firstViewPlayerDirection", firstViewPlayerDirection);
        data.addProperty("viewChangeSpeed", viewChangeSpeed);
        data.addProperty("firstViewMemory", firstViewMemory);
        data.addProperty("radarLockNorth", radarLockNorth);
        data.addProperty("radarFloorHide", radarFloorHide);
        data.addProperty("hudDisplaySize", hudDisplaySize);
        data.addProperty("hudHideNameTags", hudHideNameTags);
        data.addProperty("lockOnEnabled", lockOnEnabled);

        data.addProperty("weaponSwitchMode", weaponSwitchMode);
        data.addProperty("weaponSwitchA", weaponSwitchA);
        data.addProperty("weaponSwitchB", weaponSwitchB);
        data.addProperty("weaponSwitchC", weaponSwitchC);
        data.addProperty("weaponSwitchNow", weaponSwitchNow);
        data.addProperty("weaponSwitchBefore", weaponSwitchBefore);
        data.addProperty("itemSwitchMode", itemSwitchMode);

        data.addProperty("codec1Name", codec1Name);
        data.addProperty("codec1a", codec1a);
        data.addProperty("codec1b", codec1b);
        data.addProperty("codec1c", codec1c);
        data.addProperty("codec1d", codec1d);

        data.addProperty("codec2Name", codec2Name);
        data.addProperty("codec2a", codec2a);
        data.addProperty("codec2b", codec2b);
        data.addProperty("codec2c", codec2c);
        data.addProperty("codec2d", codec2d);

        data.addProperty("codec3Name", codec3Name);
        data.addProperty("codec3a", codec3a);
        data.addProperty("codec3b", codec3b);
        data.addProperty("codec3c", codec3c);
        data.addProperty("codec3d", codec3d);

        data.addProperty("codec4Name", codec4Name);
        data.addProperty("codec4a", codec4a);
        data.addProperty("codec4b", codec4b);
        data.addProperty("codec4c", codec4c);
        data.addProperty("codec4d", codec4d);

        data.addProperty("voiceChatRecognitionLevel", voiceChatRecognitionLevel);
        data.addProperty("voiceChatVolume", voiceChatVolume);
        data.addProperty("headsetVolume", headsetVolume);
        data.addProperty("bgmVolume", bgmVolume);

        return Util.jsonEncode(data);
    }
}
