//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;

import com.jme.image.Texture;
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
}
