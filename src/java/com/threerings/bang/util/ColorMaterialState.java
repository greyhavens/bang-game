//
// $Id$

package com.threerings.bang.util;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.MaterialState;
import com.jme.util.geom.BufferUtils;

/**
 * An OpenGL material state that also handles the color material setting, which
 * controls the material parameter that varies by vertex color.
 */
public class ColorMaterialState extends MaterialState
{
    /**
     * Creates a state with color material disabled.
     */
    public ColorMaterialState ()
    {
        this(-1);
    }
    
    /**
     * Creates a state with the specified color material setting (GL_AMBIENT,
     * GL_DIFFUSE, etc.).
     */
    public ColorMaterialState (int type)
    {
        _type = type;
    }
    
    @Override // documentation inherited
    public void apply ()
    {
        if (!isEnabled()) {
            return;
        }
        // enable or disable color materials as necessary
        if (_type == -1 && _currentColorMaterial) {
            GL11.glDisable(GL11.GL_COLOR_MATERIAL);
            _currentColorMaterial = false;
            
        } else if (_type != -1) {
            if (_type != _currentType) {
                GL11.glColorMaterial(GL11.GL_FRONT, _currentType = _type);
            }
            if (!_currentColorMaterial) {
                GL11.glEnable(GL11.GL_COLOR_MATERIAL);
                _currentColorMaterial = true;
            }
        }
        
        // update the material properties; we would like to update them only
        // when they're different, but for some reason that fails sometimes
        BufferUtils.setInBuffer(getAmbient(), _cbuf, 0);
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, _cbuf);
        
        BufferUtils.setInBuffer(getDiffuse(), _cbuf, 0);
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, _cbuf);
        
        BufferUtils.setInBuffer(getSpecular(), _cbuf, 0);
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, _cbuf);
        
        BufferUtils.setInBuffer(getEmissive(), _cbuf, 0);
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_EMISSION, _cbuf);
        
        GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, getShininess());
    }
    
    /** The type of color material mapping (GL_AMBIENT, GL_DIFFUSE, etc.), or
     * -1 for none. */
    protected int _type;
    
    /** A color buffer to reuse. */
    protected FloatBuffer _cbuf = BufferUtils.createColorBuffer(1);
    
    /** Variables that track the context's current state so that we don't make
     * unnecessary state changes (the initial values are OpenGL's defaults). */
    protected static boolean _currentColorMaterial = false;
    protected static int _currentType = GL11.GL_AMBIENT_AND_DIFFUSE;
}
