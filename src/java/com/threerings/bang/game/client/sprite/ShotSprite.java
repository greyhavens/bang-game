//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.scene.Node;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Sprite;
import com.threerings.media.image.Colorization;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a fired shot.
 */
public class ShotSprite extends Sprite
{
    /**
     * Creates the shot sprite.
     *
     * @param type the model type
     * @param name the model name
     * @param zations the colorizations to apply, or <code>null</code> for none
     */
    public ShotSprite (
        BangContext ctx, String type, String name, Colorization[] zations)
    {
        // our models are centered at the origin, but we need to shift
        // them to the center of the prop's footprint
        ctx.getModelCache().getModel(type, name, zations,
            new ResultAttacher<Model>(this));
        setLocalScale(0.5f);
    }
}
