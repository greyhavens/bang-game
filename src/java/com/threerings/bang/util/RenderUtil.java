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
import com.jme.scene.state.ZBufferState;
import com.jme.util.TextureManager;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Useful graphics related utility methods.
 */
public class RenderUtil
{
    public static AlphaState iconAlpha;

    public static ZBufferState alwaysZBuf;

    public static ZBufferState lequalZBuf;

    /**
     * Initializes our commonly used render states.
     */
    public static void init (BangContext ctx)
    {
        iconAlpha = ctx.getRenderer().createAlphaState();
        iconAlpha.setBlendEnabled(true);
        iconAlpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        iconAlpha.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
        iconAlpha.setEnabled(true);

        alwaysZBuf = ctx.getRenderer().createZBufferState();
        alwaysZBuf.setEnabled(true);
        alwaysZBuf.setFunction(ZBufferState.CF_ALWAYS);

        lequalZBuf = ctx.getRenderer().createZBufferState();
        lequalZBuf.setEnabled(true);
        lequalZBuf.setFunction(ZBufferState.CF_LEQUAL);
    }

    /**
     * Creates a texture using the supplied image.
     */
    public static TextureState createTexture (
        BangContext ctx, BufferedImage image)
    {
        Texture texture = TextureManager.loadTexture(
            image, Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
        TextureState tstate = ctx.getRenderer().createTextureState();
        tstate.setEnabled(true);
        tstate.setTexture(texture);
        return tstate;
    }

    /**
     * Creates a single tile "icon" image which is a textured quad that
     * covers a tile worth of space.
     */
    public static Quad createIcon (BangContext ctx, String path)
    {
        return createIcon(createTexture(ctx, ctx.loadImage(path)));
    }

    /**
     * Creates a single tile "icon" image which is a textured quad that
     * covers a tile worth of space.
     */
    public static Quad createIcon (TextureState tstate)
    {
        Quad icon = createIcon(TILE_SIZE, TILE_SIZE);
        icon.setLocalTranslation(new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));
        icon.setRenderState(tstate);
        icon.updateRenderState();
        return icon;
    }

    /**
     * Creates an icon with proper alpha state and no lighting of the
     * specified width and height. No translation is done, this is for
     * creating non-tile-sized icons.
     */
    public static Quad createIcon (float width, float height)
    {
        Quad icon = new Quad("icon", width, height);
        icon.setRenderState(iconAlpha);
        icon.setLightCombineMode(LightState.OFF);
        return icon;
    }
}
