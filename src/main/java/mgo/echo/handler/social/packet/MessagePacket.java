package mgo.echo.handler.social.packet;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.MessageClanApplication;
import mgo.echo.handler.social.dto.MessageRecipientError;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Packet writers for message operations.
 */
public final class MessagePacket {

    private static final byte[] SEND_GENERAL_ERROR = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x53,
            (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
            (byte) 0x20, (byte) 0x65, (byte) 0x72, (byte) 0x72, (byte) 0x6F,
            (byte) 0x72, (byte) 0x21, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0xC0, (byte) 0xFF, (byte) 0xEE, (byte) 0x01
    };

    private MessagePacket() {
    }

    public static void writeSendError(ChannelHandlerContext ctx) {
        Packets.write(ctx, 0x4801, Unpooled.wrappedBuffer(SEND_GENERAL_ERROR));
    }

    public static void writeSendResponse(ChannelHandlerContext ctx, List<MessageRecipientError> errors) {
        ByteBuf bo = ctx.alloc().directBuffer(9 + errors.size() * 20);

        bo.writeInt(errors.isEmpty() ? 0 : 1);
        bo.writeByte(0).writeInt(errors.size());

        for (MessageRecipientError error : errors) {
            Util.writeString(error.getRecipient(), 16, bo);
            int code = error.getError().getCode();
            if (!error.getError().isOfficial()) {
                code |= 0xC0FFEE << 8;
            }
            bo.writeInt(code);
        }

        Packets.write(ctx, 0x4801, bo);
    }

    public static void writeMailMessages(ChannelHandlerContext ctx) {
        Packets.write(ctx, 0x4821, 0);
        Packets.write(ctx, 0x4823, 0);
    }

    public static void writeClanApplicationMessages(ChannelHandlerContext ctx, List<MessageClanApplication> messages) {
        ByteBuf[] payloads = new ByteBuf[messages.size()];

        for (int i = 0; i < payloads.length; i++) {
            ByteBuf bo = ctx.alloc().directBuffer(266);
            MessageClanApplication message = messages.get(i);

            int mtype = 0;
            boolean important = false;
            boolean read = false;
            int unk1 = 1;
            int unk2 = 0;

            bo.writeByte(mtype).writeByte(i).writeByte(unk1);
            Util.writeString(message.getCharacter().getName(), 128, bo);
            Util.writeString(message.getComment(), 128, bo);
            bo.writeInt(message.getTime()).writeByte(unk2).writeBoolean(important).writeBoolean(read);

            payloads[i] = bo;
        }

        Packets.write(ctx, 0x4821, 0);
        Packets.write(ctx, 0x4822, payloads);
        Packets.write(ctx, 0x4823, 0);
    }
}
