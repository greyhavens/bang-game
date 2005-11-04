//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BIcon;

import com.threerings.media.image.ColorPository;

import com.threerings.bang.util.BangContext;

/**
 * Displays a popup menu in which the user can select a particular colorization
 * from a colorization class.
 */
public class ColorSelector extends BComboBox
{
    public ColorSelector (BangContext ctx, String colorClass)
    {
        ColorPository cpos = ctx.getAvatarLogic().getColorPository();
        ColorPository.ColorRecord[] colors = cpos.enumerateColors(colorClass);

        SwatchIcon[] icons = new SwatchIcon[colors.length];
        for (int ii = 0; ii < icons.length; ii++) {
            Color jcolor = colors[ii].getColorization().getColorizedRoot();
            ColorRGBA color = new ColorRGBA(jcolor.getRed()/255f,
                                            jcolor.getGreen()/255f,
                                            jcolor.getBlue()/255f, 1f);
            icons[ii] = new SwatchIcon(colors[ii].colorId, color);
        }

        if (icons.length > 0) {
            setItems(icons);
            selectItem(0);
        }
    }

    public int getSelectedColor ()
    {
        return _selidx == -1 ? -1 : ((SwatchIcon)getSelectedItem()).colorId;
    }

    protected static class SwatchIcon extends BIcon
    {
        public int colorId;

        public SwatchIcon (int colorId, ColorRGBA color) {
            this.colorId = colorId;
            _color = color;
        }

        public int getWidth () {
            return SIZE;
        }

        public int getHeight () {
            return SIZE;
        }

        public void render (Renderer renderer, int x, int y) {
            super.render(renderer, x, y);
            GL11.glColor4f(_color.r, _color.g, _color.b, _color.a);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x + SIZE, y);
            GL11.glVertex2f(x + SIZE, y + SIZE);
            GL11.glVertex2f(x, y + SIZE);
            GL11.glEnd();
        }

        protected ColorRGBA _color;
    }

    protected static final int SIZE = 16;
}
