//
// $Id$

package com.threerings.bang.util;

import java.lang.ref.WeakReference;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.RenderState.TestFunction;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

import com.jmex.bui.util.Dimension;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.data.TerrainConfig;
import com.threerings.jme.util.ImageCache;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Shared graphics utility methods and a small <b>Material/render-helper library</b> for the
 * Bang! client.
 *
 * <h3>jME3 cutover (Phase 2, cluster 6 — the keystone for the sprite/effect Wave-2 clusters)</h3>
 *
 * The fork {@code RenderUtil} was a central <em>render-state factory</em>: it manufactured shared,
 * mutable {@code AlphaState}/{@code ZBufferState}/{@code CullState}/{@code LightState}/
 * {@code TextureState} objects that callers attached to spatials via {@code setRenderState(state)}.
 * jME3 has no stateful fixed-function pipeline (migration map §2.4): a {@link Geometry} carries a
 * {@link Material}; blend/depth/cull live on {@code material.getAdditionalRenderState()}; lights
 * attach to spatials. The {@code setRenderState(sharedState)} idiom is therefore <b>dead</b>.
 *
 * <p>This class is the jME3 replacement the sprite (cluster 1) and effect-viz (cluster 2) clusters
 * code against. The mapping the Wave-2 agents should follow:
 *
 * <ul>
 *   <li><b>The old shared blend/depth/cull state fields become "applier" helpers.</b> Where fork
 *   code did {@code spatial.setRenderState(RenderUtil.blendAlpha)}, jME3 code does
 *   {@code RenderUtil.applyBlendAlpha(material)} (or builds the material with the right preset up
 *   front via {@link #createTextureMaterial}). The applier mutates {@code material
 *   .getAdditionalRenderState()} and (for the overlay/transparent presets) sets the geometry's
 *   queue bucket via the {@link #setOverlay} / {@code Bucket} helpers below. The presets are also
 *   exposed as the raw {@link BlendMode}/{@link FaceCullMode}/{@link TestFunction} constants so a
 *   caller building its own MatDef can apply them directly.</li>
 *   <li><b>{@code createTextureState(ctx, path)} now returns a textured {@link Material}</b>
 *   ({@code Unshaded.j3md} with a {@code ColorMap}), not a fork {@code TextureState}. Call sites
 *   that did {@code geom.setRenderState(RenderUtil.createTextureState(ctx, path))} become
 *   {@code geom.setMaterial(RenderUtil.createTextureMaterial(ctx, path))}. {@code createTextureState}
 *   is retained as a thin alias for source compatibility during the port.</li>
 *   <li><b>{@code createTexture}/{@code configureTexture}/{@code getGroundTexture} return
 *   {@link Texture2D}</b> built from a jME3 {@link Image} (the {@code ImageCache} already produces
 *   jME3 {@code Image}s). The fork {@code Texture}'s scale/translation/rotation transform has no
 *   {@code Texture2D} equivalent; transform-on-texture callers (ground tiling, shadow rotation)
 *   move to texture-coordinate math or a material param (see {@link #setTextureTile} /
 *   {@link #createShadowTexture}).</li>
 *   <li><b>Render-to-texture</b> ({@code createTextureRenderer}) returns an {@link OffscreenRenderer}
 *   backed by a jME3 {@code FrameBuffer} + offscreen {@code ViewPort} (replacing the fork
 *   back-buffer-copy {@code TextureRenderer}; see {@link BackTextureRenderer}).</li>
 * </ul>
 *
 * @see BackTextureRenderer the jME3 FrameBuffer-backed render-to-texture helper.
 */
public class RenderUtil
{
    // ---------------------------------------------------------------------------------------------
    // Render-state presets (the fork shared-state objects, as jME3 constants + appliers).
    //
    // Wave-2 mapping:
    //   spatial.setRenderState(RenderUtil.blendAlpha)   -> RenderUtil.applyBlendAlpha(material)
    //   spatial.setRenderState(RenderUtil.addAlpha)     -> RenderUtil.applyAddAlpha(material)
    //   spatial.setRenderState(RenderUtil.overlayZBuf)  -> RenderUtil.applyOverlay(material/geom)
    //   spatial.setRenderState(RenderUtil.backCull)     -> RenderUtil.applyBackCull(material)
    // ---------------------------------------------------------------------------------------------

    /** Standard back-to-front alpha blend (fork {@code blendAlpha}: SRC_ALPHA / ONE_MINUS_SRC_ALPHA). */
    public static final BlendMode BLEND_ALPHA = BlendMode.Alpha;

    /** Additive blend (fork {@code addAlpha}: SRC_ALPHA / ONE) for glows and emissive particles. */
    public static final BlendMode BLEND_ADD = BlendMode.AlphaAdditive;

    /** No blending (fork {@code opaqueAlpha}). */
    public static final BlendMode BLEND_OPAQUE = BlendMode.Off;

    /** Back-face culling (fork {@code backCull}). */
    public static final FaceCullMode CULL_BACK = FaceCullMode.Back;

    /** Front-face culling (fork {@code frontCull}). */
    public static final FaceCullMode CULL_FRONT = FaceCullMode.Front;

    /**
     * Applies the standard alpha-blend preset (fork {@code RenderUtil.blendAlpha}) to a material's
     * additional render state and moves its geometry into the transparent queue. Returns the
     * material for chaining.
     */
    public static Material applyBlendAlpha (Material material)
    {
        RenderState rs = material.getAdditionalRenderState();
        rs.setBlendMode(BLEND_ALPHA);
        return material;
    }

    /**
     * Applies the additive-blend preset (fork {@code RenderUtil.addAlpha}) to a material.
     */
    public static Material applyAddAlpha (Material material)
    {
        material.getAdditionalRenderState().setBlendMode(BLEND_ADD);
        return material;
    }

    /**
     * Applies the opaque (no-blend) preset (fork {@code RenderUtil.opaqueAlpha}) to a material.
     */
    public static Material applyOpaqueAlpha (Material material)
    {
        material.getAdditionalRenderState().setBlendMode(BLEND_OPAQUE);
        return material;
    }

    /**
     * Applies the "always-pass, no depth write" depth preset (fork {@code RenderUtil.alwaysZBuf}).
     */
    public static Material applyAlwaysZBuf (Material material)
    {
        RenderState rs = material.getAdditionalRenderState();
        rs.setDepthTest(true);
        rs.setDepthWrite(false);
        rs.setDepthFunc(TestFunction.Always);
        return material;
    }

    /**
     * Applies the standard "less-or-equal, depth write" depth preset (fork
     * {@code RenderUtil.lequalZBuf}).
     */
    public static Material applyLequalZBuf (Material material)
    {
        RenderState rs = material.getAdditionalRenderState();
        rs.setDepthTest(true);
        rs.setDepthWrite(true);
        rs.setDepthFunc(TestFunction.LessOrEqual);
        return material;
    }

    /**
     * Applies the overlay depth preset (fork {@code RenderUtil.overlayZBuf}: less-or-equal test,
     * <em>no</em> depth write) to a material. Callers that also want the geometry in the
     * transparent bucket should call {@link #setOverlay(Spatial)} on the spatial.
     */
    public static Material applyOverlayZBuf (Material material)
    {
        RenderState rs = material.getAdditionalRenderState();
        rs.setDepthTest(true);
        rs.setDepthWrite(false);
        rs.setDepthFunc(TestFunction.LessOrEqual);
        return material;
    }

    /**
     * Applies back-face culling (fork {@code RenderUtil.backCull}) to a material.
     */
    public static Material applyBackCull (Material material)
    {
        material.getAdditionalRenderState().setFaceCullMode(CULL_BACK);
        return material;
    }

    /**
     * Applies front-face culling (fork {@code RenderUtil.frontCull}) to a material.
     */
    public static Material applyFrontCull (Material material)
    {
        material.getAdditionalRenderState().setFaceCullMode(CULL_FRONT);
        return material;
    }

    /**
     * Moves a spatial into the transparent render queue (the jME3 analogue of the fork's
     * {@code QUEUE_TRANSPARENT} + overlay depth setup).
     */
    public static void setOverlay (Spatial spatial)
    {
        spatial.setQueueBucket(Bucket.Transparent);
    }

    /**
     * Initializes shared graphics resources. The fork created its shared render-state objects here;
     * jME3 has no such shared state, so this is now a no-op kept for call-site compatibility (the
     * presets above are immutable constants).
     */
    public static void init (BasicContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * @deprecated jME3 has no shared render-state objects to initialize; use the {@code apply*}
     * helpers on a per-material basis. Retained as a no-op for source compatibility.
     */
    @Deprecated
    public static void initStates ()
    {
        // no-op: jME3 has no fixed-function shared state objects
    }

    /** Rounds the supplied value up to a power of two. */
    public static int nextPOT (int value)
    {
        return (Integer.bitCount(value) > 1) ?
            (Integer.highestOneBit(value) << 1) : value;
    }

    /**
     * Returns a randomly selected ground texture for the specified terrain type.
     */
    public static Texture2D getGroundTexture (BasicContext ctx, int code)
    {
        ArrayList<WeakReference<Texture2D>> texs = _groundTexs.get(code);
        if (texs == null) {
            TerrainConfig terrain = TerrainConfig.getConfig(code);
            if (terrain == null) {
                log.warning("Requested ground texture for unknown terrain", "code", code);
                return null;
            }
            _groundTexs.put(code, texs = new ArrayList<WeakReference<Texture2D>>());
            String prefix = "terrain/" + terrain.type + "/texture";
            for (int ii = 1; ; ii++) {
                String path = prefix + ii + ".png";
                if (!ctx.getResourceManager().getResourceFile(path).exists()) {
                    break;
                }
                texs.add(new WeakReference<Texture2D>(null));
            }
            if (texs.isEmpty()) {
                log.warning("Found no ground textures", "type", terrain);
            }
        }
        int tsize = texs.size();
        if (tsize == 0) {
            return null;
        }
        int idx = RandomUtil.getInt(tsize);
        Texture2D tex = texs.get(idx).get();
        if (tex == null) {
            TerrainConfig terrain = TerrainConfig.getConfig(code);
            String path = "terrain/" + terrain.type + "/texture" + (idx + 1) + ".png";
            texs.set(idx, new WeakReference<Texture2D>(
                tex = ctx.getTextureCache().getTexture(
                    path, BangPrefs.isMediumDetail() ? 1f : 0.5f)));
            // fork: tex.setScale(1/scale) — jME3 textures carry no transform; the terrain splat
            // material applies the 1/scale tiling factor via its texture-coordinate generation.
            tex.setWrap(WrapMode.Repeat);
        }
        return tex;
    }

    /**
     * Creates a {@link Quad}-geometry whose material displays the supplied text. The text is white;
     * tint it via the returned material's {@code Color} param.
     */
    public static Geometry createTextQuad (BasicContext ctx, Font font, String text)
    {
        return createTextQuad(ctx, font, ColorRGBA.White, null, text);
    }

    /**
     * Creates a textured {@link Quad}-geometry that displays the supplied text.
     *
     * @param color Color of the text
     * @param ocolor Outline color of the text
     */
    public static Geometry createTextQuad (BasicContext ctx, Font font,
            ColorRGBA color, ColorRGBA ocolor, String text)
    {
        return createTextQuad(ctx, font, color, ocolor, text, 1);
    }

    public static Geometry createTextQuad (BasicContext ctx, Font font,
            ColorRGBA color, ColorRGBA ocolor, String text, int outline)
    {
        Vector2f[] tcoords = new Vector2f[4];
        Dimension size = new Dimension();
        Texture2D tex = createTextTexture(
            ctx, font, color, ocolor, text, tcoords, size, outline);

        Quad mesh = new Quad(size.width, size.height);
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.TexCoord, 2,
            BufferUtils.createFloatBuffer(tcoords));
        Geometry quad = new Geometry("text", mesh);

        Material mat = createTextureMaterial(ctx, tex);
        applyBlendAlpha(mat);
        quad.setMaterial(mat);
        quad.setQueueBucket(Bucket.Transparent);
        return quad;
    }

    /**
     * Renders the specified text into an image (sized appropriately) and creates a texture from it.
     *
     * @param ocolor if not-null the text will be outlined in the supplied color.
     * @param tcoords filled in with the texture coordinates that show only the text.
     */
    public static Texture2D createTextTexture (
        BasicContext ctx, Font font, ColorRGBA color, ColorRGBA ocolor,
        String text, Vector2f[] tcoords, Dimension size)
    {
        return createTextTexture(
                ctx, font, color, ocolor, text, tcoords, size, 1);
    }

    public static Texture2D createTextTexture (
        BasicContext ctx, Font font, ColorRGBA color, ColorRGBA ocolor,
        String text, Vector2f[] tcoords, Dimension size, int outline)
    {
        Graphics2D gfx = _scratch.createGraphics();
        Color acolor = new Color(color.r, color.g, color.b, color.a);
        Color oacolor = (ocolor == null) ? null :
            new Color(ocolor.r, ocolor.g, ocolor.b, ocolor.a);
        TextLayout layout;
        try {
            gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gfx.setColor(acolor);
            layout = new TextLayout(text, font, gfx.getFontRenderContext());
        } finally {
            gfx.dispose();
        }

        // BUG-4 (bottom-clip facet): size the texture to the FULL glyph-outline bounds, not the
        // ascent/descent line metrics. The outline (and, for the outlined path, its stroke) can
        // extend a pixel or two ABOVE the cap line (outline bounds y < -ascent) and below the
        // baseline; the old code anchored the draw at (0, ascent) and sized height to
        // leading+ascent+descent, so any part of the outline above row 0 was cropped at the image's
        // top edge. ImageCache.convertImage does NOT flip Y, so jME maps image row 0 -> texcoord
        // V=0 (the quad's bottom) — that cropped top edge therefore surfaces as a clipped GLYPH
        // BOTTOM live. We measure the real ink box and pad it so nothing touches an edge.
        Rectangle2D ob = layout.getOutline(null).getBounds2D();
        // outline path is in baseline space (y grows downward, baseline at y=0); the stroke widens
        // the ink box by ~half the stroke on every side.
        int pad = Math.max(2, (outline + 1) / 2 + 1);
        // glyph ink extents relative to the drawing origin we will use (top-left of the ink box).
        int gw = (int)Math.ceil(ob.getWidth());
        int gh = (int)Math.ceil(ob.getHeight());
        int width = gw + 2 * pad;
        int height = gh + 2 * pad;
        // where to place the pen so the ink box lands at (pad, pad): the outline's top-left is at
        // (ob.getX(), ob.getY()) relative to the pen origin, so shift the pen by -ob.x, -ob.y + pad.
        float penX = (float)(-ob.getX() + pad);
        float penY = (float)(-ob.getY() + pad);

        if (size != null) {
            size.width = width;
            size.height = height;
        }

        // texture image must be square and a power of two
        int tsize = nextPOT(Math.max(width, Math.max(height, 1)));

        // render the text into the image
        BufferedImage image = ImageCache.createCompatibleImage(tsize, tsize, true);
        gfx = image.createGraphics();
        try {
            gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            gfx.setColor(acolor);
            // BUG-4 (mirror facet): pre-flip the glyph vertically within its [0,height] band. Every
            // other texture path runs through ImageCache.createImage(.., flip=true), which mirrors
            // the AWT image into OpenGL's bottom-up row order before convertImage. createTextTexture
            // historically called convertImage DIRECTLY (no flip) and relied on tcoords for
            // orientation, so the glyph ended up upside-down in GL — invisible on near-symmetric
            // digits like "3" but reading as a reversed/"S"-shaped "2" (the camera-azimuth-dependent
            // "mirror"). Mirroring here matches the standard flip while leaving the tcoords (and the
            // qrat sizing that reads tcoords[2].y) untouched: the band [0,height] flip composes with
            // the no-flip convert + GL's row0->V0 to a net-upright glyph.
            gfx.translate(0, height);
            gfx.scale(1, -1);
            if (oacolor != null) {
                gfx.translate(penX, penY);
                if (outline > 1) {
                    gfx.setColor(oacolor);
                    Stroke oldstroke = gfx.getStroke();
                    gfx.setStroke(new BasicStroke(outline));
                    gfx.draw(layout.getOutline(null));
                    gfx.setStroke(oldstroke);
                    gfx.setColor(acolor);
                }
                gfx.fill(layout.getOutline(null));
                if (outline <= 1) {
                    gfx.setColor(oacolor);
                    gfx.draw(layout.getOutline(null));
                }
            } else {
                gfx.setComposite(AlphaComposite.SrcOut);
                layout.draw(gfx, penX, penY);
            }
        } finally {
            gfx.dispose();
        }

        // fill in the texture coordinates. jME3 cutover: jME3's com.jme3.scene.shape.Quad winds its
        // vertices bottom-left, bottom-right, top-right, top-left with the U axis running along the
        // quad's local +X. The fork's Quad used a different winding, so the ported coords (which
        // assigned the texture's U to the quad's +Y) rotated every glyph 90 deg -> the sideways
        // claim/counter/level numbers. Map U->local X and V->local Y so the text reads upright.
        // The glyph is pre-flipped vertically above (BUG-4 mirror fix), so these tcoords stay as the
        // 6b winding and the qrat sizing (which reads tcoords[2].y) is unaffected.
        float tsf = tsize;
        tcoords[0] = new Vector2f(0, 0);             // v0 bottom-left
        tcoords[1] = new Vector2f(width/tsf, 0);     // v1 bottom-right
        tcoords[2] = new Vector2f(width/tsf, height/tsf); // v2 top-right
        tcoords[3] = new Vector2f(0, height/tsf);    // v3 top-left

        return createTexture(ctx, ImageCache.convertImage(image));
    }

    /**
     * Creates a {@link Texture2D} using the supplied jME3 {@link Image}.
     */
    public static Texture2D createTexture (BasicContext ctx, Image image)
    {
        Texture2D texture = ctx.getTextureCache().createTexture();
        configureTexture(texture, image);
        return texture;
    }

    /**
     * Configures an existing texture with the supplied image and the standard linear/mipmap/wrap
     * settings.
     */
    public static void configureTexture (Texture texture, Image image)
    {
        texture.setImage(image);
        texture.setMagFilter(MagFilter.Bilinear);
        texture.setMinFilter(BangPrefs.isMediumDetail() ?
            MinFilter.Trilinear : MinFilter.BilinearNoMipMaps);
        texture.setWrap(WrapMode.Repeat);
    }

    /**
     * Attempts to enable S3TC texture compression on the supplied texture. jME3 selects compressed
     * internal formats from the image format; with no fork {@code Image} type to flip this is a
     * no-op placeholder retained for call-site compatibility (carried for the Phase-4 material
     * pass, where DXT formats are chosen at asset-load time).
     */
    public static void enableTextureCompression (Texture texture)
    {
        // no-op on jME3; compression is selected by the asset loader / image format (Phase 4).
    }

    /**
     * Creates a textured {@link Material} ({@code Unshaded.j3md} with a {@code ColorMap}) for the
     * image at the supplied path, loaded through the texture cache. This is the jME3 replacement
     * for the fork {@code createTextureState(ctx, path)} — call sites assign it via
     * {@code geom.setMaterial(...)}.
     */
    public static Material createTextureMaterial (BasicContext ctx, String path)
    {
        return createTextureMaterial(ctx, path, 1f);
    }

    /**
     * Creates a textured {@link Material} for the image at the supplied path and scale.
     */
    public static Material createTextureMaterial (
        BasicContext ctx, String path, float scale)
    {
        return createTextureMaterial(ctx, ctx.getTextureCache().getTexture(path, scale));
    }

    /**
     * Creates a textured {@link Material} ({@code Unshaded.j3md}, {@code ColorMap}) using the
     * supplied texture.
     */
    public static Material createTextureMaterial (BasicContext ctx, Texture texture)
    {
        Material mat = new Material(ctx.getAssetManager(),
            "Common/MatDefs/Misc/Unshaded.j3md");
        if (texture != null) {
            mat.setTexture("ColorMap", texture);
        }
        return mat;
    }

    /**
     * Creates a terrain splat-layer {@link Material} (the custom {@code shaders/TerrainSplat.j3md}).
     * The ground texture is sampled per-pixel and modulated by the per-vertex shadow color; if an
     * alpha map is supplied the layer's coverage comes from it (per-pixel splat blending across
     * terrain-type boundaries, the jME3 replacement for the fork's fixed-function texture combine).
     * Pass {@code alpha == null} for the opaque base layer.
     */
    public static Material createTerrainMaterial (
        BasicContext ctx, Texture ground, Texture alpha)
    {
        Material mat = new Material(ctx.getAssetManager(), "shaders/TerrainSplat.j3md");
        if (ground != null) {
            mat.setTexture("ColorMap", ground);
        }
        if (alpha != null) {
            mat.setTexture("AlphaMap", alpha);
        }
        mat.setBoolean("VertexColor", true);
        return mat;
    }

    /**
     * Source-compatible alias for {@link #createTextureMaterial(BasicContext, String)}.
     *
     * @deprecated prefer {@link #createTextureMaterial}; the fork returned a {@code TextureState},
     * jME3 returns a {@link Material} assigned via {@code setMaterial}.
     */
    @Deprecated
    public static Material createTextureState (BasicContext ctx, String path)
    {
        return createTextureMaterial(ctx, path);
    }

    /** @deprecated see {@link #createTextureState(BasicContext, String)}. */
    @Deprecated
    public static Material createTextureState (BasicContext ctx, String path, float scale)
    {
        return createTextureMaterial(ctx, path, scale);
    }

    /** @deprecated see {@link #createTextureState(BasicContext, String)}. */
    @Deprecated
    public static Material createTextureState (BasicContext ctx, Texture texture)
    {
        return createTextureMaterial(ctx, texture);
    }

    /**
     * Ensures that the specified texture is uploaded. jME3 uploads textures lazily on first render
     * and manages the GL name itself, so this is a no-op kept for call-site compatibility.
     */
    public static void ensureLoaded (BasicContext ctx, Texture tex)
    {
        // no-op on jME3 (lazy upload)
    }

    /**
     * Configures the supplied texture's wrap mode to repeat for tiled use. The fork applied a
     * scale/translation transform on the texture object to select tile {@code tile} from a
     * {@code size}x{@code size} grid; jME3 textures carry no transform, so tiling is expressed via
     * the mesh's texture coordinates. This helper now only ensures the wrap mode; callers that need
     * sub-tile selection set the appropriate UVs on their mesh.
     */
    public static void setTextureTile (Texture texture, int size, int tile)
    {
        texture.setWrap(WrapMode.Repeat);
    }

    /**
     * Creates (and caches) a {@link Texture2D} containing a soft shadow blob for the specified
     * light parameters. The fork rotated/translated the texture object to orient the shadow; the
     * rotation is baked into the generated pixels here so the result is a plain {@link Texture2D}.
     */
    public static Texture2D createShadowTexture (
        BasicContext ctx, float length, float rotation, float intensity)
    {
        if (_shadtex != null && _slength == length &&
            _srotation == rotation && _sintensity == intensity) {
            return _shadtex;
        }

        _slength = length;
        _srotation = rotation;
        _sintensity = intensity;

        float yscale = length / TILE_SIZE;
        int size = SHADOW_TEXTURE_SIZE, hsize = size / 2;
        float cos = FastMath.cos(rotation), sin = FastMath.sin(rotation);
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * size * 4);
        byte[] pixel = new byte[] { 0, 0, 0, 0 };
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // rotate the sample point about the texture center (bakes the fork
                // texture-rotation into the pixels, since Texture2D has no transform)
                float sx = (float)(x - hsize) / hsize, sy = (float)(y - hsize) / hsize;
                float rx = cos * sx - sin * sy, ry = sin * sx + cos * sy;
                float xd = rx, yd = yscale * ry,
                    d = FastMath.sqrt(xd*xd + yd*yd),
                    val = d < 0.25f ? intensity : intensity *
                        Math.max(0f, 1.333f - 1.333f*d);
                pixel[3] = (byte)(val * 255);
                pbuf.put(pixel);
            }
        }
        pbuf.rewind();

        Image img = new Image(Image.Format.RGBA8, size, size, pbuf,
            com.jme3.texture.image.ColorSpace.Linear);
        _shadtex = new Texture2D(img);
        _shadtex.setWrap(WrapMode.Clamp);
        return _shadtex;
    }

    /**
     * Creates a render-to-texture helper for an offscreen target of the given dimensions, backed by
     * a jME3 {@code FrameBuffer} + offscreen {@code ViewPort}.
     */
    public static BackTextureRenderer createTextureRenderer (BasicContext ctx,
        int width, int height)
    {
        return new BackTextureRenderer(ctx, width, height);
    }

    /**
     * Determines whether the given object or any of its ancestors were determined to be outside of
     * the view frustum on the last cull.
     */
    public static boolean isOutsideFrustum (Spatial spatial)
    {
        for (; spatial != null; spatial = spatial.getParent()) {
            if (spatial.getLastFrustumIntersection() ==
                    com.jme3.renderer.Camera.FrustumIntersect.Outside) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a jME3 {@link ColorRGBA} with alpha one from a packed RGB value.
     */
    public static ColorRGBA createColorRGBA (int rgb)
    {
        return new ColorRGBA(((rgb >> 16) & 0xFF) / 255f,
                             ((rgb >> 8) & 0xFF) / 255f,
                             (rgb & 0xFF) / 255f, 1f);
    }

    protected static final int btoi (byte value)
    {
        return (value < 0) ? 256 + value : value;
    }

    /** Our application context (set in {@link #init}). */
    protected static BasicContext _ctx;

    protected static HashIntMap<ArrayList<WeakReference<Texture2D>>> _groundTexs =
        new HashIntMap<ArrayList<WeakReference<Texture2D>>>();

    /** Our most recently created shadow texture. */
    protected static Texture2D _shadtex;

    /** The parameters used to create our shadow texture. */
    protected static float _slength, _srotation, _sintensity;

    /** The maximum number of different variations we might have for a particular ground tile. */
    protected static final int MAX_TILE_VARIANT = 4;

    /** Used to obtain a graphics context for measuring text before we create the real image. */
    protected static BufferedImage _scratch =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /** Used to fill an image with transparency. */
    protected static Color BLANK = new Color(1.0f, 1.0f, 1.0f, 0f);

    /** The size of the shadow texture. */
    protected static final int SHADOW_TEXTURE_SIZE = 128;
}
