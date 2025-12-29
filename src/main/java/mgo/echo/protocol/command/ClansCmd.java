package mgo.echo.protocol.command;

/**
 * Command IDs for clan operations.
 */
public final class ClansCmd {
    private ClansCmd() {
    }

    // ========================================================================
    // Clan Management
    // ========================================================================

    /** Create clan request */
    public static final int CREATE = 0x4b00;
    /** Create clan response */
    public static final int CREATE_RESPONSE = 0x4b01;

    /** Disband clan request */
    public static final int DISBAND = 0x4b04;
    /** Disband clan response */
    public static final int DISBAND_RESPONSE = 0x4b05;

    // ========================================================================
    // Clan List/Info
    // ========================================================================

    /** Get clan list request */
    public static final int GET_LIST = 0x4b10;
    /** Get clan list response start */
    public static final int GET_LIST_START = 0x4b11;
    /** Get clan list response data */
    public static final int GET_LIST_DATA = 0x4b12;
    /** Get clan list response end */
    public static final int GET_LIST_END = 0x4b13;

    /** Get clan information (member view) request */
    public static final int GET_INFORMATION_MEMBER = 0x4b20;
    /** Get clan information (member view) response */
    public static final int GET_INFORMATION_MEMBER_RESPONSE = 0x4b21;

    // ========================================================================
    // Membership
    // ========================================================================

    /** Accept join request */
    public static final int ACCEPT_JOIN = 0x4b30;
    /** Accept join response */
    public static final int ACCEPT_JOIN_RESPONSE = 0x4b31;

    /** Decline join request */
    public static final int DECLINE_JOIN = 0x4b32;
    /** Decline join response */
    public static final int DECLINE_JOIN_RESPONSE = 0x4b33;

    /** Banish member request */
    public static final int BANISH = 0x4b36;
    /** Banish member response */
    public static final int BANISH_RESPONSE = 0x4b37;

    /** Leave clan request */
    public static final int LEAVE = 0x4b40;
    /** Leave clan response */
    public static final int LEAVE_RESPONSE = 0x4b41;

    /** Apply to clan request */
    public static final int APPLY = 0x4b42;
    /** Apply to clan response */
    public static final int APPLY_RESPONSE = 0x4b43;

    /** Update member state request */
    public static final int UPDATE_STATE = 0x4b46;
    /** Update member state response */
    public static final int UPDATE_STATE_RESPONSE = 0x4b47;

    // ========================================================================
    // Emblem
    // ========================================================================

    /** Get emblem request (variant 1) */
    public static final int GET_EMBLEM_1 = 0x4b48;
    /** Get emblem response (variant 1) */
    public static final int GET_EMBLEM_1_RESPONSE = 0x4b49;

    /** Get emblem request (variant 2) */
    public static final int GET_EMBLEM_2 = 0x4b4a;
    /** Get emblem response (variant 2) */
    public static final int GET_EMBLEM_2_RESPONSE = 0x4b4b;

    /** Get emblem request (variant 3) */
    public static final int GET_EMBLEM_3 = 0x4b4c;
    /** Get emblem response (variant 3) */
    public static final int GET_EMBLEM_3_RESPONSE = 0x4b4d;

    /** Set emblem request */
    public static final int SET_EMBLEM = 0x4b50;
    /** Set emblem response */
    public static final int SET_EMBLEM_RESPONSE = 0x4b51;

    // ========================================================================
    // Roster/Leadership
    // ========================================================================

    /** Get roster request */
    public static final int GET_ROSTER = 0x4b52;
    /** Get roster response start */
    public static final int GET_ROSTER_START = 0x4b53;
    /** Get roster response data */
    public static final int GET_ROSTER_DATA = 0x4b54;
    /** Get roster response end */
    public static final int GET_ROSTER_END = 0x4b55;

    /** Transfer leadership request */
    public static final int TRANSFER_LEADERSHIP = 0x4b60;
    /** Transfer leadership response */
    public static final int TRANSFER_LEADERSHIP_RESPONSE = 0x4b61;

    /** Set emblem editor request */
    public static final int SET_EMBLEM_EDITOR = 0x4b62;
    /** Set emblem editor response */
    public static final int SET_EMBLEM_EDITOR_RESPONSE = 0x4b63;

    /** Update comment request */
    public static final int UPDATE_COMMENT = 0x4b64;
    /** Update comment response */
    public static final int UPDATE_COMMENT_RESPONSE = 0x4b65;

    /** Update notice request */
    public static final int UPDATE_NOTICE = 0x4b66;
    /** Update notice response */
    public static final int UPDATE_NOTICE_RESPONSE = 0x4b67;

    // ========================================================================
    // Stats/Info/Search
    // ========================================================================

    /** Get clan stats request */
    public static final int GET_STATS = 0x4b70;
    /** Get clan stats response */
    public static final int GET_STATS_RESPONSE = 0x4b71;

    /** Get clan information request */
    public static final int GET_INFORMATION = 0x4b80;
    /** Get clan information response */
    public static final int GET_INFORMATION_RESPONSE = 0x4b81;

    /** Search clans request */
    public static final int SEARCH = 0x4b90;
    /** Search clans response start */
    public static final int SEARCH_START = 0x4b91;
    /** Search clans response data */
    public static final int SEARCH_DATA = 0x4b92;
    /** Search clans response end */
    public static final int SEARCH_END = 0x4b93;
}
