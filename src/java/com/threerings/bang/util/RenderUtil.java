//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;

import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

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
            image, Texture.MM_LINEAR, Texture.FM_NEAREST, true);
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

    /**
     * Creates a single tile "icon" image which is a textured quad that
     * covers a tile worth of space.
     */
    public static Quad createIcon (BangContext ctx, String path)
    {
        return createIcon(ctx, createTexture(ctx, ctx.loadImage(path)));
    }

    /**
     * Creates a single tile "icon" image which is a textured quad that
     * covers a tile worth of space.
     */
    public static Quad createIcon (BangContext ctx, TextureState tstate)
    {
        Quad icon = new Quad("icon", TILE_SIZE, TILE_SIZE);
        icon.setLocalTranslation(new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));
        icon.setRenderState(getIconAlpha(ctx));
        icon.setLightCombineMode(LightState.OFF);
        icon.setRenderState(tstate);
        icon.updateRenderState();
        return icon;
    }

    protected static AlphaState _ialpha;
}
