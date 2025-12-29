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
        bo.writeByte(0x01);
        bo.writeByte(0x11);
        bo.writeByte(0x00);
        bo.writeByte(0x01);
        bo.writeInt(0); // totalReward
        bo.writeInt(data.playtimeSeconds);

        String comment = data.comment != null ? data.comment : "";
        int commentLen = Math.min(comment.length(), 127);
        bo.writeByte(commentLen);
        Util.writeString(comment, 127, bo);

        bo.writeInt(0);
        bo.writeByte(data.hasClan ? 0x01 : 0x00);
        Util.writeString(data.clanName, 16, bo);
        bo.writeByte(data.level);
        bo.writeInt(0);
        bo.writeInt(0);
        bo.writeInt(0);
        bo.writeByte(0x0C);
        bo.writeShort(0);
        bo.writeByte(0x0F);
        bo.writeByte(0x00);
        bo.writeByte(0x01);
        bo.writeByte(0x02);

        Packets.write(ctx, CharactersCmd.GET_CHARACTER_CARD_RESPONSE, bo);
    }
}
