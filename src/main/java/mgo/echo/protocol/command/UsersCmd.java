package mgo.echo.protocol.command;

/**
 * Command IDs for user/account operations.
 * Used in Account/Gate lobbies for authentication and session management.
 */
public final class UsersCmd {
    private UsersCmd() {
    }

    // ========================================================================
    // Session/Authentication
    // ========================================================================

    /** Check session request */
    public static final int CHECK_SESSION = 0x3003;
    /** Check session response */
    public static final int CHECK_SESSION_RESPONSE = 0x3004;

    /** Disconnect notification */
    public static final int DISCONNECT = 0x2003;

    // ========================================================================
    // Character List/CRUD
    // ========================================================================

    /** Get character list request */
    public static final int GET_CHARACTER_LIST = 0x3048;
    /** Get character list response */
    public static final int GET_CHARACTER_LIST_RESPONSE = 0x3049;

    /** Create character request */
    public static final int CREATE_CHARACTER = 0x3101;
    /** Create character response */
    public static final int CREATE_CHARACTER_RESPONSE = 0x3102;

    /** Select character request */
    public static final int SELECT_CHARACTER = 0x3103;
    /** Select character response */
    public static final int SELECT_CHARACTER_RESPONSE = 0x3104;

    /** Delete character request */
    public static final int DELETE_CHARACTER = 0x3105;
    /** Delete character response */
    public static final int DELETE_CHARACTER_RESPONSE = 0x3106;

    // ========================================================================
    // News/Info
    // ========================================================================

    /** Get news start */
    public static final int GET_NEWS_START = 0x3201;
    /** Get news data */
    public static final int GET_NEWS_DATA = 0x3202;
    /** Get news end */
    public static final int GET_NEWS_END = 0x3203;
}
