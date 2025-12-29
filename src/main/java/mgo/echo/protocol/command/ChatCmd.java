package mgo.echo.protocol.command;

/**
 * Command IDs for chat operations.
 */
public final class ChatCmd {
    private ChatCmd() {
    }

    // ========================================================================
    // Chat
    // ========================================================================

    /** Send chat message request */
    public static final int SEND = 0x4400;
    /** Chat message response/broadcast */
    public static final int SEND_RESPONSE = 0x4401;

    /** Unknown 4440 request */
    public static final int UNKNOWN_4440 = 0x4440;
    /** Unknown 4441 response */
    public static final int UNKNOWN_4441 = 0x4441;
}
