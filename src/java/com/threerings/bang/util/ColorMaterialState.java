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
        boolean refresh = false;
        if (_type == -1 && _currentColorMaterial) {
            GL11.glDisable(GL11.GL_COLOR_MATERIAL);
            _currentColorMaterial = false;
            refresh = true;
            
        } else if (_type != -1) {
            if (_type != _currentType) {
                GL11.glColorMaterial(GL11.GL_FRONT, _currentType = _type);
                refresh = true;
            }
            if (!_currentColorMaterial) {
                GL11.glEnable(GL11.GL_COLOR_MATERIAL);
                _currentColorMaterial = true;
                refresh = true;
            }
        }
        
        // set material parameters
        ColorRGBA ambient = getAmbient(), diffuse = getDiffuse(),
            specular = getSpecular(), emissive = getEmissive();
        float shininess = getShininess();
        if (_type != GL11.GL_AMBIENT_AND_DIFFUSE &&
            _type != GL11.GL_AMBIENT &&
            (refresh || !ambient.equals(currentAmbient))) {
            BufferUtils.setInBuffer(ambient, _cbuf, 0);
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, _cbuf);
            currentAmbient.set(ambient);
        }
        if (_type != GL11.GL_AMBIENT_AND_DIFFUSE &&
            _type != GL11.GL_DIFFUSE &&
            (refresh || !diffuse.equals(currentDiffuse))) {
            BufferUtils.setInBuffer(diffuse, _cbuf, 0);
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, _cbuf);
            currentDiffuse.set(diffuse);
        }
        if (_type != GL11.GL_SPECULAR &&
            (refresh || !specular.equals(currentSpecular))) {
            BufferUtils.setInBuffer(specular, _cbuf, 0);
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, _cbuf);
            currentSpecular.set(specular);
        }
        if (_type != GL11.GL_EMISSION &&
            (refresh || !emissive.equals(currentEmissive))) {
            BufferUtils.setInBuffer(emissive, _cbuf, 0);
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_EMISSION, _cbuf);
            currentEmissive.set(emissive);
        }
        if (refresh || shininess != currentShininess) {
            GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS,
                currentShininess = shininess);
        }
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
    static {
        currentAmbient.set(defaultAmbient);
        currentDiffuse.set(defaultDiffuse);
        currentSpecular.set(defaultSpecular);
        currentEmissive.set(defaultEmissive);
        currentShininess = defaultShininess;
    }
}
