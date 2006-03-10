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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.light.DirectionalLight;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.TextureManager;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.util.Dimension;

import com.threerings.jme.JmeCanvasApp;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Useful graphics related utility methods.
 */
public class RenderUtil
{
    public static AlphaState blendAlpha;

    public static AlphaState addAlpha;

    public static ZBufferState alwaysZBuf;

    public static ZBufferState lequalZBuf;

    public static ZBufferState overlayZBuf;

    public static CullState backCull;
    
    public static CullState frontCull;

    /**
     * Initializes our commonly used render states.
     */
    public static void init (BasicContext ctx)
    {
        addAlpha = ctx.getRenderer().createAlphaState();
        addAlpha.setBlendEnabled(true);
        addAlpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        addAlpha.setDstFunction(AlphaState.DB_ONE);
        addAlpha.setEnabled(true);

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

        frontCull = ctx.getRenderer().createCullState();
        frontCull.setCullMode(CullState.CS_FRONT);
        
        ClassLoader loader = ctx.getClass().getClassLoader();
        for (Terrain terrain : Terrain.RENDERABLE) {
            for (int ii = 1; ii <= MAX_TILE_VARIANT; ii++) {
                String path = "tiles/ground/" +
                    terrain.toString().toLowerCase() + ii + ".png";
                URL texpath = loader.getResource("rsrc/" + path);
                if (texpath == null) {
                    continue;
                }
                Image teximg = ctx.getImageCache().getImage(path);
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
     * Returns the average color of the given terrain type.
     */
    public static ColorRGBA getGroundColor (Terrain terrain)
    {
        ColorRGBA color = _groundColors.get(terrain);
        if (color != null) {
            return color;
        }
        // if we haven't computed it already, determine the overall color
        // average for the texture
        Image img = getGroundTile(terrain);
        ByteBuffer imgdata = img.getData();
        int r = 0, g = 0, b = 0, bytes = imgdata.limit();
        float divisor = 255f * (bytes / 3);
        for (int ii = 0; ii < bytes; ii += 3) {
            // the bytes are stored unsigned in the image but java is going
            // to interpret them as signed, so we need to do some fiddling
            r += btoi(imgdata.get(ii));
            g += btoi(imgdata.get(ii+1));
            b += btoi(imgdata.get(ii+2));
        }
        color = new ColorRGBA(r / divisor, g / divisor, b / divisor, 1f);
        _groundColors.put(terrain, color);
        return color;
    }

    /**
     * Creates a {@link Quad} with a texture configured to display the supplied
     * text. The text will be white, but its color may be set with {@link
     * Quad#setDefaultColor}.
     */
    public static Quad createTextQuad (BasicContext ctx, Font font, String text)
    {
        Vector2f[] tcoords = new Vector2f[4];
        Dimension size = new Dimension();
        TextureState tstate = ctx.getRenderer().createTextureState();
        Texture tex = createTextTexture(
            ctx, font, ColorRGBA.white, text, tcoords, size);
        tstate.setTexture(tex);

        Quad quad = new Quad("text", size.width, size.height);
        tstate.setEnabled(true);
        quad.setRenderState(tstate);

        quad.setTextureBuffer(BufferUtils.createFloatBuffer(tcoords));
        quad.setRenderState(blendAlpha);
        quad.updateRenderState();

        return quad;
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
        BasicContext ctx, Font font, ColorRGBA color, String text,
        Vector2f[] tcoords, Dimension size)
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

        if (size != null) {
            size.width = width;
            size.height = height;
        }

        // now determine the size of our texture image which must be
        // square and a power of two (yay!)
        int tsize = nextPOT(Math.max(width, Math.max(height, 1)));

        // render the text into the image
        BufferedImage image = ctx.getImageCache().createCompatibleImage(
            tsize, tsize, true);
        gfx = image.createGraphics();
        try {
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

        return createTexture(ctx.getImageCache().convertImage(image));
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
     * Creates a texture state using the image with the supplied path. The
     * texture is loaded via the texture cache.
     */
    public static TextureState createTextureState (
        BasicContext ctx, String path)
    {
        return createTextureState(ctx, ctx.getTextureCache().getTexture(path));
    }

    /**
     * Creates a texture state using the supplied texture.
     */
    public static TextureState createTextureState (
        BasicContext ctx, Texture texture)
    {
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
        return createIcon(createTextureState(ctx, path));
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
     * Configures the specified texture's scale and translation so as to select
     * the <code>tile</code>th tile from a <code>size</code> x
     * <code>size</code> grid.
     */
    public static void setTextureTile (Texture texture, int size, int tile)
    {
        texture.setScale(new Vector3f(1/(float)size, 1/(float)size, 0));
        int y = size - tile / size - 1, x = tile % size;
        texture.setTranslation(new Vector3f(x/(float)size, y/(float)size, 0));
    }

    /**
     * Creates the shadow texture for the specified light parameters.
     */
    public static TextureState createShadowTexture (
        BasicContext ctx, float length, float rotation, float intensity)
    {
        // reuse our existing texture if possible; if not, release old
        // texture
        if (_shadtex != null && _slength == length &&
            _srotation == rotation && _sintensity == intensity) {
            return _shadtex;
            
        } else if (_shadtex != null) {
            _shadtex.deleteAll();
        }

        _slength = length;
        _srotation = rotation;
        _sintensity = intensity;

        float yscale = length / TILE_SIZE;
        int size = SHADOW_TEXTURE_SIZE, hsize = size / 2;
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * size * 4);
        byte[] pixel = new byte[] { 0, 0, 0, 0 };
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float xd = (float)(x - hsize) / hsize,
                    yd = yscale * (y - hsize) / hsize,
                    d = FastMath.sqrt(xd*xd + yd*yd),
                    val = d < 0.25f ? intensity : intensity *
                        Math.max(0f, 1.333f - 1.333f*d);
                pixel[3] = (byte)(val * 255);
                pbuf.put(pixel);
            }
        }
        pbuf.rewind();

        // we must rotate the shadow into place and translate to recenter
        Texture stex = new Texture();
        stex.setImage(new Image(Image.RGBA8888, size, size, pbuf));
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(-rotation, Vector3f.UNIT_Z);
        stex.setRotation(rot);
        Vector3f trans = new Vector3f(0.5f, 0.5f, 0f);
        rot.multLocal(trans);
        stex.setTranslation(new Vector3f(0.5f - trans.x, 0.5f - trans.y, 0f));

        _shadtex = ctx.getRenderer().createTextureState();
        _shadtex.setTexture(stex);
        return _shadtex;
    }

    /**
     * Creates a JME {@link ColorRGBA} object with alpha equal to one from a
     * packed RGB value.
     */
    public static ColorRGBA createColorRGBA (int rgb)
    {
        return new ColorRGBA(((rgb >> 16) & 0xFF) / 255f,
                             ((rgb >> 8) & 0xFF) / 255f,
                             (rgb & 0xFF) / 255f, 1f);
    }

    /**
     * Wraps the given material state inside a new state that enables or
     * disables OpenGL color materials.
     */
    public static MaterialState createColorMaterialState (
        final MaterialState mstate, final boolean enableColorMaterial)
    {
        return new MaterialState() {
            public void apply () {
                mstate.apply();
                if (enableColorMaterial) {
                    GL11.glEnable(GL11.GL_COLOR_MATERIAL);
                    GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE);

                } else {
                    GL11.glDisable(GL11.GL_COLOR_MATERIAL);
                }
            }
        };
    }

    protected static final int btoi (byte value)
    {
        return (value < 0) ? 256 + value : value;
    }

    protected static HashMap<Terrain,ArrayList<Image>> _groundTiles =
        new HashMap<Terrain,ArrayList<Image>>();

    protected static HashMap<Terrain,ArrayList<TextureState>> _groundTexs =
        new HashMap<Terrain,ArrayList<TextureState>>();

    protected static HashMap<Terrain,ColorRGBA> _groundColors =
        new HashMap<Terrain,ColorRGBA>();

    /** Our most recently created shadow texture. */
    protected static TextureState _shadtex;

    /** The parameters used to create our shadow texture. */
    protected static float _slength, _srotation, _sintensity;

    /** The maximum number of different variations we might have for a
     * particular ground tile. */
    protected static final int MAX_TILE_VARIANT = 4;

    /** Used to obtain a graphics context for measuring text before we
     * create the real image. */
    protected static BufferedImage _scratch =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /** Used to fill an image with transparency. */
    protected static Color BLANK = new Color(1.0f, 1.0f, 1.0f, 0f);

    /** The size of the shadow texture. */
    protected static final int SHADOW_TEXTURE_SIZE = 128;
}
