//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.scene.Spatial;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Sprite;
import com.threerings.media.image.Colorization;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.BangContext;

/**
 * Displays a ballistic shot.
 */
public class ShotSprite extends Sprite
{
    /**
     * Creates the shot sprite.
     *
     * @param type the model or effect type
     * @param zations the colorizations to apply, or <code>null</code> for none
     */
    public ShotSprite (BangContext ctx, String type, Colorization[] zations)
    {
        if (type.startsWith("effects/")) {
            ctx.loadParticles(type.substring(8), new ResultAttacher<Spatial>(this));
        } else {
            int idx = type.indexOf('/');
            String mtype = type.substring(0, idx), name = type.substring(idx + 1);
            ctx.getModelCache().getModel(mtype, name, zations, new ResultAttacher<Model>(this));
            setLocalScale(0.5f);
        }
    }
}
