//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.data.AuthCodes;

/**
 * Additional auth codes for the Bang! server.
 */
public interface BangAuthCodes extends AuthCodes
{
    /** A message bundle used during client authorization. */
    public static final String AUTH_MSGS = "logon";

    /** A code indicating that the user has been banned from the
     * server. */
    public static final String BANNED = "m.banned";

    /** A code indicating a bounced check or reversed payment. */
    public static final String DEADBEAT = "m.deadbeat";

    /** A code indicating that the user has been temporarily banned from the
     * server. */
    public static final String TEMP_BANNED = "m.temp_banned";

    /** A code indicating that the machine is tainted and no new accounts will
     * be able to logon from it. */
    public static final String MACHINE_TAINTED = "m.machine_tainted";

    /** A code indicating that this machine is reached its limit of free accounts. */
    public static final String NO_NEW_FREE_ACCOUNT = "m.no_new_free_account";

    /** A code indicating that this server is not open to the public. */
    public static final String NON_PUBLIC_SERVER = "m.non_public_server";

    /** A code indicating that the player doesn't have the requisite ticket. */
    public static final String NO_TICKET = "m.no_ticket";

    /** A code indicating that the server is under maintenance and normal user
     * login is not allowed. */
    public static final String UNDER_MAINTENANCE = "m.under_maintenance";

    /** A code indicating that the client version is out of date. */
    public static final String VERSION_MISMATCH = "m.version_mismatch";

    /** A code indicating that the client has a newer version of the code than
     * the server which generally means we're in the middle of updating the
     * game. */
    public static final String NEWER_VERSION = "m.newer_version";

    /** A code indicating that the username already exists. */
    public static final String NAME_IN_USE = "m.name_in_use";

    /** A code indicating that this server is not open to anonymous users. */
    public static final String NO_ANONYMOUS_ACCESS = "m.no_anonymous_access";
}
