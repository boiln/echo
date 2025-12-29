package mgo.echo.util;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface ElementConsumer {
    void accept(Integer index, ByteBuf payload) throws Exception;
}
