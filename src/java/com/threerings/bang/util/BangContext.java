//
// $Id$

package com.threerings.bang.util;

import com.threerings.parlor.util.ParlorContext;

import com.threerings.cast.CharacterManager;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AspectCatalog;
import com.threerings.bang.avatar.util.AvatarMetrics;
import com.threerings.bang.data.PlayerObject;

/**
 * Defines additional services needed by the Bang! game client.
 */
public interface BangContext extends BasicContext, ParlorContext
{
    /** Returns a reference to the current player's user object. Only
     * valid when we are logged onto the server. */
    public PlayerObject getUserObject ();

    /** Returns a reference to our character manager. */
    public CharacterManager getCharacterManager ();

    /** Returns an object used to encode and decode avatar information. */
    public AvatarMetrics getAvatarMetrics ();

    /** Returns a reference to our avatar aspect catalog. */
    public AspectCatalog getAspectCatalog ();

    /** Returns a reference to our avatar article catalog. */
    public ArticleCatalog getArticleCatalog ();
}
