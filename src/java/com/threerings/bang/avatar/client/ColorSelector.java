//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;

import com.jme.image.Image;
import com.jme.renderer.Renderer;

import com.jmex.bui.BComponent;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BStyleSheet;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.util.RenderUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;
import com.threerings.util.RandomUtil;

import com.threerings.bang.util.BangContext;

/**
 * Displays a popup menu in which the user can select a particular colorization
 * from a colorization class.
 */
public class ColorSelector extends BComponent
{
    public ColorSelector (BangContext ctx, String colorClass)
    {
        setStyleClass("color_selector");

        ColorPository cpos = ctx.getAvatarLogic().getColorPository();
        ColorPository.ColorRecord[] colors = cpos.enumerateColors(colorClass);

        int dy = 0;
        if (colorClass.endsWith("_s")) {
            dy = 24;
        } else if (colorClass.endsWith("_t")) {
            dy = 48;
        }

        BufferedImage dots =
            ctx.getImageCache().getBufferedImage("ui/barber/swatch_dots.png");
        BufferedImage circle = dots.getSubimage(0, dy, 24, 24);
        BufferedImage selcircle = dots.getSubimage(24, dy, 24, 24);
        BufferedImage square = dots.getSubimage(48, dy, 24, 24);
        BufferedImage selsquare = dots.getSubimage(72, dy, 24, 24);

        _swatches = new Swatch[colors.length];
        for (int ii = 0; ii < _swatches.length; ii++) {
            Swatch swatch = _swatches[ii] = new Swatch();
            swatch.colorId = colors[ii].colorId;
            swatch.zation = colors[ii].getColorization();
            swatch.circle = ctx.getImageCache().createImage(
                ImageUtil.recolorImage(circle, swatch.zation), true);
            swatch.selectedCircle = ctx.getImageCache().createImage(
                ImageUtil.recolorImage(selcircle, swatch.zation), true);
            swatch.square = ctx.getImageCache().createImage(
                ImageUtil.recolorImage(square, swatch.zation), true);
            swatch.selectedSquare = ctx.getImageCache().createImage(
                ImageUtil.recolorImage(selsquare, swatch.zation), true);
        }

        Arrays.sort(_swatches, new Comparator<Swatch>() {
            public int compare (Swatch a, Swatch b) {
                return a.colorId - b.colorId;
            }
        });

        if (_swatches.length > 0) {
            setSelectedColor(RandomUtil.getInt(_swatches.length));
        }
    }

    public int getSelectedColor ()
    {
        return _selidx == -1 ? -1 : _swatches[_selidx].colorId;
    }

    public Colorization getSelectedColorization ()
    {
        return _selidx == -1 ? null : _swatches[_selidx].zation;
    }

    public void setSelectedColor (int selidx)
    {
        _selidx = selidx;
    }

    public void setSelectedColorId (int colorId)
    {
        for (int ii = 0; ii < _swatches.length; ii++) {
            if (_swatches[ii].colorId == colorId) {
                _selidx = ii;
                break;
            }
        }
    }

    // documentation inherited
    public void dispatchEvent (BEvent event)
    {
        super.dispatchEvent(event);

        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_PRESSED:
                if (_menu == null) {
                    _menu = new BPopupMenu(getWindow(), true);
                    _menu.setStyleClass("color_selector_popup");
                    _menu.addListener(_listener);
                    _menu.addMenuItem(new SwatchMenuItem(_selidx, true));
                    for (int ii = 0; ii < _swatches.length; ii++) {
                        _menu.addMenuItem(new SwatchMenuItem(ii, false));
                    }
                } else {
                    ((SwatchMenuItem)_menu.getComponent(0)).index = _selidx;
                }
                _menu.popup(getAbsoluteX(), getAbsoluteY()+35, false);
                break;
            }
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        if (_selidx >= 0) {
            RenderUtil.blendState.apply();
            RenderUtil.renderImage(_swatches[_selidx].circle, 4, 5);
        }
    }

    protected static class Swatch
    {
        public int colorId;
        public Colorization zation;
        public Image circle;
        public Image selectedCircle;
        public Image square;
        public Image selectedSquare;
    }

    protected class SwatchMenuItem extends BMenuItem
    {
        public int index;

        public SwatchMenuItem (int index, boolean circle)
        {
            super(null, null, "");
            this.index = index;
            _circle = circle;
            setIcon(new BlankIcon(_circle ? 32 : 24, 36));
        }

        protected void configureStyle (BStyleSheet style)
        {
            super.configureStyle(style);

            // set up our backgrounds
            Swatch swatch = _swatches[index];
            _backgrounds[DEFAULT] = new ImageBackground(
                ImageBackground.CENTER_XY, _circle ?
                swatch.circle : swatch.square);
            _backgrounds[HOVER] = new ImageBackground(
                ImageBackground.CENTER_XY, _circle ?
                swatch.selectedCircle : swatch.selectedSquare);
        }

        protected boolean _circle;
    }

    protected ActionListener _listener = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            int idx = ((SwatchMenuItem)event.getSource()).index;
            setSelectedColor(idx);
            dispatchEvent(new ActionEvent(
                              ColorSelector.this, event.getWhen(),
                              event.getModifiers(), "selectionChanged"));
        }
    };

    protected int _selidx = -1;
    protected Swatch[] _swatches;
    protected BPopupMenu _menu;
}
