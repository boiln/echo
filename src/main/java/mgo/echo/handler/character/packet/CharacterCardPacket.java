package mgo.echo.handler.character.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.handler.character.service.CharacterService.CharacterCardData;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Character card packet writer.
 */
public class CharacterCardPacket {
    private CharacterCardPacket() {
    }

    public static void write(ChannelHandlerContext ctx, CharacterCardData data) {
        ByteBuf bo = ctx.alloc().directBuffer(207);

        bo.writeInt(0);
        bo.writeInt(data.charaId);
        Util.writeString(data.name, 16, bo);

        bo.writeShort(0);
        bo.writeShort(data.points);
        bo.writeShort(0);

        bo.writeInt(data.totalReward);
        bo.writeInt(data.playtimeSeconds);

        String comment = data.comment != null ? data.comment : "";
        bo.writeByte(0);
        Util.writeString(comment, 127, bo);

        bo.writeShort(0);
        bo.writeByte(0);
        bo.writeByte(data.hasClan ? 0x12 : 0x00);

        String clanTag = "";
        if (data.hasClan && data.clanName != null && !data.clanName.isEmpty()) {
            clanTag = ";" + data.clanName;
        }
        Util.writeString(clanTag, 13, bo);
        bo.writeByte(0);

        bo.writeInt(data.hasClan ? 1 : 0);
        bo.writeInt(0);
        bo.writeInt(0);
        bo.writeByte(data.hasEmblem ? 3 : 0);
        bo.writeInt(data.points);
        bo.writeInt(0x0F00);
        bo.writeShort(0x0100);

        Packets.write(ctx, CharactersCmd.GET_CHARACTER_CARD_RESPONSE, bo);
    }
}
