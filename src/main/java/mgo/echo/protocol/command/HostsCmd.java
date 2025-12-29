package mgo.echo.protocol.command;

/**
 * Command IDs for host operations.
 * Used for game hosting, settings, player management, stats updates.
 */
public final class HostsCmd {
    private HostsCmd() {
    }

    // ========================================================================
    // Host Settings
    // ========================================================================

    /** Get host settings request */
    public static final int GET_SETTINGS = 0x4304;
    /** Get host settings response */
    public static final int GET_SETTINGS_RESPONSE = 0x4305;

    /** Check/update host settings request */
    public static final int CHECK_SETTINGS = 0x4310;
    /** Check settings response */
    public static final int CHECK_SETTINGS_RESPONSE = 0x4311;

    // ========================================================================
    // Game Creation
    // ========================================================================

    /** Create game request */
    public static final int CREATE_GAME = 0x4316;
    /** Create game response */
    public static final int CREATE_GAME_RESPONSE = 0x4317;

    // ========================================================================
    // Player Management
    // ========================================================================

    /** Player connected notification */
    public static final int PLAYER_CONNECTED = 0x4340;
    /** Player connected response */
    public static final int PLAYER_CONNECTED_RESPONSE = 0x4341;

    /** Player disconnected notification */
    public static final int PLAYER_DISCONNECTED = 0x4342;
    /** Player disconnected response */
    public static final int PLAYER_DISCONNECTED_RESPONSE = 0x4343;

    /** Set player team request */
    public static final int SET_PLAYER_TEAM = 0x4344;
    /** Set player team response */
    public static final int SET_PLAYER_TEAM_RESPONSE = 0x4345;

    /** Kick player request */
    public static final int KICK_PLAYER = 0x4346;
    /** Kick player response */
    public static final int KICK_PLAYER_RESPONSE = 0x4347;

    // ========================================================================
    // Game State
    // ========================================================================

    /** Update stats request */
    public static final int UPDATE_STATS = 0x4390;
    /** Update stats response */
    public static final int UPDATE_STATS_RESPONSE = 0x4391;

    /** Set current game request */
    public static final int SET_GAME = 0x4392;
    /** Set current game response */
    public static final int SET_GAME_RESPONSE = 0x4393;

    /** Update game environment request */
    public static final int UPDATE_GAME_ENVIRONMENT = 0x4394;
    /** Update game environment response */
    public static final int UPDATE_GAME_ENVIRONMENT_RESPONSE = 0x4395;

    /** Update pings request */
    public static final int UPDATE_PINGS = 0x4398;
    /** Update pings response */
    public static final int UPDATE_PINGS_RESPONSE = 0x4399;

    // ========================================================================
    // Host Actions
    // ========================================================================

    /** Pass host request */
    public static final int PASS = 0x43a0;
    /** Pass host response */
    public static final int PASS_RESPONSE = 0x43a1;

    /** Unknown 43a2 (acknowledged) */
    public static final int UNKNOWN_43A2 = 0x43a2;
    /** Unknown 43a3 response */
    public static final int UNKNOWN_43A3 = 0x43a3;

    /** Unknown 43c0 (ping related) */
    public static final int UNKNOWN_43C0 = 0x43c0;
    /** Unknown 43c1 response */
    public static final int UNKNOWN_43C1 = 0x43c1;

    /** Start round request */
    public static final int START_ROUND = 0x43ca;
    /** Start round response */
    public static final int START_ROUND_RESPONSE = 0x43cb;
}
