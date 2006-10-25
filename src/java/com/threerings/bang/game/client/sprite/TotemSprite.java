//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Color;

import com.threerings.bang.game.data.effect.TotemEffect;

import com.threerings.media.image.Colorization;

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
