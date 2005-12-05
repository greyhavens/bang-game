//
// $Id$

package com.threerings.bang.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.light.DirectionalLight;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.TextureManager;
import com.jme.util.geom.BufferUtils;

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

    public static CullState backCull;

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

        backCull = ctx.getRenderer().createCullState();
        backCull.setCullMode(CullState.CS_BACK);

        ClassLoader loader = ctx.getClass().getClassLoader();
        for (Terrain terrain : Terrain.RENDERABLE) {
            for (int ii = 1; ii <= MAX_TILE_VARIANT; ii++) {
                String path = "tiles/ground/" +
                    terrain.toString().toLowerCase() + ii + ".png";
                URL texpath = loader.getResource("rsrc/" + path);
                if (texpath == null) {
                    continue;
                }
                Image teximg = ctx.loadImage(path);
                Texture texture = createTexture(teximg);
                texture.setWrap(Texture.WM_WRAP_S_WRAP_T);
                TextureState tstate = ctx.getRenderer().createTextureState();
                tstate.setEnabled(true);
                tstate.setTexture(texture);
                ArrayList<TextureState> texs = _groundTexs.get(terrain);
                if (texs == null) {
                    _groundTexs.put(
                        terrain, texs = new ArrayList<TextureState>());
                }
                texs.add(tstate);
                ArrayList<Image> tiles = _groundTiles.get(terrain);
                if (tiles == null) {
                    _groundTiles.put(terrain, tiles = new ArrayList<Image>());
                }
                tiles.add(teximg);
            }
        }
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
    public static Image getGroundTile (Terrain terrain)
    {
        ArrayList<Image> tiles = _groundTiles.get(terrain);
        return (tiles == null) ? null : (Image)RandomUtil.pickRandom(tiles);
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

        // TODO: use our faster BufferedImage -> Image routines
        return TextureManager.loadTexture(
            image, Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, false);
    }

    /**
     * Creates a texture using the supplied image.
     */
    public static Texture createTexture (Image image)
    {
        Texture texture = new Texture();
        texture.setCorrection(Texture.CM_PERSPECTIVE);
        texture.setFilter(Texture.FM_LINEAR);
        texture.setMipmapState(Texture.MM_LINEAR_LINEAR);
        texture.setImage(image);
        return texture;
    }

    /**
     * Creates a texture state using the supplied image.
     */
    public static TextureState createTextureState (BasicContext ctx, Image image)
    {
        Texture texture = createTexture(image);
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
        return createIcon(createTextureState(ctx, ctx.loadImage(path)));
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

    /**
     * Creates texture coordinates that divide a square up into a grid of the
     * specified number of units (ie. 3 gives us a 3x3 grid). The coordinates
     * will be in row major order, moving along y = 0 for each value of x, then
     * y = 1 and so on. <em>Note:</em> the "top" row is considered zero y, not
     * the bottom which would be OpenGL's opinion.
     */
    public static FloatBuffer[] createGridTexCoords (int size)
    {
        FloatBuffer[] fbuf = new FloatBuffer[size*size];
        Vector2f[] tcoords = new Vector2f[4];
        for (int ii = 0; ii < tcoords.length; ii++) {
            tcoords[ii] = new Vector2f();
        }
        float fsize = (float)size;
        int idx = 0;
        for (int yy = size-1; yy >= 0; yy--) {
            for (int xx = 0; xx < size; xx++) {
                tcoords[1].x = xx/fsize;
                tcoords[1].y = yy/fsize;
                tcoords[0].x = xx/fsize;
                tcoords[0].y = (yy+1)/fsize;
                tcoords[3].x = (xx+1)/fsize;
                tcoords[3].y = (yy+1)/fsize;
                tcoords[2].x = (xx+1)/fsize;
                tcoords[2].y = yy/fsize;
                fbuf[idx++] = BufferUtils.createFloatBuffer(tcoords);
            }
        }
        return fbuf;
    }

    protected static HashMap<Terrain,ArrayList<Image>> _groundTiles =
        new HashMap<Terrain,ArrayList<Image>>();

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
