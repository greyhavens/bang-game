//
// $Id$

package com.threerings.bang.util;

import java.nio.FloatBuffer;

import java.util.HashMap;
import java.util.Properties;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

import com.threerings.jme.util.JmeUtil;
import com.threerings.jme.util.JmeUtil.FrameState;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A configuration for an icon that supports simple animation. Static methods load all
 * configurations and provide access to icon instances.
 *
 * <h3>jME3 cutover (Phase 2, cluster 6)</h3>
 *
 * The fork built icons as {@code com.jme.scene.shape.Quad} <em>nodes</em> with a fork
 * {@code TextureState}, and animated sprite-sheet frames by setting a scale/translation transform
 * on the {@code Texture} object (overriding {@code updateWorldData} to advance the frame each tick).
 * jME3 has none of that: an icon is a {@link Geometry} wrapping a {@link Quad} mesh with an
 * {@code Unshaded} {@link Material}, and frame animation is a {@link FrameControl} ({@link
 * AbstractControl}) that rewrites the geometry's texture-coordinate buffer each tick. Every method
 * that took/returned a fork {@code TextureState}/{@code Quad} now uses {@link Material}/{@link
 * Geometry} — the sprite (cluster 1) and effect-viz (cluster 2) call sites recode against that.
 *
 * <p>Icon tint (fork {@code setDefaultColor}) maps to the {@code Color} param on the {@code
 * Unshaded} material.
 */
public class IconConfig
{
    /** The world space icon width. */
    public float width;

    /** The world space icon height. */
    public float height;

    /** If true, tint the icon with the player color of the affected piece. */
    public boolean tint;

    /** The texel width of the animation frames (-1 if not animated). */
    public int frameWidth;

    /** The texel height of the animation frames. */
    public int frameHeight;

    /** The number of animation frames contained in the texture. */
    public int frameCount;

    /** The frame rate in frames per second. */
    public float frameRate;

    /** The animation repeat type (one of {@link JmeUtil}'s {@code RT_*} constants). */
    public int repeatType;

    /**
     * Determines whether there exists an icon configuration at the given path.
     */
    public static boolean haveIcon (String path)
    {
        return _icons.containsKey(path);
    }

    /**
     * Creates a tile-sized white icon from the resource at the given path.
     */
    public static Geometry createIcon (BasicContext ctx, String path)
    {
        return createIcon(ctx, path, TILE_SIZE, TILE_SIZE, ColorRGBA.White);
    }

    /**
     * Creates an icon from the resource at the given path with the given dimensions.
     */
    public static Geometry createIcon (
        BasicContext ctx, String path, float width, float height)
    {
        return createIcon(ctx, path, width, height, ColorRGBA.White);
    }

    /**
     * Creates an icon from the resource at the given path.
     *
     * @param color the color with which to tint the icon (ignored for configured icons whose
     * {@link #tint} is disabled).
     */
    public static Geometry createIcon (
        BasicContext ctx, String path, float width, float height,
        ColorRGBA color)
    {
        Geometry icon;
        IconConfig iconfig = _icons.get(path);
        if (iconfig != null) {
            icon = iconfig.createIconInstance(ctx, path);
            color = iconfig.tint ? color : ColorRGBA.White;
        } else {
            icon = createIcon(RenderUtil.createTextureMaterial(ctx, path),
                width, height);
        }
        icon.getMaterial().setColor("Color", new ColorRGBA(color));
        return icon;
    }

    /**
     * Creates a tile-sized icon with the given material.
     */
    public static Geometry createIcon (Material material)
    {
        return createIcon(material, TILE_SIZE, TILE_SIZE);
    }

    /**
     * Creates an icon with the given material and dimensions.
     */
    public static Geometry createIcon (Material material, float width, float height)
    {
        Geometry icon = new Geometry("icon", new Quad(width, height));
        configureIcon(icon, material);
        return icon;
    }

    /**
     * Creates and returns an instance of this icon.
     */
    protected Geometry createIconInstance (BasicContext ctx, String path)
    {
        Material material = RenderUtil.createTextureMaterial(
            ctx, "effects/" + path + "/icon.png");
        if (frameWidth <= 0) {
            return createIcon(material, width, height);
        }
        Texture tex = material.getTextureParam("ColorMap").getTextureValue();
        final int fwidth = tex.getImage().getWidth() / frameWidth,
            fheight = tex.getImage().getHeight() / frameHeight;

        Geometry icon = createIcon(material, width, height);
        icon.addControl(new FrameControl(fwidth, fheight));
        return icon;
    }

    /**
     * Configures an icon's material/queue state.
     */
    public static void configureIcon (Geometry icon, Material material)
    {
        RenderUtil.applyBlendAlpha(material);
        RenderUtil.applyAlwaysZBuf(material);
        icon.setMaterial(material);
        icon.setQueueBucket(Bucket.Transparent);
    }

    /**
     * A control that advances a sprite-sheet icon's frame by rewriting the geometry's
     * texture-coordinate buffer. Replaces the fork's texture scale/translation animation.
     */
    protected class FrameControl extends AbstractControl
    {
        public FrameControl (int fwidth, int fheight)
        {
            _fwidth = fwidth;
            _fheight = fheight;
            _su = 1f / fwidth;
            _sv = 1f / fheight;
        }

        @Override // documentation inherited
        protected void controlUpdate (float tpf)
        {
            _fstate.update(tpf, frameRate, frameCount, repeatType);
            float u = (_fstate.idx % _fwidth) * _su;
            float v = (_fheight - 1 - (_fstate.idx / _fwidth)) * _sv;
            Geometry geom = (Geometry)spatial;
            VertexBuffer tc = geom.getMesh().getBuffer(VertexBuffer.Type.TexCoord);
            float[] uv = new float[] {
                u, v,
                u + _su, v,
                u + _su, v + _sv,
                u, v + _sv,
            };
            if (tc == null) {
                geom.getMesh().setBuffer(VertexBuffer.Type.TexCoord, 2,
                    BufferUtils.createFloatBuffer(uv));
            } else {
                FloatBuffer fb = (FloatBuffer)tc.getData();
                fb.rewind();
                fb.put(uv);
                tc.updateData(fb);
            }
        }

        @Override // documentation inherited
        protected void controlRender (RenderManager rm, ViewPort vp)
        {
        }

        protected int _fwidth, _fheight;
        protected float _su, _sv;
        protected FrameState _fstate = new FrameState();
    }

    protected static void registerIcon (String icon)
    {
        Properties props = BangUtil.resourceToProperties(
            "rsrc/effects/" + icon + "/icon.properties");

        IconConfig iconfig = new IconConfig();
        iconfig.width = BangUtil.getFloatProperty(
            icon, props, "width", TILE_SIZE);
        iconfig.height = BangUtil.getFloatProperty(
            icon, props, "height", TILE_SIZE);
        iconfig.tint = BangUtil.getBooleanProperty(
            icon, props, "tint", false);
        iconfig.frameWidth = BangUtil.getIntProperty(
            icon, props, "frame_width", -1);
        iconfig.frameHeight = BangUtil.getIntProperty(
            icon, props, "frame_height", -1);
        iconfig.frameCount = BangUtil.getIntProperty(
            icon, props, "frame_count", 1);
        iconfig.frameRate = BangUtil.getFloatProperty(
            icon, props, "frame_rate", 8f);
        iconfig.repeatType = JmeUtil.parseRepeatType(props.getProperty("repeat_type"),
            JmeUtil.RT_WRAP);

        _icons.put(icon, iconfig);
    }

    protected static HashMap<String, IconConfig> _icons =
        new HashMap<String, IconConfig>();

    static {
        // register our icons
        for (String icon : BangUtil.townResourceToStrings(
            "rsrc/effects/TOWN/icons.txt")) {
            registerIcon(icon);
        }
    }
}
