package mgo.echo.util;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.ClanMember;
import mgo.echo.data.entity.Player;
import mgo.echo.data.entity.User;
import mgo.echo.session.ActiveUsers;

/**
 * Two styles:
 * - "require" methods: write error packet and return null on failure
 * - "requireSilent" methods: return null without writing error (for
 * broadcasts/background)
 */
public final class Guards {
    private static final Logger logger = LogManager.getLogger(Guards.class);

    private Guards() {
    }

    // ========================================================================
    // User Guards
    // ========================================================================

    /**
     * Get the user from the channel context.
     * Writes error packet and returns null if user not found.
     */
    public static User requireUser(ChannelHandlerContext ctx, int errorCommand) {
        return requireUser(ctx.channel(), errorCommand);
    }

    /**
     * Get the user from the channel.
     * Writes error packet and returns null if user not found.
     */
    public static User requireUser(Channel channel, int errorCommand) {
        User user = ActiveUsers.get(channel);
        if (user != null) {
            return user;
        }

        logger.error("Guard failed: No user found for channel.");
        Packets.write(channel, errorCommand, Error.INVALID_SESSION.getCode());
        return null;
    }

    /**
     * Get the user from the channel context silently.
     * Returns null without writing error packet (for broadcasts/background tasks).
     */
    public static User requireUserSilent(ChannelHandlerContext ctx) {
        return requireUserSilent(ctx.channel());
    }

    /**
     * Get the user from the channel silently.
     * Returns null without writing error packet (for broadcasts/background tasks).
     */
    public static User requireUserSilent(Channel channel) {
        return ActiveUsers.get(channel);
    }

    // ========================================================================
    // Character Guards
    // ========================================================================

    /**
     * Get the current character from the user.
     * Writes error packet and returns null if character not found.
     */
    public static Character requireCharacter(ChannelHandlerContext ctx, User user, int errorCommand) {
        if (user == null) {
            logger.error("Guard failed: User is null, cannot get character.");
            Packets.write(ctx, errorCommand, Error.INVALID_SESSION);
            return null;
        }

        Character character = user.getCurrentCharacter();
        if (character != null) {
            return character;
        }

        logger.error("Guard failed: No current character for user.");
        Packets.write(ctx, errorCommand, Error.INVALID_SESSION);
        return null;
    }

    /**
     * Get the current character from the user silently.
     * Returns null without writing error packet (for broadcasts/background tasks).
     */
    public static Character requireCharacterSilent(User user) {
        if (user == null) {
            return null;
        }
        return user.getCurrentCharacter();
    }

    // ========================================================================
    // Player Guards (character in a game)
    // ========================================================================

    /**
     * Get the player (character in a game) from the character.
     * Writes error packet and returns null if player not found.
     */
    public static Player requirePlayer(ChannelHandlerContext ctx, Character character, int errorCommand) {
        if (character == null) {
            logger.error("Guard failed: Character is null, cannot get player.");
            Packets.write(ctx, errorCommand, Error.INVALID_SESSION);
            return null;
        }

        if (character.getPlayer().isEmpty()) {
            logger.error("Guard failed: Character is not in a game.");
            Packets.write(ctx, errorCommand, Error.INVALID_SESSION);
            return null;
        }

        return character.getPlayer().get(0);
    }

    /**
     * Get the player (character in a game) from the character silently.
     * Returns null without writing error packet (for broadcasts/background tasks).
     */
    public static Player requirePlayerSilent(Character character) {
        if (character == null) {
            return null;
        }

        if (character.getPlayer().isEmpty()) {
            return null;
        }

        return character.getPlayer().get(0);
    }

    // ========================================================================
    // Clan Membership Guards
    // ========================================================================

    /**
     * Get the clan member from the character.
     * Writes error packet and returns null if not a clan member.
     */
    public static ClanMember requireClanMember(ChannelHandlerContext ctx, Character character, int errorCommand) {
        if (character == null) {
            logger.error("Guard failed: Character is null, cannot get clan member.");
            Packets.write(ctx, errorCommand, Error.INVALID_SESSION);
            return null;
        }

        List<ClanMember> members = character.getClanMember();
        if (members != null && !members.isEmpty()) {
            return members.get(0);
        }

        logger.error("Guard failed: Character is not in a clan.");
        Packets.write(ctx, errorCommand, Error.CLAN_NOTAMEMBER);
        return null;
    }

    /**
     * Get the clan member from the character silently.
     * Returns null without writing error packet.
     */
    public static ClanMember requireClanMemberSilent(Character character) {
        if (character == null) {
            return null;
        }

        List<ClanMember> members = character.getClanMember();
        if (members == null || members.isEmpty()) {
            return null;
        }
        return members.get(0);
    }

    // ========================================================================
    // Role/Permission Guards
    // ========================================================================

    /**
     * Check if the user has at least the required role level.
     * Writes error packet and returns false if insufficient permissions.
     */
    public static boolean requireRole(ChannelHandlerContext ctx, User user, int minRole, int errorCommand) {
        if (user == null) {
            logger.error("Guard failed: User is null for role check.");
            Packets.write(ctx, errorCommand, Error.INVALID_SESSION);
            return false;
        }

        if (user.getRole() >= minRole) {
            return true;
        }

        logger.error("Guard failed: Insufficient role. Required: {}, Has: {}", minRole, user.getRole());
        Packets.write(ctx, errorCommand, Error.GENERAL);
        return false;
    }

    /**
     * Check if the user has at least the required role level silently.
     * Returns false without writing error packet.
     */
    public static boolean requireRoleSilent(User user, int minRole) {
        if (user == null) {
            return false;
        }
        return user.getRole() >= minRole;
    }

    // ========================================================================
    // Convenience combo guards
    // ========================================================================

    /**
     * Get user and character in one call.
     * Returns null array if either is missing (error packet written).
     */
    public static Object[] requireUserAndCharacter(ChannelHandlerContext ctx, int errorCommand) {
        User user = requireUser(ctx, errorCommand);
        if (user == null) {
            return null;
        }

        Character character = requireCharacter(ctx, user, errorCommand);
        if (character == null) {
            return null;
        }

        return new Object[] { user, character };
    }

    /**
     * Get user, character, and player in one call.
     * Returns null array if any is missing (error packet written).
     */
    public static Object[] requireUserCharacterPlayer(ChannelHandlerContext ctx, int errorCommand) {
        User user = requireUser(ctx, errorCommand);
        if (user == null) {
            return null;
        }

        Character character = requireCharacter(ctx, user, errorCommand);
        if (character == null) {
            return null;
        }

        Player player = requirePlayer(ctx, character, errorCommand);
        if (player == null) {
            return null;
        }

        return new Object[] { user, character, player };
    }
}
