//
// $Id$

package com.threerings.bang.util;

import java.util.HashMap;
import java.util.Properties;

import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A configuration for an icon that supports simple animation.  Static methods
 * load all configurations and provide access to icon instances.
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
    
    /** The animation repeat type (one of the repeat type constants in
     * {@link Controller}. */
    public int repeatType;
    
    /**
     * Determines whether there exists an icon configuration at the given
     * path.
     */
    public static boolean haveIcon (String path)
    {
        return _icons.containsKey(path);
    }
    
    /**
     * Creates an icon from the resource at the given path.  If the path
     * identifies an icon configuration, that configuration will be used;
     * otherwise, the method will assume that the path represents an image
     * and will return a tile-sized icon textured with that image.
     */
    public static Quad createIcon (BasicContext ctx, String path)
    {
        return createIcon(ctx, path, TILE_SIZE, TILE_SIZE, ColorRGBA.white);
    }
    
    /**
     * Creates an icon from the resource at the given path.  If the path
     * identifies an icon configuration, that configuration will be used;
     * otherwise, the method will assume that the path represents an image
     * and will return an icon textured with that image.
     *
     * @param width the desired world space icon width (will be ignored if
     * the path refers to an icon configuration)
     * @param height the desired world space icon height (will be ignored if
     * the path refers to an icon configuration)
     */
    public static Quad createIcon (
        BasicContext ctx, String path, float width, float height)
    {
        return createIcon(ctx, path, width, height, ColorRGBA.white);
    }
    
    /**
     * Creates an icon from the resource at the given path.  If the path
     * identifies an icon configuration, that configuration will be used;
     * otherwise, the method will assume that the path represents an image
     * and will return an icon textured with that image.
     *
     * @param width the desired world space icon width (will be ignored if
     * the path refers to an icon configuration)
     * @param height the desired world space icon height (will be ignored if
     * the path refers to an icon configuration)
     * @param color the color with which to tint the icon (will be ignored if
     * the path refers to an icon configuration and the configuration has
     * {@link #tint} disabled)
     */
    public static Quad createIcon (
        BasicContext ctx, String path, float width, float height,
        ColorRGBA color)
    {
        Quad icon;
        IconConfig iconfig = _icons.get(path);
        if (iconfig != null) {
            icon = iconfig.createIconInstance(ctx, path);
            color = iconfig.tint ? color : ColorRGBA.white;
        } else {
            icon = createIcon(RenderUtil.createTextureState(ctx, path),
                width, height);
        }
        icon.getBatch(0).setDefaultColor(new ColorRGBA(color));
        return icon;
    }

    /**
     * Creates a tile-sized icon with the given texture state.
     */
    public static Quad createIcon (TextureState tstate)
    {
        return createIcon(tstate, TILE_SIZE, TILE_SIZE);
    }

    /**
     * Creates an icon with the given texture state and dimensions.
     */
    public static Quad createIcon (
        TextureState tstate, float width, float height)
    {
        Quad icon = new Quad("icon", width, height) {
            public void updateWorldData (float time) {
                super.updateWorldData(time);
                getBatch(0).queueDistance = 0f;
            }
        };
        configureIcon(icon, tstate);
        return icon;
    }
    
    /**
     * Creates and returns an instance of this icon.
     */
    protected Quad createIconInstance (BasicContext ctx, String path)
    {
        TextureState tstate = RenderUtil.createTextureState(
            ctx, "effects/" + path + "/icon.png");
        if (frameWidth <= 0) {
            return createIcon(tstate, width, height);
        }
        tstate.load();
        final Texture tex = tstate.getTexture().createSimpleClone();
        tstate.setTexture(tex);
        final int fwidth = tex.getImage().getWidth() / frameWidth,
            fheight = tex.getImage().getHeight() / frameHeight;
        tex.setScale(new Vector3f(1f / fwidth, 1f / fheight, 1f));
        tex.setTranslation(new Vector3f());
        final float spf = 1f / frameRate;
        
        Quad icon = new Quad("icon", width, height) {
            public void updateWorldData (float time) {
                super.updateWorldData(time);
                getBatch(0).queueDistance = 0f;
                for (_faccum += time; _faccum >= spf; _faccum -= spf) {
                    advanceFrame();
                }
                Vector3f scale = tex.getScale();
                tex.getTranslation().set((_fidx % fwidth) * scale.x,
                    (fheight - 1 - (_fidx / fwidth)) * scale.y, 0f);
            }
            protected void advanceFrame () {
                if ((_fidx += _fdir) >= frameCount) {
                    if (repeatType == Controller.RT_CLAMP) {
                        _fidx = frameCount - 1;
                        _fdir = 0;
                    } else if (repeatType == Controller.RT_WRAP) {
                        _fidx = 0;
                    } else { // repeatType == Controller.RT_CYCLE
                        _fidx = frameCount - 2;
                        _fdir = -1;
                    }
                } else if (_fidx < 0) {
                    _fidx = 1;
                    _fdir = +1;
                }
            }
            protected int _fidx, _fdir = +1;
            protected float _faccum;
        };
        configureIcon(icon, tstate);
        return icon;
    }
    
    /**
     * Configures an icon with its various states and such.
     */
    protected static void configureIcon (Quad icon, TextureState tstate)
    {
        icon.setRenderState(tstate);
        icon.setRenderState(RenderUtil.blendAlpha);
        icon.setRenderState(RenderUtil.alwaysZBuf);
        icon.updateRenderState();
        icon.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        icon.setLightCombineMode(LightState.OFF);
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
        String rtype = props.getProperty("repeat_type");
        if ("wrap".equals(rtype)) {
            iconfig.repeatType = Controller.RT_WRAP;
        } else if ("cycle".equals(rtype)) {
            iconfig.repeatType = Controller.RT_CYCLE;
        } else { // "clamp" or null
            iconfig.repeatType = Controller.RT_CLAMP;
        }
        
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
