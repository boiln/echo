package mgo.echo.protocol.command;

/**
 * Command IDs for character operations.
 * Used for character info, stats, gear, skills, friends, etc.
 */
public final class CharactersCmd {
    private CharactersCmd() {
    }

    // ========================================================================
    // Character Info
    // ========================================================================

    /** Get character info request */
    public static final int GET_CHARACTER_INFO = 0x4100;
    /** Get character info response */
    public static final int GET_CHARACTER_INFO_RESPONSE = 0x4101;

    // ========================================================================
    // Settings/Options
    // ========================================================================

    /** Update gameplay options/chat macros response */
    public static final int UPDATE_SETTINGS_RESPONSE = 0x4111;

    /** Update UI settings response */
    public static final int UPDATE_UI_SETTINGS_RESPONSE = 0x4113;

    /** Get gameplay options/UI settings response */
    public static final int GET_GAMEPLAY_OPTIONS_UI_RESPONSE = 0x4120;

    /** Get chat macros response */
    public static final int GET_CHAT_MACROS_RESPONSE = 0x4121;

    /** Get personal info response */
    public static final int GET_PERSONAL_INFO_RESPONSE = 0x4122;

    /** Get gear response */
    public static final int GET_GEAR_RESPONSE = 0x4124;

    /** Get skills response */
    public static final int GET_SKILLS_RESPONSE = 0x4125;

    // ========================================================================
    // Post-Game/Personal Info
    // ========================================================================

    /** Get post-game info response */
    public static final int GET_POST_GAME_INFO_RESPONSE = 0x4129;

    /** Update personal info response */
    public static final int UPDATE_PERSONAL_INFO_RESPONSE = 0x4131;

    // ========================================================================
    // Skills/Gear Sets
    // ========================================================================

    /** Get skill sets response */
    public static final int GET_SKILL_SETS_RESPONSE = 0x4140;

    /** Update skill sets response */
    public static final int UPDATE_SKILL_SETS_RESPONSE = 0x4141;

    /** Get gear sets response */
    public static final int GET_GEAR_SETS_RESPONSE = 0x4142;

    /** Update gear sets response */
    public static final int UPDATE_GEAR_SETS_RESPONSE = 0x4143;

    // ========================================================================
    // Friends/Blocked
    // ========================================================================

    /** Add friend/blocked response */
    public static final int ADD_FRIENDS_BLOCKED_RESPONSE = 0x4501;
    /** Add friend/blocked response data */
    public static final int ADD_FRIENDS_BLOCKED_DATA = 0x4502;

    /** Remove friend/blocked response */
    public static final int REMOVE_FRIENDS_BLOCKED_RESPONSE = 0x4511;
    /** Remove friend/blocked response data */
    public static final int REMOVE_FRIENDS_BLOCKED_DATA = 0x4512;

    /** Get friends/blocked list response start */
    public static final int GET_FRIENDS_BLOCKED_LIST_START = 0x4581;
    /** Get friends/blocked list response data */
    public static final int GET_FRIENDS_BLOCKED_LIST_DATA = 0x4582;
    /** Get friends/blocked list response end */
    public static final int GET_FRIENDS_BLOCKED_LIST_END = 0x4583;

    // ========================================================================
    // Search
    // ========================================================================

    /** Search player response start */
    public static final int SEARCH_RESPONSE_START = 0x4601;
    /** Search player response data */
    public static final int SEARCH_RESPONSE_DATA = 0x4602;
    /** Search player response end */
    public static final int SEARCH_RESPONSE_END = 0x4603;

    // ========================================================================
    // Match History / Personal Stats
    // ========================================================================

    /** Get personal stats response - character info header */
    public static final int GET_PERSONAL_STATS_HEADER = 0x4103;
    /** Get personal stats response - mode stats */
    public static final int GET_PERSONAL_STATS_MODE = 0x4105;
    /** Get personal stats response - additional stats */
    public static final int GET_PERSONAL_STATS_ADDITIONAL = 0x4107;

    /** Get match history response start */
    public static final int GET_MATCH_HISTORY_START = 0x4681;
    /** Get match history response data */
    public static final int GET_MATCH_HISTORY_DATA = 0x4682;
    /** Get match history response end */
    public static final int GET_MATCH_HISTORY_END = 0x4683;

    /** Get personal stats response start */
    public static final int GET_PERSONAL_STATS_START = 0x4691;
    /** Get personal stats response data */
    public static final int GET_PERSONAL_STATS_DATA = 0x4692;
    /** Get personal stats response end */
    public static final int GET_PERSONAL_STATS_END = 0x4693;

    // ========================================================================
    // Connection Info
    // ========================================================================

    /** Update connection info response */
    public static final int UPDATE_CONNECTION_INFO_RESPONSE = 0x4701;

    // ========================================================================
    // Character Card
    // ========================================================================

    /** Get character card response */
    public static final int GET_CHARACTER_CARD_RESPONSE = 0x4221;

    // ========================================================================
    // Official Game History
    // ========================================================================

    /** Get official game history response start */
    public static final int GET_OFFICIAL_GAME_HISTORY_START = 0x4685;
    /** Get official game history response end */
    public static final int GET_OFFICIAL_GAME_HISTORY_END = 0x4687;
}
