//
// $Id$

package com.threerings.bang.util;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.jme.image.Texture;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.TextureManager;

import com.threerings.util.RandomUtil;

import com.threerings.bang.data.Terrain;
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

    public static ZBufferState overlayZBuf;

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
        alwaysZBuf.setWritable(true);
        alwaysZBuf.setEnabled(true);
        alwaysZBuf.setFunction(ZBufferState.CF_ALWAYS);

        lequalZBuf = ctx.getRenderer().createZBufferState();
        lequalZBuf.setEnabled(true);
        lequalZBuf.setFunction(ZBufferState.CF_LEQUAL);

        overlayZBuf = ctx.getRenderer().createZBufferState();
        overlayZBuf.setEnabled(true);
        overlayZBuf.setWritable(false);
        overlayZBuf.setFunction(ZBufferState.CF_LEQUAL);

        ClassLoader loader = ctx.getClass().getClassLoader();
        for (Terrain terrain : Terrain.STARTERS) {
            for (int ii = 1; ii <= 3; ii++) {
                URL texpath = loader.getResource(
                    "rsrc/media/textures/ground/" +
                    terrain.toString().toLowerCase() + ii + ".png");
                if (texpath == null) {
                    continue;
                }
                Texture texture = TextureManager.loadTexture(
                    texpath, Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR);
                TextureState tstate = ctx.getRenderer().createTextureState();
                tstate.setEnabled(true);
                tstate.setTexture(texture);
                ArrayList<TextureState> texs = _groundTexs.get(terrain);
                if (texs == null) {
                    _groundTexs.put(
                        terrain, texs = new ArrayList<TextureState>());
                }
                texs.add(tstate);
            }
        }
    }

    /**
     * Returns a randomly selected ground texture for the specified
     * terrain type.
     */
    public static TextureState getGroundTexture (Terrain terrain)
    {
        return (TextureState)RandomUtil.pickRandom(_groundTexs.get(terrain));
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
        icon.setLocalTranslation(new Vector3f(0, 0, 0.1f));
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
        icon.setRenderState(overlayZBuf);
        icon.setLightCombineMode(LightState.OFF);
        return icon;
    }

    public static LightState createDaylight (Renderer renderer)
    {
        PointLight light = new PointLight();
        light.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        // light.setAmbient(new ColorRGBA(0.75f, 0.75f, 0.75f, 1.0f));
        light.setAmbient(new ColorRGBA(0.25f, 0.25f, 0.55f, 1.0f));
        light.setLocation(new Vector3f(100, 100, 100));
        light.setAttenuate(true);
        light.setConstant(0.25f);
        light.setEnabled(true);

        LightState lights = renderer.createLightState();
        lights.setEnabled(true);
        lights.attach(light);
        return lights;
    }

    protected static HashMap<Terrain,ArrayList<TextureState>> _groundTexs =
        new HashMap<Terrain,ArrayList<TextureState>>();
}
