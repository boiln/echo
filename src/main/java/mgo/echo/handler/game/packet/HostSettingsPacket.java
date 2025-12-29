package mgo.echo.handler.game.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import mgo.echo.handler.game.dto.HostSettingsDto;
import mgo.echo.handler.game.dto.HostSettingsDto.CommonSettings;
import mgo.echo.handler.game.dto.HostSettingsDto.RuleSettings;
import mgo.echo.handler.game.dto.HostSettingsDto.WeaponRestrictions;
import mgo.echo.protocol.command.HostsCmd;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet reader/writer for host settings (0x163 byte buffer).
 */
public class HostSettingsPacket {
    private static final Logger logger = LogManager.getLogger(HostSettingsPacket.class);

    public static final int BUFFER_SIZE = 0x163;
    public static final int GAMES_BYTES_SIZE = 0x2d;
    public static final int WR_SIZE = 0x10;

    private HostSettingsPacket() {
    }

    /**
     * Write host settings response to context.
     */
    public static void writeResponse(ChannelHandlerContext ctx, HostSettingsDto settings) {
        ByteBuf bo = null;

        try {
            bo = ctx.alloc().directBuffer(BUFFER_SIZE);
            write(bo, settings);
            Packets.write(ctx, HostsCmd.GET_SETTINGS_RESPONSE, bo);
        } catch (Exception e) {
            logger.error("Exception while writing host settings.", e);
            Util.releaseBuffer(bo);
            Packets.writeError(ctx, HostsCmd.GET_SETTINGS_RESPONSE, 1);
        }
    }

    /**
     * Write host settings to buffer for GET_SETTINGS_RESPONSE.
     */
    public static void write(ByteBuf bo, HostSettingsDto settings) {
        CommonSettings common = settings.common;
        RuleSettings rules = settings.ruleSettings;
        WeaponRestrictions wr = common.weaponRestrictions;

        int commonA = buildCommonA(common);
        int commonB = buildCommonB(common);
        int commonC = 0x20;

        int extraTimeFlags = buildExtraTimeFlags(rules);
        int hostOptions = buildHostOptions(common);
        byte[] wrBytes = buildWeaponRestrictionBytes(wr);

        bo.writeInt(0);
        Util.writeString(settings.name, 0x10, bo);
        Util.writeString(settings.comment, 0x80, bo);

        writePassword(bo, settings.password);
        bo.writeBoolean(common.dedicated);
        writeGames(bo, settings.games);

        Util.padTo(0xd8, bo);
        bo.writeBytes(wrBytes, 0, WR_SIZE);
        bo.writeByte(common.maxPlayers);
        bo.writeInt(common.briefingTime);
        bo.writeByte(0x2);

        Util.padTo(0xf9, bo);
        bo.writeByte(settings.stance);
        bo.writeByte(common.levelLimitTolerance);
        bo.writeInt(common.levelLimitBase);

        writeRuleSettings(bo, rules, common);

        bo.writeByte(commonA);
        bo.writeByte(commonB);
        bo.writeByte(commonC);
        bo.writeZero(1);
        bo.writeByte(common.idleKick);
        bo.writeZero(1);
        bo.writeByte(common.teamKillKick);
        bo.writeBoolean(rules.capExtraTime);
        bo.writeByte(rules.sneSnake);
        bo.writeByte(rules.sdmTime);
        bo.writeByte(rules.sdmRounds);
        bo.writeByte(rules.intTime);
        bo.writeByte(rules.dmRounds);
        bo.writeByte(rules.scapTime);
        bo.writeByte(rules.scapRounds);
        bo.writeByte(rules.raceTime);
        bo.writeByte(rules.raceRounds);
        bo.writeZero(1);
        bo.writeByte(extraTimeFlags);
        bo.writeByte(hostOptions);
        bo.writeZero(10);
    }

    /**
     * Read host settings from buffer for CHECK_SETTINGS.
     */
    public static HostSettingsDto read(ByteBuf bi) {
        HostSettingsDto settings = new HostSettingsDto();

        settings.name = Util.readString(bi, 0x10, CharsetUtil.UTF_8);
        settings.comment = Util.readString(bi, 0x80, CharsetUtil.UTF_8);

        boolean passwordEnabled = bi.readBoolean();
        settings.password = readPassword(bi, passwordEnabled);

        settings.common.dedicated = bi.readBoolean();

        bi.skipBytes(1); // lobbySubtype2 - not used

        byte[] gamesBytes = new byte[GAMES_BYTES_SIZE];
        bi.readBytes(gamesBytes);

        bi.skipBytes(5);
        byte[] wrBytes = new byte[WR_SIZE];
        bi.readBytes(wrBytes);

        settings.common.maxPlayers = bi.readByte();
        settings.common.briefingTime = bi.readInt();
        bi.skipBytes(0xc);
        settings.stance = bi.readByte();
        settings.common.levelLimitTolerance = bi.readByte();
        settings.common.levelLimitBase = bi.readInt();

        readRuleSettingsInts(bi, settings.ruleSettings);

        int uniqueRed = bi.readByte();
        int uniqueBlue = bi.readByte();
        int commonA = bi.readByte();
        int commonB = bi.readByte();
        bi.skipBytes(1); // commonC - not used
        bi.skipBytes(1);
        int idleKick = bi.readByte();
        bi.skipBytes(1);
        int teamKillKick = bi.readByte();
        settings.ruleSettings.capExtraTime = bi.readBoolean();
        settings.ruleSettings.sneSnake = bi.readByte();
        settings.ruleSettings.sdmTime = bi.readByte();
        settings.ruleSettings.sdmRounds = bi.readByte();
        settings.ruleSettings.intTime = bi.readByte();
        settings.ruleSettings.dmRounds = bi.readByte();
        settings.ruleSettings.scapTime = bi.readByte();
        settings.ruleSettings.scapRounds = bi.readByte();
        settings.ruleSettings.raceTime = bi.readByte();
        settings.ruleSettings.raceRounds = bi.readByte();

        bi.skipBytes(1);
        int extraTimeFlags = bi.readByte();
        int hostOptions = bi.readByte();

        settings.games = parseGames(gamesBytes);
        parseCommonFlags(settings.common, commonA, commonB, hostOptions, idleKick, teamKillKick);
        parseUniques(settings.common, uniqueRed, uniqueBlue, commonA);
        parseExtraTimeFlags(settings.ruleSettings, extraTimeFlags);
        parseWeaponRestrictions(settings.common.weaponRestrictions, wrBytes);

        return settings;
    }

    private static void writePassword(ByteBuf bo, String password) {
        if (password != null) {
            bo.writeByte(1);
            Util.writeString(password, 0x0f, bo);
            bo.writeZero(1);
            return;
        }

        bo.writeZero(0x11);
    }

    private static String readPassword(ByteBuf bi, boolean passwordEnabled) {
        if (passwordEnabled) {
            String password = Util.readString(bi, 0xf, CharsetUtil.UTF_8);
            bi.skipBytes(1);
            return password;
        }

        bi.skipBytes(0x10);
        return null;
    }

    private static void writeGames(ByteBuf bo, JsonArray games) {
        for (JsonElement o : games) {
            JsonArray game = (JsonArray) o;
            int rule = game.get(0).getAsInt();
            int map = game.get(1).getAsInt();
            int flags = game.get(2).getAsInt();
            bo.writeByte(rule).writeByte(map).writeByte(flags);
        }
    }

    private static JsonArray parseGames(byte[] gamesBytes) {
        JsonArray games = new JsonArray();

        for (int i = 0; i < 15; i++) {
            int rule = gamesBytes[i * 3];
            int map = gamesBytes[i * 3 + 1];
            int flags = gamesBytes[i * 3 + 2];

            if (rule == 0 && map == 0) {
                break;
            }

            JsonArray game = new JsonArray();
            game.add(rule);
            game.add(map);
            game.add(flags);
            games.add(game);
        }

        return games;
    }

    private static void writeRuleSettings(ByteBuf bo, RuleSettings rules, CommonSettings common) {
        int uniqueRed = common.uniqueRed;
        int uniqueBlue = common.uniqueBlue;

        if (common.uniquesRandom) {
            uniqueRed += 0x80;
            uniqueBlue += 0x80;
        }

        bo.writeInt(rules.sneTime);
        bo.writeInt(rules.sneRounds);
        bo.writeInt(rules.capTime);
        bo.writeInt(rules.capRounds);
        bo.writeInt(rules.resTime);
        bo.writeInt(rules.resRounds);
        bo.writeInt(rules.tdmTime);
        bo.writeInt(rules.tdmRounds);
        bo.writeInt(rules.tdmTickets);
        bo.writeInt(rules.dmTime);
        bo.writeInt(rules.dmTickets);
        bo.writeInt(rules.baseTime);
        bo.writeInt(rules.baseRounds);
        bo.writeInt(rules.bombTime);
        bo.writeInt(rules.bombRounds);
        bo.writeInt(rules.tsneTime);
        bo.writeInt(rules.tsneRounds);
        bo.writeByte(uniqueRed);
        bo.writeByte(uniqueBlue);
    }

    private static void readRuleSettingsInts(ByteBuf bi, RuleSettings rules) {
        rules.sneTime = bi.readInt();
        rules.sneRounds = bi.readInt();
        rules.capTime = bi.readInt();
        rules.capRounds = bi.readInt();
        rules.resTime = bi.readInt();
        rules.resRounds = bi.readInt();
        rules.tdmTime = bi.readInt();
        rules.tdmRounds = bi.readInt();
        rules.tdmTickets = bi.readInt();
        rules.dmTime = bi.readInt();
        rules.dmTickets = bi.readInt();
        rules.baseTime = bi.readInt();
        rules.baseRounds = bi.readInt();
        rules.bombTime = bi.readInt();
        rules.bombRounds = bi.readInt();
        rules.tsneTime = bi.readInt();
        rules.tsneRounds = bi.readInt();
    }

    private static int buildCommonA(CommonSettings common) {
        int commonA = 0b100;
        commonA |= common.idleKick > 0 ? 0b1 : 0;
        commonA |= common.friendlyFire ? 0b1000 : 0;
        commonA |= common.ghosts ? 0b10000 : 0;
        commonA |= common.autoAim ? 0b100000 : 0;
        commonA |= common.uniquesEnabled ? 0b10000000 : 0;
        return commonA;
    }

    private static int buildCommonB(CommonSettings common) {
        int commonB = 0;
        commonB |= common.teamsSwitch ? 0b1 : 0;
        commonB |= common.autoAssign ? 0b10 : 0;
        commonB |= common.silentMode ? 0b100 : 0;
        commonB |= common.enemyNametags ? 0b1000 : 0;
        commonB |= common.levelLimitEnabled ? 0b10000 : 0;
        commonB |= common.voiceChat ? 0b1000000 : 0;
        commonB |= common.teamKillKick > 0 ? 0b10000000 : 0;
        return commonB;
    }

    private static int buildExtraTimeFlags(RuleSettings rules) {
        int extraTimeFlags = 0;
        extraTimeFlags |= !rules.scapExtraTime ? 0b1 : 0;
        extraTimeFlags |= !rules.raceExtraTime ? 0b100 : 0;
        return extraTimeFlags;
    }

    private static int buildHostOptions(CommonSettings common) {
        int hostOptions = 0;
        hostOptions |= common.nonStat ? 0b10 : 0;
        return hostOptions;
    }

    private static byte[] buildWeaponRestrictionBytes(WeaponRestrictions wr) {
        byte[] wrBytes = new byte[WR_SIZE];

        wrBytes[0] |= wr.enabled ? 0b1 : 0;
        wrBytes[0] |= !wr.knife ? 0b10 : 0;
        wrBytes[0] |= !wr.mk2 ? 0b100 : 0;
        wrBytes[0] |= !wr.operator ? 0b1000 : 0;
        wrBytes[0] |= !wr.mk23 ? 0b10000 : 0;
        wrBytes[0] |= !wr.gsr ? 0b10000000 : 0;

        wrBytes[1] |= !wr.de ? 0b1 : 0;
        wrBytes[1] |= !wr.g18 ? 0b10000000 : 0;

        wrBytes[2] |= !wr.mp5 ? 0b100 : 0;
        wrBytes[2] |= !wr.p90 ? 0b10000 : 0;
        wrBytes[2] |= !wr.patriot ? 0b1000000 : 0;
        wrBytes[2] |= !wr.vz ? 0b10000000 : 0;

        wrBytes[3] |= !wr.m4 ? 0b1 : 0;
        wrBytes[3] |= !wr.ak ? 0b10 : 0;
        wrBytes[3] |= !wr.g3a3 ? 0b100 : 0;
        wrBytes[3] |= !wr.mk17 ? 0b1000000 : 0;
        wrBytes[3] |= !wr.xm8 ? 0b10000000 : 0;

        wrBytes[4] |= !wr.m60 ? 0b1000 : 0;
        wrBytes[4] |= !wr.m870 ? 0b100000 : 0;
        wrBytes[4] |= !wr.saiga ? 0b1000000 : 0;
        wrBytes[4] |= !wr.vss ? 0b10000000 : 0;

        wrBytes[5] |= !wr.dsr ? 0b10 : 0;
        wrBytes[5] |= !wr.m14 ? 0b100 : 0;
        wrBytes[5] |= !wr.mosin ? 0b1000 : 0;
        wrBytes[5] |= !wr.svd ? 0b10000 : 0;

        wrBytes[6] |= !wr.rpg ? 0b100 : 0;
        wrBytes[6] |= !wr.grenade ? 0b10000 : 0;
        wrBytes[6] |= !wr.wp ? 0b100000 : 0;
        wrBytes[6] |= !wr.stun ? 0b1000000 : 0;
        wrBytes[6] |= !wr.chaff ? 0b10000000 : 0;

        wrBytes[7] |= !wr.smoke ? 0b1 : 0;
        wrBytes[7] |= !wr.smoke_r ? 0b10 : 0;
        wrBytes[7] |= !wr.smoke_g ? 0b100 : 0;
        wrBytes[7] |= !wr.smoke_y ? 0b1000 : 0;
        wrBytes[7] |= !wr.eloc ? 0b10000000 : 0;

        wrBytes[8] |= !wr.claymore ? 0b1 : 0;
        wrBytes[8] |= !wr.sgmine ? 0b10 : 0;
        wrBytes[8] |= !wr.c4 ? 0b100 : 0;
        wrBytes[8] |= !wr.sgsatchel ? 0b1000 : 0;
        wrBytes[8] |= !wr.magazine ? 0b100000 : 0;

        wrBytes[9] |= !wr.shield ? 0b10 : 0;
        wrBytes[9] |= !wr.masterkey ? 0b100 : 0;
        wrBytes[9] |= !wr.xm320 ? 0b1000 : 0;
        wrBytes[9] |= !wr.gp30 ? 0b10000 : 0;
        wrBytes[9] |= !wr.suppressor ? 0b100000 : 0;

        wrBytes[10] |= !wr.suppressor ? 0b1110 : 0;

        wrBytes[11] |= !wr.scope ? 0b10000 : 0;
        wrBytes[11] |= !wr.sight ? 0b100000 : 0;
        wrBytes[11] |= !wr.lightlg ? 0b10000000 : 0;

        wrBytes[12] |= !wr.laser ? 0b1 : 0;
        wrBytes[12] |= !wr.lighthg ? 0b10 : 0;
        wrBytes[12] |= !wr.grip ? 0b100 : 0;

        wrBytes[13] |= !wr.drum ? 0b100 : 0;

        wrBytes[14] |= !wr.envg ? 0b1000000 : 0;

        return wrBytes;
    }

    private static void parseCommonFlags(CommonSettings common, int commonA, int commonB,
            int hostOptions, int idleKick, int teamKillKick) {
        common.nonStat = (hostOptions & 0b10) == 0b10;
        common.friendlyFire = (commonA & 0b1000) == 0b1000;
        common.autoAim = (commonA & 0b100000) == 0b100000;
        common.enemyNametags = (commonB & 0b1000) == 0b1000;
        common.silentMode = (commonB & 0b100) == 0b100;
        common.autoAssign = (commonB & 0b10) == 0b10;
        common.teamsSwitch = (commonB & 0b1) == 0b1;
        common.ghosts = (commonA & 0b10000) == 0b10000;
        common.voiceChat = (commonB & 0b1000000) == 0b1000000;
        common.levelLimitEnabled = (commonB & 0b10000) == 0b10000;

        int adjustedTeamKillKick = teamKillKick;
        int adjustedIdleKick = idleKick;
        if ((commonB & 0b10000000) != 0b10000000) {
            adjustedTeamKillKick = 0;
        }
        if ((commonA & 0b1) != 0b1) {
            adjustedIdleKick = 0;
        }

        common.teamKillKick = adjustedTeamKillKick;
        common.idleKick = adjustedIdleKick;
    }

    private static void parseUniques(CommonSettings common, int uniqueRed, int uniqueBlue, int commonA) {
        common.uniquesEnabled = (commonA & 0b10000000) == 0b10000000;
        common.uniquesRandom = (uniqueRed & 0x80) == 0x80;
        int adjustedRed = uniqueRed;
        int adjustedBlue = uniqueBlue;
        if (common.uniquesRandom) {
            adjustedRed = uniqueRed & 0x7F;
            adjustedBlue = uniqueBlue & 0x7F;
        }

        common.uniqueRed = adjustedRed;
        common.uniqueBlue = adjustedBlue;
    }

    private static void parseExtraTimeFlags(RuleSettings rules, int extraTimeFlags) {
        rules.scapExtraTime = (extraTimeFlags & 0b1) == 0;
        rules.raceExtraTime = (extraTimeFlags & 0b100) == 0;
    }

    private static void parseWeaponRestrictions(WeaponRestrictions wr, byte[] wrBytes) {
        wr.enabled = (wrBytes[0] & 0b1) == 0b1;

        wr.vz = (wrBytes[2] & 0b10000000) == 0;
        wr.p90 = (wrBytes[2] & 0b10000) == 0;
        wr.mp5 = (wrBytes[2] & 0b100) == 0;
        wr.patriot = (wrBytes[2] & 0b1000000) == 0;
        wr.ak = (wrBytes[3] & 0b10) == 0;
        wr.m4 = (wrBytes[3] & 0b1) == 0;
        wr.mk17 = (wrBytes[3] & 0b1000000) == 0;
        wr.xm8 = (wrBytes[3] & 0b10000000) == 0;
        wr.g3a3 = (wrBytes[3] & 0b100) == 0;
        wr.svd = (wrBytes[5] & 0b10000) == 0;
        wr.mosin = (wrBytes[5] & 0b1000) == 0;
        wr.m14 = (wrBytes[5] & 0b100) == 0;
        wr.vss = (wrBytes[4] & 0b10000000) == 0;
        wr.dsr = (wrBytes[5] & 0b10) == 0;
        wr.m870 = (wrBytes[4] & 0b100000) == 0;
        wr.saiga = (wrBytes[4] & 0b1000000) == 0;
        wr.m60 = (wrBytes[4] & 0b1000) == 0;
        wr.shield = (wrBytes[9] & 0b10) == 0;
        wr.rpg = (wrBytes[6] & 0b100) == 0;
        wr.knife = (wrBytes[0] & 0b10) == 0;

        wr.gsr = (wrBytes[0] & 0b10000000) == 0;
        wr.mk2 = (wrBytes[0] & 0b100) == 0;
        wr.operator = (wrBytes[0] & 0b1000) == 0;
        wr.g18 = (wrBytes[1] & 0b10000000) == 0;
        wr.mk23 = (wrBytes[0] & 0b10000) == 0;
        wr.de = (wrBytes[1] & 0b1) == 0;

        wr.grenade = (wrBytes[6] & 0b10000) == 0;
        wr.wp = (wrBytes[6] & 0b100000) == 0;
        wr.stun = (wrBytes[6] & 0b1000000) == 0;
        wr.chaff = (wrBytes[6] & 0b10000000) == 0;
        wr.smoke = (wrBytes[7] & 0b1) == 0;
        wr.smoke_r = (wrBytes[7] & 0b10) == 0;
        wr.smoke_g = (wrBytes[7] & 0b100) == 0;
        wr.smoke_y = (wrBytes[7] & 0b1000) == 0;
        wr.eloc = (wrBytes[7] & 0b10000000) == 0;
        wr.claymore = (wrBytes[8] & 0b1) == 0;
        wr.sgmine = (wrBytes[8] & 0b10) == 0;
        wr.c4 = (wrBytes[8] & 0b100) == 0;
        wr.sgsatchel = (wrBytes[8] & 0b1000) == 0;
        wr.magazine = (wrBytes[8] & 0b100000) == 0;

        wr.suppressor = (wrBytes[9] & 0b100000) == 0;
        wr.gp30 = (wrBytes[9] & 0b10000) == 0;
        wr.xm320 = (wrBytes[9] & 0b1000) == 0;
        wr.masterkey = (wrBytes[9] & 0b100) == 0;
        wr.scope = (wrBytes[11] & 0b10000) == 0;
        wr.sight = (wrBytes[11] & 0b100000) == 0;
        wr.laser = (wrBytes[12] & 0b1) == 0;
        wr.lighthg = (wrBytes[12] & 0b10) == 0;
        wr.lightlg = (wrBytes[11] & 0b10000000) == 0;
        wr.grip = (wrBytes[12] & 0b100) == 0;

        wr.envg = (wrBytes[14] & 0b1000000) == 0;
        wr.drum = (wrBytes[13] & 0b100) == 0;
    }

    /**
     * Check if games array contains a Clan Room (rule 11).
     */
    public static boolean hasClanRoom(JsonArray games) {
        for (int i = 0; i < games.size(); i++) {
            JsonArray game = games.get(i).getAsJsonArray();
            int rule = game.get(0).getAsInt();
            if (rule == 11) {
                return true;
            }
        }
        
        return false;
    }
}
