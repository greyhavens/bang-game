//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;

import com.jme.image.Texture;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

import com.threerings.bang.util.BangContext;

/**
 * Useful graphics related utility methods.
 */
public class RenderUtil
{
    /**
     * Creates a texture using the supplied image.
     */
    public static TextureState createTexture (
        BangContext ctx, BufferedImage image)
    {
        Texture texture = TextureManager.loadTexture(
            image, Texture.MM_LINEAR_LINEAR, Texture.FM_NEAREST, false);
        TextureState tstate =
            ctx.getDisplay().getRenderer().createTextureState();
        tstate.setEnabled(true);
        tstate.setTexture(texture);
        return tstate;
    }

    /**
     * Returns an alpha state that combines in the standard way: source
     * plus destination times one minus source.
     */
    public static AlphaState getIconAlpha (BangContext ctx)
    {
        if (_ialpha == null) {
            _ialpha = ctx.getDisplay().getRenderer().createAlphaState();
            _ialpha.setBlendEnabled(true);
            _ialpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
            _ialpha.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
            _ialpha.setEnabled(true);
        }
        return _ialpha;
    }

    protected static AlphaState _ialpha;
}
