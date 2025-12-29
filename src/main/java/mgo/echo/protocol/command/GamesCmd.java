package mgo.echo.protocol.command;

/**
 * Command IDs for game operations.
 * Used for game listing, joining, details, etc.
 */
public final class GamesCmd {
    private GamesCmd() {
    }

    // ========================================================================
    // Game List
    // ========================================================================

    /** Get game list request */
    public static final int GET_LIST = 0x4300;
    /** Get game list response start */
    public static final int GET_LIST_START = 0x4301;
    /** Get game list response data */
    public static final int GET_LIST_DATA = 0x4302;
    /** Get game list response end */
    public static final int GET_LIST_END = 0x4303;

    // ========================================================================
    // Game Details
    // ========================================================================

    /** Get game details request */
    public static final int GET_DETAILS = 0x4312;
    /** Get game details response */
    public static final int GET_DETAILS_RESPONSE = 0x4313;

    // ========================================================================
    // Join/Leave
    // ========================================================================

    /** Join game request */
    public static final int JOIN = 0x4320;
    /** Join game response */
    public static final int JOIN_RESPONSE = 0x4321;

    /** Join failed notification */
    public static final int JOIN_FAILED = 0x4322;
    /** Join failed response */
    public static final int JOIN_FAILED_RESPONSE = 0x4323;

    /** Quit game request */
    public static final int QUIT = 0x4380;
    /** Quit game response */
    public static final int QUIT_RESPONSE = 0x4381;
}
