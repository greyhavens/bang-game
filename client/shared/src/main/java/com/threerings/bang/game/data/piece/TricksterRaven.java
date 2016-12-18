//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TricksterRavenSprite;

/**
 * Handles the special capabilities of the Trickster Raven unit.
 */
public class TricksterRaven extends Unit
{
    @Override // documentation inherited
    public boolean isFlyer ()
    {
        return true;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TricksterRavenSprite(_config.type);
    }
}
