//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import java.net.URL;

import com.jme.image.Texture;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.math.Vector3f;
import com.jme.scene.Text;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;

import com.jmex.bui.util.Dimension;

/**
 * Creates instances of {@link BText} for text rendering.
 */
public class JMEBitmapTextFactory extends BTextFactory
{
    /**
     * Creates a bitmap text factory with the specified font URL and the
     * supplied per-character width and height.
     */
    public JMEBitmapTextFactory (URL font, int width, int height)
    {
        _width = width;
        _height = height;

        // create a texture from our font image
        Texture texture = TextureManager.loadTexture(
            font, Texture.MM_NONE, Texture.FM_NEAREST);
        _tstate = DisplaySystem.getDisplaySystem().getRenderer().
            createTextureState();
        _tstate.setEnabled(true);
        _tstate.setTexture(texture);

        // create an alpha state that we'll use to blend our font over the
        // background
        _astate = DisplaySystem.getDisplaySystem().getRenderer().
            createAlphaState();
        _astate.setBlendEnabled(true);
        _astate.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        _astate.setDstFunction(AlphaState.DB_ONE);
        _astate.setEnabled(true);
    }

    // documentation inherited
    public int getHeight ()
    {
        return 16; // JME text is all hardcoded to 16 pixels presently
    }

    // documentation inherited
    public BText createText (final String text, final ColorRGBA color,
                             int effect, int effectSize, ColorRGBA effectColor,
                             boolean useAdvance)
    {
        // compute the dimensions of this text
        final Dimension dims = new Dimension(text.length() * _width, _height);

        // create a text object to display it
        final Text tgeom = new Text("text", text);
        tgeom.setCullMode(Text.CULL_NEVER);
        tgeom.setTextureCombineMode(TextureState.REPLACE);
        tgeom.setRenderState(_tstate);
        tgeom.setRenderState(_astate);
        tgeom.setTextColor(new ColorRGBA(color));
        
        // wrap it all up in the right object
        return new BText() {
            public int getLength () {
                return text.length();
            }
            public Dimension getSize () {
                return dims;
            }
            public int getHitPos (int x, int y) {
                return (x-5)/10;
            }
            public int getCursorPos (int index) {
                // JME characters are hardcoded to 10x16
                return 10 * index;
            }
            public void wasAdded () {
            }
            public void wasRemoved () {
            }
            public void render (Renderer renderer, int x, int y, float alpha) {
                x -= 4; // TEMP: handle Text offset bug
                tgeom.setLocalTranslation(new Vector3f(x, y, 0));
                tgeom.getTextColor().a = alpha * color.a;
                renderer.draw(tgeom);
            }
        };
    }

    // documentation inherited
    public BText[] wrapText (
        String text, ColorRGBA color, int effect, int effectSize, 
        ColorRGBA effectColor, int maxWidth /*, int[] remain */)
    {
        // determine how many characters we can fit (note: JME currently
        // assumes all text is width 10 so we propagate that hack)
        int maxChars = maxWidth / 10;

        // deal with the easy case
        if (text.length() <= maxChars) {
//             remain[0] = 0;
            return new BText[] { createText(text, color) };
        }

        // scan backwards from maxChars looking for whitespace
        for (int ii = maxChars; ii >= 0; ii--) {
            if (Character.isWhitespace(text.charAt(ii))) {
                // subtract one to absorb the whitespace that we used to wrap
//                 remain[0] = (text.length() - ii - 1);
                return new BText[] { createText(text.substring(0, ii), color) };
            }
        }

        // ugh, found no whitespace, just hard-wrap at maxChars
//         remain[0] = (text.length() - maxChars);
        return new BText[] { createText(text.substring(0, maxChars), color) };
    }

    protected int _width, _height;
    protected TextureState _tstate;
    protected AlphaState _astate;
}
