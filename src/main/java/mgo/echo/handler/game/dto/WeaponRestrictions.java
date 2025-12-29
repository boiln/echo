package mgo.echo.handler.game.dto;

import com.google.gson.JsonObject;

/** Weapon restrictions parsed from JSON common field. */
public class WeaponRestrictions {
    public boolean enabled;

    // Primary
    public boolean vz;
    public boolean p90;
    public boolean mp5;
    public boolean patriot;
    public boolean ak;
    public boolean m4;
    public boolean mk17;
    public boolean xm8;
    public boolean g3a3;
    public boolean svd;
    public boolean mosin;
    public boolean m14;
    public boolean vss;
    public boolean dsr;
    public boolean m870;
    public boolean saiga;
    public boolean m60;
    public boolean shield;
    public boolean rpg;
    public boolean knife;

    // Secondary
    public boolean gsr;
    public boolean mk2;
    public boolean operator;
    public boolean g18;
    public boolean mk23;
    public boolean de;

    // Support
    public boolean grenade;
    public boolean wp;
    public boolean stun;
    public boolean chaff;
    public boolean smoke;
    public boolean smokeR;
    public boolean smokeG;
    public boolean smokeY;
    public boolean eloc;
    public boolean claymore;
    public boolean sgmine;
    public boolean c4;
    public boolean sgsatchel;
    public boolean magazine;

    // Custom
    public boolean suppressor;
    public boolean gp30;
    public boolean xm320;
    public boolean masterkey;
    public boolean scope;
    public boolean sight;
    public boolean laser;
    public boolean lighthg;
    public boolean lightlg;
    public boolean grip;

    // Items
    public boolean envg;
    public boolean drum;

    public static WeaponRestrictions parse(JsonObject common) {
        WeaponRestrictions w = new WeaponRestrictions();

        JsonObject wr = common.get("weaponRestrictions").getAsJsonObject();
        w.enabled = wr.get("enabled").getAsBoolean();

        JsonObject primary = wr.get("primary").getAsJsonObject();
        w.vz = primary.get("vz").getAsBoolean();
        w.p90 = primary.get("p90").getAsBoolean();
        w.mp5 = primary.get("mp5").getAsBoolean();
        w.patriot = primary.get("patriot").getAsBoolean();
        w.ak = primary.get("ak").getAsBoolean();
        w.m4 = primary.get("m4").getAsBoolean();
        w.mk17 = primary.get("mk17").getAsBoolean();
        w.xm8 = primary.get("xm8").getAsBoolean();
        w.g3a3 = primary.get("g3a3").getAsBoolean();
        w.svd = primary.get("svd").getAsBoolean();
        w.mosin = primary.get("mosin").getAsBoolean();
        w.m14 = primary.get("m14").getAsBoolean();
        w.vss = primary.get("vss").getAsBoolean();
        w.dsr = primary.get("dsr").getAsBoolean();
        w.m870 = primary.get("m870").getAsBoolean();
        w.saiga = primary.get("saiga").getAsBoolean();
        w.m60 = primary.get("m60").getAsBoolean();
        w.shield = primary.get("shield").getAsBoolean();
        w.rpg = primary.get("rpg").getAsBoolean();
        w.knife = primary.get("knife").getAsBoolean();

        JsonObject secondary = wr.get("secondary").getAsJsonObject();
        w.gsr = secondary.get("gsr").getAsBoolean();
        w.mk2 = secondary.get("mk2").getAsBoolean();
        w.operator = secondary.get("operator").getAsBoolean();
        w.g18 = secondary.get("g18").getAsBoolean();
        w.mk23 = secondary.get("mk23").getAsBoolean();
        w.de = secondary.get("de").getAsBoolean();

        JsonObject support = wr.get("support").getAsJsonObject();
        w.grenade = support.get("grenade").getAsBoolean();
        w.wp = support.get("wp").getAsBoolean();
        w.stun = support.get("stun").getAsBoolean();
        w.chaff = support.get("chaff").getAsBoolean();
        w.smoke = support.get("smoke").getAsBoolean();
        w.smokeR = support.get("smoke_r").getAsBoolean();
        w.smokeG = support.get("smoke_g").getAsBoolean();
        w.smokeY = support.get("smoke_y").getAsBoolean();
        w.eloc = support.get("eloc").getAsBoolean();
        w.claymore = support.get("claymore").getAsBoolean();
        w.sgmine = support.get("sgmine").getAsBoolean();
        w.c4 = support.get("c4").getAsBoolean();
        w.sgsatchel = support.get("sgsatchel").getAsBoolean();
        w.magazine = support.get("magazine").getAsBoolean();

        JsonObject custom = wr.get("custom").getAsJsonObject();
        w.suppressor = custom.get("suppressor").getAsBoolean();
        w.gp30 = custom.get("gp30").getAsBoolean();
        w.xm320 = custom.get("xm320").getAsBoolean();
        w.masterkey = custom.get("masterkey").getAsBoolean();
        w.scope = custom.get("scope").getAsBoolean();
        w.sight = custom.get("sight").getAsBoolean();
        w.laser = custom.get("laser").getAsBoolean();
        w.lighthg = custom.get("lighthg").getAsBoolean();
        w.lightlg = custom.get("lightlg").getAsBoolean();
        w.grip = custom.get("grip").getAsBoolean();

        JsonObject items = wr.get("items").getAsJsonObject();
        w.envg = items.get("envg").getAsBoolean();
        w.drum = items.get("drum").getAsBoolean();

        return w;
    }

    /** Build the 16-byte weapon restriction flags array. */
    public byte[] toBytes() {
        byte[] wr = new byte[0x10];

        wr[0] |= enabled ? 0b1 : 0;
        wr[0] |= !knife ? 0b10 : 0;
        wr[0] |= !mk2 ? 0b100 : 0;
        wr[0] |= !operator ? 0b1000 : 0;
        wr[0] |= !mk23 ? 0b10000 : 0;
        wr[0] |= !gsr ? 0b10000000 : 0;

        wr[1] |= !de ? 0b1 : 0;
        wr[1] |= !g18 ? 0b10000000 : 0;

        wr[2] |= !mp5 ? 0b100 : 0;
        wr[2] |= !p90 ? 0b10000 : 0;
        wr[2] |= !patriot ? 0b1000000 : 0;
        wr[2] |= !vz ? 0b10000000 : 0;

        wr[3] |= !m4 ? 0b1 : 0;
        wr[3] |= !ak ? 0b10 : 0;
        wr[3] |= !g3a3 ? 0b100 : 0;
        wr[3] |= !mk17 ? 0b1000000 : 0;
        wr[3] |= !xm8 ? 0b10000000 : 0;

        wr[4] |= !m60 ? 0b1000 : 0;
        wr[4] |= !m870 ? 0b100000 : 0;
        wr[4] |= !saiga ? 0b1000000 : 0;
        wr[4] |= !vss ? 0b10000000 : 0;

        wr[5] |= !dsr ? 0b10 : 0;
        wr[5] |= !m14 ? 0b100 : 0;
        wr[5] |= !mosin ? 0b1000 : 0;
        wr[5] |= !svd ? 0b10000 : 0;

        wr[6] |= !rpg ? 0b100 : 0;
        wr[6] |= !grenade ? 0b10000 : 0;
        wr[6] |= !wp ? 0b100000 : 0;
        wr[6] |= !stun ? 0b1000000 : 0;
        wr[6] |= !chaff ? 0b10000000 : 0;

        wr[7] |= !smoke ? 0b1 : 0;
        wr[7] |= !smokeR ? 0b10 : 0;
        wr[7] |= !smokeG ? 0b100 : 0;
        wr[7] |= !smokeY ? 0b1000 : 0;
        wr[7] |= !eloc ? 0b10000000 : 0;

        wr[8] |= !claymore ? 0b1 : 0;
        wr[8] |= !sgmine ? 0b10 : 0;
        wr[8] |= !c4 ? 0b100 : 0;
        wr[8] |= !sgsatchel ? 0b1000 : 0;
        wr[8] |= !magazine ? 0b100000 : 0;

        wr[9] |= !shield ? 0b10 : 0;
        wr[9] |= !masterkey ? 0b100 : 0;
        wr[9] |= !xm320 ? 0b1000 : 0;
        wr[9] |= !gp30 ? 0b10000 : 0;
        wr[9] |= !suppressor ? 0b100000 : 0;

        wr[10] |= !suppressor ? 0b1110 : 0;

        wr[11] |= !scope ? 0b10000 : 0;
        wr[11] |= !sight ? 0b100000 : 0;
        wr[11] |= !lightlg ? 0b10000000 : 0;

        wr[12] |= !laser ? 0b1 : 0;
        wr[12] |= !lighthg ? 0b10 : 0;
        wr[12] |= !grip ? 0b100 : 0;

        wr[13] |= !drum ? 0b100 : 0;

        wr[14] |= !envg ? 0b1000000 : 0;

        return wr;
    }
}
