//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.jme.renderer.Renderer;

import com.jmex.bui.BComponent;
import com.jmex.bui.BImage;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BStyleSheet;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BlankIcon;

import com.samskivert.util.RandomUtil;

import com.threerings.media.image.ColorPository.ColorRecord;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.presents.dobj.DObject;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.ColorConstraints;
import com.threerings.bang.client.BangUI;

/**
 * Displays a popup menu in which the user can select a particular colorization
 * from a colorization class.
 */
public class ColorSelector extends BComponent
{
    public ColorSelector (
        BangContext ctx, String colorClass, ActionListener listener)
    {
        this(ctx, colorClass, ctx.getUserObject(), listener);
    }

    public ColorSelector (
        BangContext ctx, String colorClass, DObject entity, ActionListener listener)
    {
        setStyleClass("color_selector");
        addListener(listener);
        _colorClass = colorClass;

        ArrayList<ColorRecord> colors =
            ColorConstraints.getAvailableColors(
                ctx.getAvatarLogic().getColorPository(), colorClass,
                entity);

        int dy = 0; // HAIR
        if (colorClass.equals("buckle_p")) {
            dy = 96;
        } else if (colorClass.equals("buckle_back_p")) {
            dy = 144;
        } else if (colorClass.equals("buckle_back_s")) {
            dy = 120;
        } else if (colorClass.equals("armadillo_t")) {
            dy = 216;
        } else if (colorClass.equals("buzzard_p")) {
            dy = 168;
        } else if (colorClass.equals("raccoon_s")) {
            dy = 192;
        } else if (colorClass.endsWith("_s")) {
            dy = 24;
        } else if (colorClass.endsWith("_t")) {
            dy = 48;
        } else if (colorClass.equals(AvatarLogic.SKIN)) {
            dy = 72;
        }

        BufferedImage dots =
            ctx.getImageCache().getBufferedImage("ui/barber/swatch_dots.png");
        BufferedImage circle = dots.getSubimage(0, dy, 24, 24);
        BufferedImage selcircle = dots.getSubimage(24, dy, 24, 24);
        BufferedImage square = dots.getSubimage(48, dy, 24, 24);
        BufferedImage selsquare = dots.getSubimage(72, dy, 24, 24);

        int scount = colors.size();
        _swatches = new Swatch[scount];
        for (int ii = 0; ii < _swatches.length; ii++) {
            Swatch swatch = _swatches[ii] = new Swatch();
            swatch.colorId = colors.get(ii).colorId;
            swatch.zation = colors.get(ii).getColorization();
            swatch.circle = new BImage(
                ImageUtil.recolorImage(circle, swatch.zation));
            swatch.selectedCircle = new BImage(
                ImageUtil.recolorImage(selcircle, swatch.zation));
            swatch.square = new BImage(
                ImageUtil.recolorImage(square, swatch.zation));
            swatch.selectedSquare = new BImage(
                ImageUtil.recolorImage(selsquare, swatch.zation));
        }

        Arrays.sort(_swatches, new Comparator<Swatch>() {
            public int compare (Swatch a, Swatch b) {
                return a.colorId - b.colorId;
            }
        });

        // start with a randomly selected color
        pickRandom();
    }

    public void pickRandom ()
    {
        if (_swatches.length > 0) {
            setSelectedColor(RandomUtil.getInt(_swatches.length));
        }
    }

    public String getColorClass ()
    {
        return _colorClass;
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
    public boolean dispatchEvent (BEvent event)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_PRESSED:
                if (_menu == null) {
                    _menu = new BPopupMenu(getWindow(), true);
                    _menu.setStyleClass("color_selector_popup");
                    _menu.addListener(_listener);
                    _menu.setLayer(BangUI.POPUP_MENU_LAYER);
                    _menu.addMenuItem(new SwatchMenuItem(_selidx, true));
                    for (int ii = 0; ii < _swatches.length; ii++) {
                        _menu.addMenuItem(new SwatchMenuItem(ii, false));
                    }
                } else {
                    ((SwatchMenuItem)_menu.getComponent(0)).index = _selidx;
                }
                _menu.popup(getAbsoluteX(), getAbsoluteY()+35, false);
                return true;
            }
        }

        return super.dispatchEvent(event);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // we need to manage our circle image because we render it by hand
        for (int ii = 0; ii < _swatches.length; ii++) {
            _swatches[ii].circle.reference();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // we need to manage our circle image because we render it by hand
        for (int ii = 0; ii < _swatches.length; ii++) {
            _swatches[ii].circle.release();
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        if (_selidx >= 0) {
            _swatches[_selidx].circle.render(renderer, 4, 5, _alpha);
        }
    }

    protected static class Swatch
    {
        public int colorId;
        public Colorization zation;
        public BImage circle;
        public BImage selectedCircle;
        public BImage square;
        public BImage selectedSquare;
    }

    protected class SwatchMenuItem extends BMenuItem
    {
        public int index;

        public SwatchMenuItem (int index, boolean circle)
        {
            super(null, null, "");
            setStyleClass("color_selector_item");
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

    protected String _colorClass;
    protected int _selidx = -1;
    protected Swatch[] _swatches;
    protected BPopupMenu _menu;
}
