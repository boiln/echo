package mgo.echo.protocol.command;

/**
 * Command IDs for hub/lobby operations.
 */
public final class HubCmd {
    private HubCmd() {
    }

    // ========================================================================
    // Lobby Connection
    // ========================================================================

    /** Lobby disconnect request */
    public static final int LOBBY_DISCONNECT = 0x4150;
    /** Lobby disconnect response */
    public static final int LOBBY_DISCONNECT_RESPONSE = 0x4151;

    /** Training connect request */
    public static final int TRAINING_CONNECT = 0x43d0;
    /** Training connect response */
    public static final int TRAINING_CONNECT_RESPONSE = 0x43d1;

    // ========================================================================
    // Lobby Info
    // ========================================================================

    /** Get game lobby info request */
    public static final int GET_GAME_LOBBY_INFO = 0x4900;
    /** Get game lobby info response */
    public static final int GET_GAME_LOBBY_INFO_RESPONSE = 0x4901;

    /** Get game entry info request */
    public static final int GET_GAME_ENTRY_INFO = 0x4990;
    /** Get game entry info response */
    public static final int GET_GAME_ENTRY_INFO_RESPONSE = 0x4991;

    // ========================================================================
    // Unknown/Legacy
    // ========================================================================

    /** Unknown 2005 response */
    public static final int UNKNOWN_2005 = 0x2005;
}
