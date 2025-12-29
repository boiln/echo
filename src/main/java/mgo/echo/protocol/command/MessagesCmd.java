package mgo.echo.protocol.command;

/**
 * Command IDs for mail/message operations.
 */
public final class MessagesCmd {
    private MessagesCmd() {
    }

    // ========================================================================
    // Send Messages
    // ========================================================================

    /** Send message request */
    public static final int SEND = 0x4800;
    /** Send message response */
    public static final int SEND_RESPONSE = 0x4801;

    // ========================================================================
    // Get Messages
    // ========================================================================

    /** Get messages request */
    public static final int GET_MESSAGES = 0x4820;
    /** Get messages response start */
    public static final int GET_MESSAGES_START = 0x4821;
    /** Get messages response data */
    public static final int GET_MESSAGES_DATA = 0x4822;
    /** Get messages response end */
    public static final int GET_MESSAGES_END = 0x4823;

    /** Get message contents request */
    public static final int GET_CONTENTS = 0x4840;
    /** Get message contents response */
    public static final int GET_CONTENTS_RESPONSE = 0x4841;

    // ========================================================================
    // Sent Messages
    // ========================================================================

    /** Add sent message request */
    public static final int ADD_SENT = 0x4860;
    /** Add sent message response */
    public static final int ADD_SENT_RESPONSE = 0x4861;
}
