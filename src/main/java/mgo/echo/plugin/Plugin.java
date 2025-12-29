package mgo.echo.plugin;

import org.hibernate.cfg.Configuration;

import io.netty.channel.ChannelHandlerContext;
import mgo.echo.chat.ChatMessage;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.User;
import mgo.echo.protocol.Packet;

public class Plugin {
    public void initialize() {

    }

    public void onStart() {

    }

    public void addAnnotatedClass(Configuration configuration) {

    }

    public int handleGameLobbyCommand(ChannelHandlerContext ctx, Packet in) {
        return -1;
    }

    public ChatMessage handleChatCommand(User user, String message) {
        return null;
    }

    public void gameNCheck(Game game) {

    }
}
