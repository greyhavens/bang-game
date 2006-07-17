//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.ThunderbirdSprite;

/**
 * Handles the special capabilities of the Thunderbird unit.
 */
public class Thunderbird extends Unit
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new ThunderbirdSprite(_config.type);
    }
}
