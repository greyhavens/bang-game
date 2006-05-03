//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.scene.Node;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.client.util.ModelAttacher;
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
     */
    public ShotSprite (BangContext ctx, String type, String name)
    {
        // our models are centered at the origin, but we need to shift
        // them to the center of the prop's footprint
        ctx.loadModel(type, name, new ModelAttacher(this));
        setLocalScale(0.5f);
    }
}
