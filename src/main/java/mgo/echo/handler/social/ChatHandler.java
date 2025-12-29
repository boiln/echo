package mgo.echo.handler.social;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.chat.ChatMessage;
import mgo.echo.chat.MessageRecipient;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.Game;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.plugin.PluginHandler;
import mgo.echo.protocol.Packet;
import mgo.echo.session.ActiveChannels;
import mgo.echo.session.ActiveGames;
import mgo.echo.session.ActiveUsers;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

public class ChatHandler {
    private static final Logger logger = LogManager.getLogger(ChatHandler.class);
    private static final String SERVER_MESSAGE_PREFIX = "Server | ";

    public static ByteBuf constructMessage(int chara, int flag2, String message) {
        ByteBuf bb = PooledByteBufAllocator.DEFAULT.directBuffer(message.length() + 6);
        bb.writeInt(chara).writeByte(flag2);
        Util.writeString(message, message.length() + 1, bb);
        return bb;
    }

    public static ByteBuf constructMessage(ChannelHandlerContext ctx, int chara, int flag2, String message) {
        ByteBuf bb = ctx.alloc().directBuffer(message.length() + 6);
        bb.writeInt(chara).writeByte(flag2);
        Util.writeString(message, message.length() + 1, bb);
        return bb;
    }

    public static void sendServerMessageToSelf(ChannelHandlerContext ctx, String message) {
        String fmessage = SERVER_MESSAGE_PREFIX + message;
        ByteBuf bo = null;

        try {
            User targetUser = ActiveUsers.get(ctx.channel());
            Character targetCharacter = targetUser.getCurrentCharacter();

            bo = ChatHandler.constructMessage(ctx, targetCharacter.getId(), 0x30, fmessage);
            Packets.write(ctx, 0x4401, bo);
            Packets.flush(ctx);
        } catch (Exception e) {
            logger.error("Exception during chat processing.", e);
            Util.releaseBuffer(bo);
        }
    }

    public static void sendServerMessageToGame(String message, Game game) {
        String fmessage = SERVER_MESSAGE_PREFIX + message;

        try {
            ArrayList<Player> recipients = new ArrayList<>(game.getPlayers());

            ActiveChannels.process((ch) -> {
                try {
                    User targetUser = ActiveUsers.get(ch);
                    Character targetCharacter = targetUser.getCurrentCharacter();

                    if (targetCharacter == null) {
                        return false;
                    }

                    return recipients.stream()
                            .anyMatch(e -> e.getCharacterId() == targetCharacter.getId());
                } catch (Exception e) {
                    logger.error("Exception during chat processing.", e);
                    return false;
                }
            }, (ch) -> {
                ByteBuf bo = null;

                try {
                    User targetUser = ActiveUsers.get(ch);
                    Character targetCharacter = targetUser.getCurrentCharacter();

                    bo = constructMessage(targetCharacter.getId(), 0x30, fmessage);
                    Packets.write(ch, 0x4401, bo);
                    Packets.flush(ch);
                } catch (Exception e) {
                    logger.error("Exception during chat processing.", e);
                    Util.releaseBuffer(bo);
                }
            });
        } catch (Exception e) {
            logger.error("Exception during chat processing.", e);
        }
    }

    private static ChatMessage handleCommand(User user, String message) {
        // /global command
        if (message.startsWith("/global ")) {
            if (user.getRole() < 10) {
                return new ChatMessage(MessageRecipient.SELF, "You do not have permission to use this command.");
            }
            String out = message.replaceFirst("/global ", "");
            return new ChatMessage(MessageRecipient.GLOBAL, out);
        }

        // /room command
        if (message.startsWith("/room ")) {
            if (user.getRole() < 10) {
                return new ChatMessage(MessageRecipient.SELF, "You do not have permission to use this command.");
            }
            String out = message.replaceFirst("/room ", "");
            return new ChatMessage(MessageRecipient.ROOM, out);
        }

        // /kick command
        if (message.startsWith("/kick ")) {
            return handleKickCommand(user, message);
        }

        // /gamelog command
        if (message.startsWith("/gamelog")) {
            return handleGamelogCommand(user);
        }

        return null;
    }

    private static ChatMessage handleKickCommand(User user, String message) {
        if (user.getRole() < 10) {
            return new ChatMessage(MessageRecipient.SELF, "You do not have permission to use this command.");
        }

        try {
            String idStr = message.replaceFirst("/kick ", "");
            int targetId = Integer.parseInt(idStr);

            ActiveChannels.process((ch) -> {
                try {
                    User targetUser = ActiveUsers.get(ch);
                    Character targetCharacter = targetUser.getCurrentCharacter();

                    if (targetCharacter == null) {
                        return false;
                    }

                    return targetCharacter.getId() == targetId;
                } catch (Exception e) {
                    logger.error("Exception during /kicking character.", e);
                    return false;
                }
            }, (ch) -> {
                try {
                    logger.info("/kicking: {}", Util.getUserInfo(ch));
                    ch.close();
                } catch (Exception e) {
                    logger.error("Exception during /kicking character", e);
                }
            });

            return new ChatMessage(MessageRecipient.SELF, "Kicked character.");
        } catch (Exception e) {
            logger.error("Exception occurred while /kicking character", e);
            return new ChatMessage(MessageRecipient.SELF, "Failed to kick character.");
        }
    }

    private static ChatMessage handleGamelogCommand(User user) {
        if (user.getRole() < 10) {
            return new ChatMessage(MessageRecipient.SELF, "You do not have permission to use this command.");
        }

        try {
            Collection<Game> games = ActiveGames.getGames();

            for (Game aGame : games) {
                StringBuilder lout = new StringBuilder("GameLog | ");
                lout.append(aGame.getName()).append(" (").append(aGame.getId()).append(") | ");

                List<Player> aPlayers = aGame.getPlayers();
                for (Player aPlayer : aPlayers) {
                    lout.append(aPlayer.getCharacter().getName())
                            .append(" (").append(aPlayer.getCharacterId()).append("), ");
                }

                logger.info("{}", lout);
            }

            return new ChatMessage(MessageRecipient.SELF, "Logged all game info.");
        } catch (Exception e) {
            return new ChatMessage(MessageRecipient.SELF, "Failed to log info.");
        }
    }

    public static void send(ChannelHandlerContext ctx, Packet in) {
        try {
            User user = ActiveUsers.get(ctx.channel());
            if (user == null) {
                logger.error("Error while sending message: No user.");
                return;
            }

            Character character = user.getCurrentCharacter();
            Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
            if (player == null) {
                logger.error("Error while sending message: Not in a game.");
                return;
            }

            Game game = player.getGame();
            List<Player> players = game.getPlayers();

            ByteBuf bi = in.getPayload();
            int flag2 = bi.readByte();
            String message = Util.readString(bi, 0x7f);

            message = stripMessagePrefix(message);
            ChatMessage chatMessage = resolveCommand(user, message);

            dispatchMessage(ctx, chatMessage, character, flag2, players);
        } catch (Exception e) {
            logger.error("Exception while sending message.", e);
        }
    }

    private static String stripMessagePrefix(String message) {
        if (message.startsWith("/all")) {
            message = message.replaceFirst("/all", "");
        } else if (message.startsWith("/team")) {
            message = message.replaceFirst("/team", "");
        }

        if (message.matches("(\\s+).*")) {
            message = message.replaceFirst("(\\s+)", "");
        }

        return message;
    }

    private static ChatMessage resolveCommand(User user, String message) {
        ChatMessage chatMessage = PluginHandler.get().getPlugin().handleChatCommand(user, message);
        if (chatMessage != null) {
            return chatMessage;
        }

        chatMessage = handleCommand(user, message);
        if (chatMessage != null) {
            return chatMessage;
        }

        if (message.toLowerCase().startsWith(SERVER_MESSAGE_PREFIX.toLowerCase())) {
            return new ChatMessage(MessageRecipient.SELF, "You can't send server messages.");
        }

        return new ChatMessage(MessageRecipient.NORMAL, message);
    }

    private static void dispatchMessage(ChannelHandlerContext ctx, ChatMessage chatMessage,
            Character character, int flag2, List<Player> players) {
        switch (chatMessage.getRecipient()) {
            case NORMAL:
                sendNormalMessage(ctx, chatMessage, character, flag2, players);
                break;
            case SELF:
                sendSelfMessage(ctx, chatMessage, character, flag2);
                break;
            case ROOM:
                sendRoomMessage(ctx, chatMessage, character, flag2, players);
                break;
            case GLOBAL:
                sendGlobalMessage(ctx, chatMessage, character, flag2);
                break;
        }
    }

    private static void sendNormalMessage(ChannelHandlerContext ctx, ChatMessage chatMessage,
            Character character, int flag2, List<Player> players) {
        ArrayList<Player> recipients = new ArrayList<>(players);
        String fmessage = chatMessage.getMessage();

        ActiveChannels.process((ch) -> {
            try {
                User targetUser = ActiveUsers.get(ch);
                Character targetCharacter = targetUser.getCurrentCharacter();

                if (targetCharacter == null) {
                    return false;
                }

                return recipients.stream()
                        .anyMatch(e -> e.getCharacterId() == targetCharacter.getId());
            } catch (Exception e) {
                logger.error("Exception during chat processing.", e);
                return false;
            }
        }, (ch) -> {
            ByteBuf bo = null;

            try {
                bo = constructMessage(ctx, character.getId(), flag2, fmessage);
                Packets.write(ch, 0x4401, bo);
                Packets.flush(ch);
            } catch (Exception e) {
                logger.error("Exception during chat processing.", e);
                Util.releaseBuffer(bo);
            }
        });
    }

    private static void sendSelfMessage(ChannelHandlerContext ctx, ChatMessage chatMessage,
            Character character, int flag2) {
        String fmessage = SERVER_MESSAGE_PREFIX + chatMessage.getMessage();
        ByteBuf bo = null;

        try {
            bo = constructMessage(ctx, character.getId(), flag2, fmessage);
            Packets.write(ctx, 0x4401, bo);
        } catch (Exception e) {
            logger.error("Exception during chat processing.", e);
            Util.releaseBuffer(bo);
        }
    }

    private static void sendRoomMessage(ChannelHandlerContext ctx, ChatMessage chatMessage,
            Character character, int flag2, List<Player> players) {
        ArrayList<Player> recipients = new ArrayList<>(players);
        String fmessage = SERVER_MESSAGE_PREFIX + chatMessage.getMessage();

        ActiveChannels.process((ch) -> {
            try {
                User targetUser = ActiveUsers.get(ch);
                Character targetCharacter = targetUser.getCurrentCharacter();

                if (targetCharacter == null) {
                    return false;
                }

                return recipients.stream()
                        .anyMatch(e -> e.getCharacterId() == targetCharacter.getId());
            } catch (Exception e) {
                logger.error("Exception during chat processing.", e);
                return false;
            }
        }, (ch) -> {
            ByteBuf bo = null;

            try {
                bo = constructMessage(ctx, character.getId(), flag2, fmessage);
                Packets.write(ch, 0x4401, bo);
                Packets.flush(ch);
            } catch (Exception e) {
                logger.error("Exception during chat processing.", e);
                Util.releaseBuffer(bo);
            }
        });
    }

    private static void sendGlobalMessage(ChannelHandlerContext ctx, ChatMessage chatMessage,
            Character character, int flag2) {
        String fmessage = SERVER_MESSAGE_PREFIX + chatMessage.getMessage();

        ActiveChannels.process((ch) -> {
            try {
                User targetUser = ActiveUsers.get(ch);
                Character targetCharacter = targetUser.getCurrentCharacter();
                return targetCharacter != null;
            } catch (Exception e) {
                logger.error("Exception during chat processing.", e);
                return false;
            }
        }, (ch) -> {
            ByteBuf bo = null;

            try {
                User targetUser = ActiveUsers.get(ch);
                Character targetCharacter = targetUser.getCurrentCharacter();

                bo = constructMessage(ctx, targetCharacter.getId(), flag2, fmessage);
                Packets.write(ch, 0x4401, bo);
                Packets.flush(ch);
            } catch (Exception e) {
                logger.error("Exception during chat processing.", e);
                Util.releaseBuffer(bo);
            }
        });
    }
}
