//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.bang.avatar.client.AvatarService;

/**
 * Defines codes and constants relating to the avatar system.
 */
public interface AvatarCodes
{
    /** The identifier for our message bundle. */
    public static final String AVATAR_MSGS = "avatar";

    /** The identifier for the article message bundle. */
    public static final String ARTICLE_MSGS = "article";

    /** The identifier for the buckle message bundle. */
    public static final String BUCKLE_MSGS = "buckle";

    /** The resource set that contains our avatar imagery. */
    public static final String AVATAR_RSRC_SET = "avatars";

    /** The base scrip cost for a new look. */
    public static final int BASE_LOOK_SCRIP_COST = 800;

    /** The base coin cost for a new look. */
    public static final int BASE_LOOK_COIN_COST = 1;

    /** The maximum cost of an aspect offered when creating a first look. */
    public static final int MAX_STARTER_COST = 100;

    /** An error message used by {@link AvatarService#createAvatar}. */
    public static final String ERR_RESERVED_HANDLE = "m.reserved_handle";

    /** An error message used by {@link AvatarService#createAvatar}. */
    public static final String ERR_VULGAR_HANDLE = "m.vulgar_handle";

    /** An error message used by {@link AvatarService#createAvatar}. */
    public static final String ERR_DUP_HANDLE = "m.duplicate_handle";
}
