package mgo.echo.handler.character.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterStats;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writer for personal stats (0x4103, 0x4105, 0x4107).
 */
public final class StatsPacket {
    private static final Logger logger = LogManager.getLogger(StatsPacket.class);

    private static final int STATS_BUFFER_SIZE = 1024;
    private static final int MODE_STATS_BLOCK_SIZE = 72;

    private StatsPacket() {
    }

    public static void write(ChannelHandlerContext ctx, int targetCharaId, Character targetCharacter,
            CharacterStats stats) {
        ByteBuf bo1 = null;
        ByteBuf bo2 = null;
        ByteBuf bo3 = null;
        ByteBuf bo4 = null;

        try {
            // Header packet (0x4103)
            bo1 = ctx.alloc().directBuffer(STATS_BUFFER_SIZE);
            bo1.writeInt(0);
            bo1.writeInt(targetCharaId);
            Util.writeString(targetCharacter.getName(), 16, bo1);
            bo1.writeZero(STATS_BUFFER_SIZE - bo1.writerIndex());

            // Per-mode stats packet (0x4105 first)
            bo2 = ctx.alloc().directBuffer(STATS_BUFFER_SIZE);
            bo2.writeZero(80);
            writeAggregateStatsBlock(bo2, stats);
            for (int mode = 0; mode <= 10; mode++) {
                writeModeStatsBlock(bo2, stats, mode);
            }
            bo2.writeZero(STATS_BUFFER_SIZE - bo2.writerIndex());

            // Extended stats packet (0x4105 second)
            bo3 = ctx.alloc().directBuffer(STATS_BUFFER_SIZE);
            bo3.writeInt(0);
            bo3.writeInt(1);
            bo3.writeZero(844);
            bo3.writeInt(stats != null ? stats.getScore() : 0);
            bo3.writeZero(STATS_BUFFER_SIZE - bo3.writerIndex());

            // Additional stats packet (0x4107)
            bo4 = ctx.alloc().directBuffer(STATS_BUFFER_SIZE);
            bo4.writeZero(20);
            bo4.writeInt(stats != null ? stats.getStunsReceived() : 0);
            bo4.writeZero(STATS_BUFFER_SIZE - bo4.writerIndex());

            Packets.write(ctx, CharactersCmd.GET_PERSONAL_STATS_HEADER, bo1);
            bo1 = null;
            Packets.write(ctx, CharactersCmd.GET_PERSONAL_STATS_MODE, bo2);
            bo2 = null;
            Packets.write(ctx, CharactersCmd.GET_PERSONAL_STATS_MODE, bo3);
            bo3 = null;
            Packets.write(ctx, CharactersCmd.GET_PERSONAL_STATS_ADDITIONAL, bo4);
            bo4 = null;

        } catch (Exception e) {
            logger.error("Exception while writing stats packet.", e);
            Packets.write(ctx, CharactersCmd.GET_PERSONAL_STATS_HEADER, Error.GENERAL);
        } finally {
            if (bo1 != null)
                bo1.release();
            if (bo2 != null)
                bo2.release();
            if (bo3 != null)
                bo3.release();
            if (bo4 != null)
                bo4.release();
        }
    }

    private static void writeAggregateStatsBlock(ByteBuf buf, CharacterStats stats) {
        if (stats == null) {
            buf.writeZero(MODE_STATS_BLOCK_SIZE);
            return;
        }

        buf.writeInt(stats.getKills());
        buf.writeInt(stats.getDeaths());
        buf.writeInt(stats.getLockKills());
        buf.writeInt(stats.getScore());
        buf.writeInt(stats.getStuns());
        buf.writeInt(stats.getStunsReceived());
        buf.writeInt(stats.getHeadshotKills());
        buf.writeInt(stats.getHeadshotDeaths());
        buf.writeInt(stats.getHeadshotStuns());
        buf.writeInt(stats.getHeadshotStunsReceived());
        buf.writeInt(stats.getLockStuns());
        buf.writeInt(stats.getLockDeaths());
        buf.writeInt(stats.getLockStunsReceived());
        buf.writeInt(stats.getScore());
        buf.writeInt(stats.getRounds());
        buf.writeInt(0);
        buf.writeInt(stats.getWins());
        buf.writeInt(stats.getTime());
    }

    private static void writeModeStatsBlock(ByteBuf buf, CharacterStats stats, int mode) {
        if (stats == null) {
            buf.writeZero(MODE_STATS_BLOCK_SIZE);
            return;
        }

        try {
            String json = stats.getStatsByMode(mode);
            if (json == null || json.isEmpty()) {
                buf.writeZero(MODE_STATS_BLOCK_SIZE);
                return;
            }

            JsonObject modeStats = Util.jsonDecode(json);

            buf.writeInt(getJsonInt(modeStats, "kills"));
            buf.writeInt(getJsonInt(modeStats, "deaths"));
            buf.writeInt(getJsonInt(modeStats, "lockKills"));
            buf.writeInt(getJsonInt(modeStats, "score"));
            buf.writeInt(getJsonInt(modeStats, "stuns"));
            buf.writeInt(getJsonInt(modeStats, "stunsRec"));
            buf.writeInt(getJsonInt(modeStats, "hsKills"));
            buf.writeInt(getJsonInt(modeStats, "hsDeaths"));
            buf.writeInt(getJsonInt(modeStats, "hsStuns"));
            buf.writeInt(getJsonInt(modeStats, "hsStunsRec"));
            buf.writeInt(getJsonInt(modeStats, "lockStuns"));
            buf.writeInt(getJsonInt(modeStats, "lockDeaths"));
            buf.writeInt(getJsonInt(modeStats, "lockStunsRec"));
            buf.writeInt(getJsonInt(modeStats, "score"));
            buf.writeInt(getJsonInt(modeStats, "rounds"));
            buf.writeInt(0);
            buf.writeInt(getJsonInt(modeStats, "wins"));
            buf.writeInt(getJsonInt(modeStats, "time"));
        } catch (Exception e) {
            logger.warn("Failed to write mode {} stats: {}", mode, e.getMessage());
            buf.writeZero(MODE_STATS_BLOCK_SIZE);
        }
    }

    private static int getJsonInt(JsonObject json, String key) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsInt();
        }
        return 0;
    }
}
