//
// $Id$

package com.threerings.bang.game.client.sprite;

/**
 * Displays a totem bonus.
 */
public class TotemSprite extends BonusSprite
{
    public TotemSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    public Coloring getColoringType ()
    {
        return Coloring.STATIC;
    }
}
