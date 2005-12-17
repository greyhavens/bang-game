//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;

import java.io.File;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Dome;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Used to display the sky.
 */
public class SkyNode extends Node
{
    public SkyNode (BasicContext ctx)
    {
        super("skynode");
        _ctx = ctx;
        
        setLightCombineMode(LightState.OFF);
        
        // create the dome geometry
        _dome = new Dome("dome", DOME_PLANES, DOME_RADIAL_SAMPLES,
            DOME_RADIUS);
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        _dome.setLocalRotation(rot);
        _dome.setLocalTranslation(new Vector3f(0f, 0f, -10f));
        attachChild(_dome);
    }
    
    /**
     * Initializes the sky geometry using data from the given board
     * and saves the board reference for later updates.
     */
    public void createBoardSky (BangBoard board)
    {
        _board = board;
        
        // (re)create the gradient texture
        refreshGradient();
    }
    
    /**
     * Updates the gradient texture according to the board parameters.
     */
    public void refreshGradient ()
    {
        TextureState tstate = _ctx.getRenderer().createTextureState();
        tstate.setTexture(createGradientTexture());
        setRenderState(tstate);
        updateRenderState();
    }
    
    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);
        
        // match the position of the camera
        setLocalTranslation(_ctx.getRenderer().getCamera().getLocation());
    }
    
    /**
     * Creates and returns the gradient texture that fades from the horizon
     * color to the overhead color.
     */
    protected Texture createGradientTexture ()
    {
        int size = GRADIENT_TEXTURE_SIZE;
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * 3);
        ColorRGBA hcolor = RenderUtil.createColorRGBA(
            _board.getSkyHorizonColor()),
                ocolor = RenderUtil.createColorRGBA(
            _board.getSkyOverheadColor()),
                tcolor = new ColorRGBA();
        float falloff = _board.getSkyFalloff();
        
        for (int i = 0; i < size; i++) {
            float s = i / (size-1f),
                a = FastMath.exp(-falloff * s);
            tcolor.interpolate(ocolor, hcolor, a);
            
            pbuf.put((byte)(tcolor.r * 255));
            pbuf.put((byte)(tcolor.g * 255));
            pbuf.put((byte)(tcolor.b * 255));
        }
        pbuf.rewind();
        
        Texture texture = new Texture();
        texture.setImage(new Image(Image.RGB888, 1, size, pbuf));
        texture.setFilter(Texture.FM_LINEAR);
        return texture;
    }
    
    /** The application context. */
    protected BasicContext _ctx;
    
    /** The dome geometry. */
    protected Dome _dome;
    
    /** The current board object. */
    protected BangBoard _board;
    
    /** The number of vertical samples in the sky dome. */
    protected static final int DOME_PLANES = 16;
    
    /** The number of radial samples for the sky dome. */
    protected static final int DOME_RADIAL_SAMPLES = 32;
    
    /** The radius of the sky dome. */
    protected static final float DOME_RADIUS = 1000f;
    
    /** The size of the one-dimensional gradient texture. */
    protected static final int GRADIENT_TEXTURE_SIZE = 1024;
}
