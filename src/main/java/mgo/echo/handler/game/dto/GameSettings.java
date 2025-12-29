package mgo.echo.handler.game.dto;

import com.google.gson.JsonObject;

/**
 * Game settings parsed from JSON common field.
 */
public class GameSettings {
    public boolean dedicated;
    public int briefingTime;
    public boolean nonStat;
    public boolean friendlyFire;
    public boolean autoAim;
    public boolean uniquesEnabled;
    public boolean uniquesRandom;
    public int uniqueRed;
    public int uniqueBlue;
    public boolean enemyNametags;
    public boolean silentMode;
    public boolean autoAssign;
    public boolean teamsSwitch;
    public boolean ghosts;
    public boolean levelLimitEnabled;
    public int levelLimitBase;
    public int levelLimitTolerance;
    public boolean voiceChat;
    public int teamKillKick;
    public int idleKick;

    public static GameSettings parse(JsonObject common) {
        GameSettings s = new GameSettings();

        s.dedicated = common.get("dedicated").getAsBoolean();
        s.briefingTime = common.get("briefingTime").getAsInt();
        s.nonStat = common.get("nonStat").getAsBoolean();
        s.friendlyFire = common.get("friendlyFire").getAsBoolean();
        s.autoAim = common.get("autoAim").getAsBoolean();

        JsonObject uniques = common.get("uniques").getAsJsonObject();
        s.uniquesEnabled = uniques.get("enabled").getAsBoolean();
        s.uniquesRandom = uniques.get("random").getAsBoolean();
        s.uniqueRed = uniques.get("red").getAsInt();
        s.uniqueBlue = uniques.get("blue").getAsInt();

        s.enemyNametags = common.get("enemyNametags").getAsBoolean();
        s.silentMode = common.get("silentMode").getAsBoolean();
        s.autoAssign = common.get("autoAssign").getAsBoolean();
        s.teamsSwitch = common.get("teamsSwitch").getAsBoolean();
        s.ghosts = common.get("ghosts").getAsBoolean();

        JsonObject levelLimit = common.get("levelLimit").getAsJsonObject();
        s.levelLimitEnabled = levelLimit.get("enabled").getAsBoolean();
        s.levelLimitBase = levelLimit.get("base").getAsInt();
        s.levelLimitTolerance = levelLimit.get("tolerance").getAsInt();

        s.voiceChat = common.get("voiceChat").getAsBoolean();
        s.teamKillKick = common.get("teamKillKick").getAsInt();
        s.idleKick = common.get("idleKick").getAsInt();

        return s;
    }

    public int buildCommonA() {
        int commonA = 0b100;
        commonA |= idleKick > 0 ? 0b1 : 0;
        commonA |= friendlyFire ? 0b1000 : 0;
        commonA |= ghosts ? 0b10000 : 0;
        commonA |= autoAim ? 0b100000 : 0;
        commonA |= uniquesEnabled ? 0b10000000 : 0;
        return commonA;
    }

    public int buildCommonB() {
        int commonB = 0;
        commonB |= teamsSwitch ? 0b1 : 0;
        commonB |= autoAssign ? 0b10 : 0;
        commonB |= silentMode ? 0b100 : 0;
        commonB |= enemyNametags ? 0b1000 : 0;
        commonB |= levelLimitEnabled ? 0b10000 : 0;
        commonB |= voiceChat ? 0b1000000 : 0;
        commonB |= teamKillKick > 0 ? 0b10000000 : 0;
        return commonB;
    }

    public int buildHostOptions(boolean hasPassword) {
        int hostOptions = 0;
        hostOptions |= hasPassword ? 0b1 : 0;
        hostOptions |= dedicated ? 0b10 : 0;
        return hostOptions;
    }
}
