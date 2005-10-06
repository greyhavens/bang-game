//
// $Id$

package com.threerings.bang.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.jme.image.Texture;
import com.jme.light.PointLight;
import com.jme.math.Vector2f;
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

import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Useful graphics related utility methods.
 */
public class RenderUtil
{
    public static AlphaState blendAlpha;

    public static ZBufferState alwaysZBuf;

    public static ZBufferState lequalZBuf;

    public static ZBufferState overlayZBuf;

    /**
     * Initializes our commonly used render states.
     */
    public static void init (BasicContext ctx)
    {
        blendAlpha = ctx.getRenderer().createAlphaState();
        blendAlpha.setBlendEnabled(true);
        blendAlpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        blendAlpha.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
        blendAlpha.setEnabled(true);

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
        for (Terrain terrain : Terrain.RENDERABLE) {
            for (int ii = 1; ii <= MAX_TILE_VARIANT; ii++) {
                String path = "tiles/ground/" +
                    terrain.toString().toLowerCase() + ii + ".png";
                URL texpath = loader.getResource("rsrc/" + path);
                if (texpath == null) {
                    continue;
                }
                BufferedImage teximg = ctx.loadImage(path);
                Texture texture = TextureManager.loadTexture(
                    teximg, Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
                TextureState tstate = ctx.getRenderer().createTextureState();
                tstate.setEnabled(true);
                tstate.setTexture(texture);
                ArrayList<TextureState> texs = _groundTexs.get(terrain);
                if (texs == null) {
                    _groundTexs.put(
                        terrain, texs = new ArrayList<TextureState>());
                }
                texs.add(tstate);
                ArrayList<BufferedImage> tiles = _groundTiles.get(terrain);
                if (tiles == null) {
                    _groundTiles.put(
                        terrain, tiles = new ArrayList<BufferedImage>());
                }
                tiles.add(teximg);
            }
        }

        // put in a special blank tile image for the rim
        ArrayList<BufferedImage> tiles = new ArrayList<BufferedImage>();
        tiles.add(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
        _groundTiles.put(Terrain.RIM, tiles);
    }

    /** Rounds the supplied value up to a power of two. */
    public static int nextPOT (int value)
    {
        return (Integer.bitCount(value) > 1) ?
            (Integer.highestOneBit(value) << 1) : value;
    }

    /**
     * Returns a randomly selected ground texture for the specified
     * terrain type.
     */
    public static TextureState getGroundTexture (Terrain terrain)
    {
        ArrayList<TextureState> texs = _groundTexs.get(terrain);
        return (texs == null) ? null : 
            (TextureState)RandomUtil.pickRandom(texs);
    }

    /**
     * Returns a randomly selected ground tile for the specified terrain
     * type.
     */
    public static BufferedImage getGroundTile (Terrain terrain)
    {
        ArrayList<BufferedImage> tiles = _groundTiles.get(terrain);
        return (tiles == null) ? null : 
            (BufferedImage)RandomUtil.pickRandom(tiles);
    }

    /**
     * Renders the specified text into an image (which will be sized
     * appropriately for the text) and creates a texture from it.
     *
     * @param tcoords should be a four element array which will be filled
     * in with the appropriate texture coordinates to only display the
     * text.
     */
    public static Texture createTextTexture (
        Font font, ColorRGBA color, String text, Vector2f[] tcoords)
    {
        Graphics2D gfx = _scratch.createGraphics();
        Color acolor = new Color(color.r, color.g, color.b, color.a);
        TextLayout layout;
        try {
            gfx.setFont(font);
            gfx.setColor(acolor);
            layout = new TextLayout(text, font, gfx.getFontRenderContext());
        } finally {
            gfx.dispose();
        }

        // determine the size of our rendered text
        // TODO: do the Mac hack to get the real bounds
        Rectangle2D bounds = layout.getBounds();
        int width = (int)(Math.max(bounds.getX(), 0) + bounds.getWidth());
        int height = (int)(layout.getLeading() + layout.getAscent() +
                           layout.getDescent());

        // now determine the size of our texture image which must be
        // square and a power of two (yay!)
        int tsize = nextPOT(Math.max(width, Math.max(height, 1)));

        // render the text into the image
        BufferedImage image = new BufferedImage(
            tsize, tsize, BufferedImage.TYPE_4BYTE_ABGR);
        gfx = image.createGraphics();
        try {
            gfx.setColor(BLANK);
            gfx.fillRect(0, 0, tsize, tsize);
            gfx.setColor(acolor);
            layout.draw(gfx, -(float)bounds.getX(), layout.getAscent());
        } finally {
            gfx.dispose();
        }

        // fill in the texture coordinates
        float tsf = tsize;
        tcoords[0] = new Vector2f(0, 0);
        tcoords[1] = new Vector2f(0, height/tsf);
        tcoords[2] = new Vector2f(width/tsf, height/tsf);
        tcoords[3] = new Vector2f(width/tsf, 0);

        return TextureManager.loadTexture(
            image, Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, false);
    }

    /**
     * Creates a texture using the supplied image.
     */
    public static TextureState createTexture (
        BasicContext ctx, BufferedImage image)
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
    public static Quad createIcon (BasicContext ctx, String path)
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
        icon.setRenderState(blendAlpha);
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

    protected static HashMap<Terrain,ArrayList<BufferedImage>> _groundTiles =
        new HashMap<Terrain,ArrayList<BufferedImage>>();

    protected static HashMap<Terrain,ArrayList<TextureState>> _groundTexs =
        new HashMap<Terrain,ArrayList<TextureState>>();

    /** The maximum number of different variations we might have for a
     * particular ground tile. */
    protected static final int MAX_TILE_VARIANT = 4;

    /** Used to obtain a graphics context for measuring text before we
     * create the real image. */
    protected static BufferedImage _scratch =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /** Used to fill an image with transparency. */
    protected static Color BLANK = new Color(1.0f, 1.0f, 1.0f, 0f);
}
