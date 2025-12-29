package mgo.echo.handler.game.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mgo.echo.util.Util;

/**
 * Host settings data.
 * This holds the complete game host configuration.
 */
public class HostSettingsDto {
    // Basic info
    public String name;
    public String password;
    public int stance;
    public String comment;
    public JsonArray games = new JsonArray();

    // Common settings
    public CommonSettings common = new CommonSettings();
    public RuleSettings ruleSettings = new RuleSettings();

    public static class CommonSettings {
        public boolean dedicated;
        public int maxPlayers;
        public int briefingTime;
        public boolean nonStat;
        public boolean friendlyFire;
        public boolean autoAim;

        // Uniques
        public boolean uniquesEnabled;
        public boolean uniquesRandom;
        public int uniqueRed;
        public int uniqueBlue;

        public boolean enemyNametags;
        public boolean silentMode;
        public boolean autoAssign;
        public boolean teamsSwitch;
        public boolean ghosts;

        // Level limit
        public boolean levelLimitEnabled;
        public int levelLimitBase;
        public int levelLimitTolerance;

        public boolean voiceChat;
        public int teamKillKick;
        public int idleKick;

        // Weapon restrictions
        public WeaponRestrictions weaponRestrictions = new WeaponRestrictions();
    }

    public static class WeaponRestrictions {
        public boolean enabled;

        // Primary weapons
        public boolean vz, p90, mp5, patriot, ak, m4, mk17, xm8, g3a3;
        public boolean svd, mosin, m14, vss, dsr, m870, saiga, m60, shield, rpg, knife;

        // Secondary weapons
        public boolean gsr, mk2, operator, g18, mk23, de;

        // Support items
        public boolean grenade, wp, stun, chaff;
        public boolean smoke, smoke_r, smoke_g, smoke_y;
        public boolean eloc, claymore, sgmine, c4, sgsatchel, magazine;

        // Custom parts
        public boolean suppressor, gp30, xm320, masterkey;
        public boolean scope, sight, laser, lighthg, lightlg, grip;

        // Items
        public boolean envg, drum;
    }

    public static class RuleSettings {
        // DM
        public int dmTime, dmRounds, dmTickets;

        // TDM
        public int tdmTime, tdmRounds, tdmTickets;

        // RES
        public int resTime, resRounds;

        // CAP
        public int capTime, capRounds;
        public boolean capExtraTime;

        // SNE
        public int sneTime, sneRounds, sneSnake;

        // BASE
        public int baseTime, baseRounds;

        // BOMB
        public int bombTime, bombRounds;

        // TSNE
        public int tsneTime, tsneRounds;

        // SDM
        public int sdmTime, sdmRounds;

        // INT
        public int intTime;

        // SCAP
        public int scapTime, scapRounds;
        public boolean scapExtraTime;

        // RACE
        public int raceTime, raceRounds;
        public boolean raceExtraTime;
    }

    public static HostSettingsDto fromJson(String jsonStr) {
        JsonObject settings = Util.jsonDecode(jsonStr);
        HostSettingsDto dto = new HostSettingsDto();

        dto.name = settings.get("name").getAsString();
        dto.password = settings.get("password") != null && !settings.get("password").isJsonNull()
                ? settings.get("password").getAsString()
                : null;
        dto.stance = settings.get("stance").getAsInt();
        dto.comment = settings.get("comment").getAsString();
        dto.games = settings.get("games").getAsJsonArray();

        // Common settings
        JsonObject common = settings.get("common").getAsJsonObject();
        dto.common.dedicated = common.get("dedicated").getAsBoolean();
        dto.common.maxPlayers = common.get("maxPlayers").getAsInt();
        dto.common.briefingTime = common.get("briefingTime").getAsInt();
        dto.common.nonStat = common.get("nonStat").getAsBoolean();
        dto.common.friendlyFire = common.get("friendlyFire").getAsBoolean();
        dto.common.autoAim = common.get("autoAim").getAsBoolean();

        // Uniques
        JsonObject uniques = common.get("uniques").getAsJsonObject();
        dto.common.uniquesEnabled = uniques.get("enabled").getAsBoolean();
        dto.common.uniquesRandom = uniques.get("random").getAsBoolean();
        dto.common.uniqueRed = uniques.get("red").getAsInt();
        dto.common.uniqueBlue = uniques.get("blue").getAsInt();

        dto.common.enemyNametags = common.get("enemyNametags").getAsBoolean();
        dto.common.silentMode = common.get("silentMode").getAsBoolean();
        dto.common.autoAssign = common.get("autoAssign").getAsBoolean();
        dto.common.teamsSwitch = common.get("teamsSwitch").getAsBoolean();
        dto.common.ghosts = common.get("ghosts").getAsBoolean();

        // Level limit
        JsonObject levelLimit = common.get("levelLimit").getAsJsonObject();
        dto.common.levelLimitEnabled = levelLimit.get("enabled").getAsBoolean();
        dto.common.levelLimitBase = levelLimit.get("base").getAsInt();
        dto.common.levelLimitTolerance = levelLimit.get("tolerance").getAsInt();

        dto.common.voiceChat = common.get("voiceChat").getAsBoolean();
        dto.common.teamKillKick = common.get("teamKillKick").getAsInt();
        dto.common.idleKick = common.get("idleKick").getAsInt();

        // Weapon restrictions
        JsonObject weaponRestrictions = common.get("weaponRestrictions").getAsJsonObject();
        dto.common.weaponRestrictions.enabled = weaponRestrictions.get("enabled").getAsBoolean();

        JsonObject wrPrimary = weaponRestrictions.get("primary").getAsJsonObject();
        dto.common.weaponRestrictions.vz = wrPrimary.get("vz").getAsBoolean();
        dto.common.weaponRestrictions.p90 = wrPrimary.get("p90").getAsBoolean();
        dto.common.weaponRestrictions.mp5 = wrPrimary.get("mp5").getAsBoolean();
        dto.common.weaponRestrictions.patriot = wrPrimary.get("patriot").getAsBoolean();
        dto.common.weaponRestrictions.ak = wrPrimary.get("ak").getAsBoolean();
        dto.common.weaponRestrictions.m4 = wrPrimary.get("m4").getAsBoolean();
        dto.common.weaponRestrictions.mk17 = wrPrimary.get("mk17").getAsBoolean();
        dto.common.weaponRestrictions.xm8 = wrPrimary.get("xm8").getAsBoolean();
        dto.common.weaponRestrictions.g3a3 = wrPrimary.get("g3a3").getAsBoolean();
        dto.common.weaponRestrictions.svd = wrPrimary.get("svd").getAsBoolean();
        dto.common.weaponRestrictions.mosin = wrPrimary.get("mosin").getAsBoolean();
        dto.common.weaponRestrictions.m14 = wrPrimary.get("m14").getAsBoolean();
        dto.common.weaponRestrictions.vss = wrPrimary.get("vss").getAsBoolean();
        dto.common.weaponRestrictions.dsr = wrPrimary.get("dsr").getAsBoolean();
        dto.common.weaponRestrictions.m870 = wrPrimary.get("m870").getAsBoolean();
        dto.common.weaponRestrictions.saiga = wrPrimary.get("saiga").getAsBoolean();
        dto.common.weaponRestrictions.m60 = wrPrimary.get("m60").getAsBoolean();
        dto.common.weaponRestrictions.shield = wrPrimary.get("shield").getAsBoolean();
        dto.common.weaponRestrictions.rpg = wrPrimary.get("rpg").getAsBoolean();
        dto.common.weaponRestrictions.knife = wrPrimary.get("knife").getAsBoolean();

        JsonObject wrSecondary = weaponRestrictions.get("secondary").getAsJsonObject();
        dto.common.weaponRestrictions.gsr = wrSecondary.get("gsr").getAsBoolean();
        dto.common.weaponRestrictions.mk2 = wrSecondary.get("mk2").getAsBoolean();
        dto.common.weaponRestrictions.operator = wrSecondary.get("operator").getAsBoolean();
        dto.common.weaponRestrictions.g18 = wrSecondary.get("g18").getAsBoolean();
        dto.common.weaponRestrictions.mk23 = wrSecondary.get("mk23").getAsBoolean();
        dto.common.weaponRestrictions.de = wrSecondary.get("de").getAsBoolean();

        JsonObject wrSupport = weaponRestrictions.get("support").getAsJsonObject();
        dto.common.weaponRestrictions.grenade = wrSupport.get("grenade").getAsBoolean();
        dto.common.weaponRestrictions.wp = wrSupport.get("wp").getAsBoolean();
        dto.common.weaponRestrictions.stun = wrSupport.get("stun").getAsBoolean();
        dto.common.weaponRestrictions.chaff = wrSupport.get("chaff").getAsBoolean();
        dto.common.weaponRestrictions.smoke = wrSupport.get("smoke").getAsBoolean();
        dto.common.weaponRestrictions.smoke_r = wrSupport.get("smoke_r").getAsBoolean();
        dto.common.weaponRestrictions.smoke_g = wrSupport.get("smoke_g").getAsBoolean();
        dto.common.weaponRestrictions.smoke_y = wrSupport.get("smoke_y").getAsBoolean();
        dto.common.weaponRestrictions.eloc = wrSupport.get("eloc").getAsBoolean();
        dto.common.weaponRestrictions.claymore = wrSupport.get("claymore").getAsBoolean();
        dto.common.weaponRestrictions.sgmine = wrSupport.get("sgmine").getAsBoolean();
        dto.common.weaponRestrictions.c4 = wrSupport.get("c4").getAsBoolean();
        dto.common.weaponRestrictions.sgsatchel = wrSupport.get("sgsatchel").getAsBoolean();
        dto.common.weaponRestrictions.magazine = wrSupport.get("magazine").getAsBoolean();

        JsonObject wrCustom = weaponRestrictions.get("custom").getAsJsonObject();
        dto.common.weaponRestrictions.suppressor = wrCustom.get("suppressor").getAsBoolean();
        dto.common.weaponRestrictions.gp30 = wrCustom.get("gp30").getAsBoolean();
        dto.common.weaponRestrictions.xm320 = wrCustom.get("xm320").getAsBoolean();
        dto.common.weaponRestrictions.masterkey = wrCustom.get("masterkey").getAsBoolean();
        dto.common.weaponRestrictions.scope = wrCustom.get("scope").getAsBoolean();
        dto.common.weaponRestrictions.sight = wrCustom.get("sight").getAsBoolean();
        dto.common.weaponRestrictions.laser = wrCustom.get("laser").getAsBoolean();
        dto.common.weaponRestrictions.lighthg = wrCustom.get("lighthg").getAsBoolean();
        dto.common.weaponRestrictions.lightlg = wrCustom.get("lightlg").getAsBoolean();
        dto.common.weaponRestrictions.grip = wrCustom.get("grip").getAsBoolean();

        JsonObject wrItems = weaponRestrictions.get("items").getAsJsonObject();
        dto.common.weaponRestrictions.envg = wrItems.get("envg").getAsBoolean();
        dto.common.weaponRestrictions.drum = wrItems.get("drum").getAsBoolean();

        // Rule settings
        JsonObject ruleSettings = settings.get("ruleSettings").getAsJsonObject();

        JsonObject dm = ruleSettings.get("dm").getAsJsonObject();
        dto.ruleSettings.dmTime = dm.get("time").getAsInt();
        dto.ruleSettings.dmRounds = dm.get("rounds").getAsInt();
        dto.ruleSettings.dmTickets = dm.get("tickets").getAsInt();

        JsonObject tdm = ruleSettings.get("tdm").getAsJsonObject();
        dto.ruleSettings.tdmTime = tdm.get("time").getAsInt();
        dto.ruleSettings.tdmRounds = tdm.get("rounds").getAsInt();
        dto.ruleSettings.tdmTickets = tdm.get("tickets").getAsInt();

        JsonObject res = ruleSettings.get("res").getAsJsonObject();
        dto.ruleSettings.resTime = res.get("time").getAsInt();
        dto.ruleSettings.resRounds = res.get("rounds").getAsInt();

        JsonObject cap = ruleSettings.get("cap").getAsJsonObject();
        dto.ruleSettings.capTime = cap.get("time").getAsInt();
        dto.ruleSettings.capRounds = cap.get("rounds").getAsInt();
        dto.ruleSettings.capExtraTime = cap.get("extraTime").getAsBoolean();

        JsonObject sne = ruleSettings.get("sne").getAsJsonObject();
        dto.ruleSettings.sneTime = sne.get("time").getAsInt();
        dto.ruleSettings.sneRounds = sne.get("rounds").getAsInt();
        dto.ruleSettings.sneSnake = sne.get("snake").getAsInt();

        JsonObject base = ruleSettings.get("base").getAsJsonObject();
        dto.ruleSettings.baseTime = base.get("time").getAsInt();
        dto.ruleSettings.baseRounds = base.get("rounds").getAsInt();

        JsonObject bomb = ruleSettings.get("bomb").getAsJsonObject();
        dto.ruleSettings.bombTime = bomb.get("time").getAsInt();
        dto.ruleSettings.bombRounds = bomb.get("rounds").getAsInt();

        JsonObject tsne = ruleSettings.get("tsne").getAsJsonObject();
        dto.ruleSettings.tsneTime = tsne.get("time").getAsInt();
        dto.ruleSettings.tsneRounds = tsne.get("rounds").getAsInt();

        JsonObject sdm = ruleSettings.get("sdm").getAsJsonObject();
        dto.ruleSettings.sdmTime = sdm.get("time").getAsInt();
        dto.ruleSettings.sdmRounds = sdm.get("rounds").getAsInt();

        JsonObject intr = ruleSettings.get("int").getAsJsonObject();
        dto.ruleSettings.intTime = intr.get("time").getAsInt();

        JsonObject scap = ruleSettings.get("scap").getAsJsonObject();
        dto.ruleSettings.scapTime = scap.get("time").getAsInt();
        dto.ruleSettings.scapRounds = scap.get("rounds").getAsInt();
        dto.ruleSettings.scapExtraTime = scap.get("extraTime").getAsBoolean();

        JsonObject race = ruleSettings.get("race").getAsJsonObject();
        dto.ruleSettings.raceTime = race.get("time").getAsInt();
        dto.ruleSettings.raceRounds = race.get("rounds").getAsInt();
        dto.ruleSettings.raceExtraTime = race.get("extraTime").getAsBoolean();

        return dto;
    }

    public String toJson() {
        JsonObject settings = new JsonObject();
        settings.addProperty("name", name);
        settings.addProperty("password", password);
        settings.addProperty("stance", stance);
        settings.addProperty("comment", comment);
        settings.add("games", games);

        // Common settings
        JsonObject common = new JsonObject();
        settings.add("common", common);
        common.addProperty("dedicated", this.common.dedicated);
        common.addProperty("maxPlayers", this.common.maxPlayers);
        common.addProperty("briefingTime", this.common.briefingTime);
        common.addProperty("nonStat", this.common.nonStat);
        common.addProperty("friendlyFire", this.common.friendlyFire);
        common.addProperty("autoAim", this.common.autoAim);

        // Uniques
        JsonObject uniques = new JsonObject();
        common.add("uniques", uniques);
        uniques.addProperty("enabled", this.common.uniquesEnabled);
        uniques.addProperty("random", this.common.uniquesRandom);
        uniques.addProperty("red", this.common.uniqueRed);
        uniques.addProperty("blue", this.common.uniqueBlue);

        common.addProperty("enemyNametags", this.common.enemyNametags);
        common.addProperty("silentMode", this.common.silentMode);
        common.addProperty("autoAssign", this.common.autoAssign);
        common.addProperty("teamsSwitch", this.common.teamsSwitch);
        common.addProperty("ghosts", this.common.ghosts);

        // Level limit
        JsonObject levelLimit = new JsonObject();
        common.add("levelLimit", levelLimit);
        levelLimit.addProperty("enabled", this.common.levelLimitEnabled);
        levelLimit.addProperty("base", this.common.levelLimitBase);
        levelLimit.addProperty("tolerance", this.common.levelLimitTolerance);

        common.addProperty("voiceChat", this.common.voiceChat);
        common.addProperty("teamKillKick", this.common.teamKillKick);
        common.addProperty("idleKick", this.common.idleKick);

        // Weapon restrictions
        JsonObject weaponRestrictions = new JsonObject();
        common.add("weaponRestrictions", weaponRestrictions);
        weaponRestrictions.addProperty("enabled", this.common.weaponRestrictions.enabled);

        JsonObject wrPrimary = new JsonObject();
        weaponRestrictions.add("primary", wrPrimary);
        wrPrimary.addProperty("vz", this.common.weaponRestrictions.vz);
        wrPrimary.addProperty("p90", this.common.weaponRestrictions.p90);
        wrPrimary.addProperty("mp5", this.common.weaponRestrictions.mp5);
        wrPrimary.addProperty("patriot", this.common.weaponRestrictions.patriot);
        wrPrimary.addProperty("ak", this.common.weaponRestrictions.ak);
        wrPrimary.addProperty("m4", this.common.weaponRestrictions.m4);
        wrPrimary.addProperty("mk17", this.common.weaponRestrictions.mk17);
        wrPrimary.addProperty("xm8", this.common.weaponRestrictions.xm8);
        wrPrimary.addProperty("g3a3", this.common.weaponRestrictions.g3a3);
        wrPrimary.addProperty("svd", this.common.weaponRestrictions.svd);
        wrPrimary.addProperty("mosin", this.common.weaponRestrictions.mosin);
        wrPrimary.addProperty("m14", this.common.weaponRestrictions.m14);
        wrPrimary.addProperty("vss", this.common.weaponRestrictions.vss);
        wrPrimary.addProperty("dsr", this.common.weaponRestrictions.dsr);
        wrPrimary.addProperty("m870", this.common.weaponRestrictions.m870);
        wrPrimary.addProperty("saiga", this.common.weaponRestrictions.saiga);
        wrPrimary.addProperty("m60", this.common.weaponRestrictions.m60);
        wrPrimary.addProperty("shield", this.common.weaponRestrictions.shield);
        wrPrimary.addProperty("rpg", this.common.weaponRestrictions.rpg);
        wrPrimary.addProperty("knife", this.common.weaponRestrictions.knife);

        JsonObject wrSecondary = new JsonObject();
        weaponRestrictions.add("secondary", wrSecondary);
        wrSecondary.addProperty("gsr", this.common.weaponRestrictions.gsr);
        wrSecondary.addProperty("mk2", this.common.weaponRestrictions.mk2);
        wrSecondary.addProperty("operator", this.common.weaponRestrictions.operator);
        wrSecondary.addProperty("g18", this.common.weaponRestrictions.g18);
        wrSecondary.addProperty("mk23", this.common.weaponRestrictions.mk23);
        wrSecondary.addProperty("de", this.common.weaponRestrictions.de);

        JsonObject wrSupport = new JsonObject();
        weaponRestrictions.add("support", wrSupport);
        wrSupport.addProperty("grenade", this.common.weaponRestrictions.grenade);
        wrSupport.addProperty("wp", this.common.weaponRestrictions.wp);
        wrSupport.addProperty("stun", this.common.weaponRestrictions.stun);
        wrSupport.addProperty("chaff", this.common.weaponRestrictions.chaff);
        wrSupport.addProperty("smoke", this.common.weaponRestrictions.smoke);
        wrSupport.addProperty("smoke_r", this.common.weaponRestrictions.smoke_r);
        wrSupport.addProperty("smoke_g", this.common.weaponRestrictions.smoke_g);
        wrSupport.addProperty("smoke_y", this.common.weaponRestrictions.smoke_y);
        wrSupport.addProperty("eloc", this.common.weaponRestrictions.eloc);
        wrSupport.addProperty("claymore", this.common.weaponRestrictions.claymore);
        wrSupport.addProperty("sgmine", this.common.weaponRestrictions.sgmine);
        wrSupport.addProperty("c4", this.common.weaponRestrictions.c4);
        wrSupport.addProperty("sgsatchel", this.common.weaponRestrictions.sgsatchel);
        wrSupport.addProperty("magazine", this.common.weaponRestrictions.magazine);

        JsonObject wrCustom = new JsonObject();
        weaponRestrictions.add("custom", wrCustom);
        wrCustom.addProperty("suppressor", this.common.weaponRestrictions.suppressor);
        wrCustom.addProperty("gp30", this.common.weaponRestrictions.gp30);
        wrCustom.addProperty("xm320", this.common.weaponRestrictions.xm320);
        wrCustom.addProperty("masterkey", this.common.weaponRestrictions.masterkey);
        wrCustom.addProperty("scope", this.common.weaponRestrictions.scope);
        wrCustom.addProperty("sight", this.common.weaponRestrictions.sight);
        wrCustom.addProperty("laser", this.common.weaponRestrictions.laser);
        wrCustom.addProperty("lighthg", this.common.weaponRestrictions.lighthg);
        wrCustom.addProperty("lightlg", this.common.weaponRestrictions.lightlg);
        wrCustom.addProperty("grip", this.common.weaponRestrictions.grip);

        JsonObject wrItems = new JsonObject();
        weaponRestrictions.add("items", wrItems);
        wrItems.addProperty("envg", this.common.weaponRestrictions.envg);
        wrItems.addProperty("drum", this.common.weaponRestrictions.drum);

        // Rule settings
        JsonObject ruleSettingsJson = new JsonObject();
        settings.add("ruleSettings", ruleSettingsJson);

        JsonObject dm = new JsonObject();
        ruleSettingsJson.add("dm", dm);
        dm.addProperty("time", this.ruleSettings.dmTime);
        dm.addProperty("rounds", this.ruleSettings.dmRounds);
        dm.addProperty("tickets", this.ruleSettings.dmTickets);

        JsonObject tdm = new JsonObject();
        ruleSettingsJson.add("tdm", tdm);
        tdm.addProperty("time", this.ruleSettings.tdmTime);
        tdm.addProperty("rounds", this.ruleSettings.tdmRounds);
        tdm.addProperty("tickets", this.ruleSettings.tdmTickets);

        JsonObject res = new JsonObject();
        ruleSettingsJson.add("res", res);
        res.addProperty("time", this.ruleSettings.resTime);
        res.addProperty("rounds", this.ruleSettings.resRounds);

        JsonObject cap = new JsonObject();
        ruleSettingsJson.add("cap", cap);
        cap.addProperty("time", this.ruleSettings.capTime);
        cap.addProperty("rounds", this.ruleSettings.capRounds);
        cap.addProperty("extraTime", this.ruleSettings.capExtraTime);

        JsonObject sne = new JsonObject();
        ruleSettingsJson.add("sne", sne);
        sne.addProperty("time", this.ruleSettings.sneTime);
        sne.addProperty("rounds", this.ruleSettings.sneRounds);
        sne.addProperty("snake", this.ruleSettings.sneSnake);

        JsonObject base = new JsonObject();
        ruleSettingsJson.add("base", base);
        base.addProperty("time", this.ruleSettings.baseTime);
        base.addProperty("rounds", this.ruleSettings.baseRounds);

        JsonObject bomb = new JsonObject();
        ruleSettingsJson.add("bomb", bomb);
        bomb.addProperty("time", this.ruleSettings.bombTime);
        bomb.addProperty("rounds", this.ruleSettings.bombRounds);

        JsonObject tsne = new JsonObject();
        ruleSettingsJson.add("tsne", tsne);
        tsne.addProperty("time", this.ruleSettings.tsneTime);
        tsne.addProperty("rounds", this.ruleSettings.tsneRounds);

        JsonObject sdm = new JsonObject();
        ruleSettingsJson.add("sdm", sdm);
        sdm.addProperty("time", this.ruleSettings.sdmTime);
        sdm.addProperty("rounds", this.ruleSettings.sdmRounds);

        JsonObject intr = new JsonObject();
        ruleSettingsJson.add("int", intr);
        intr.addProperty("time", this.ruleSettings.intTime);

        JsonObject scap = new JsonObject();
        ruleSettingsJson.add("scap", scap);
        scap.addProperty("time", this.ruleSettings.scapTime);
        scap.addProperty("rounds", this.ruleSettings.scapRounds);
        scap.addProperty("extraTime", this.ruleSettings.scapExtraTime);

        JsonObject race = new JsonObject();
        ruleSettingsJson.add("race", race);
        race.addProperty("time", this.ruleSettings.raceTime);
        race.addProperty("rounds", this.ruleSettings.raceRounds);
        race.addProperty("extraTime", this.ruleSettings.raceExtraTime);

        return Util.jsonEncode(settings);
    }
}
