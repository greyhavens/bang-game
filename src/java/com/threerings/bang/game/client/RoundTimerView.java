//
// $Id$

package com.threerings.bang.game.client;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.jme.math.FastMath;
import com.jme.renderer.Renderer;

import com.jmex.bui.BImage;
import com.jmex.bui.BWindow;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.util.BasicContext;

/**
 * Displays the round countdown timer.
 */
public class RoundTimerView extends BWindow
    implements AttributeChangeListener
{
    public RoundTimerView (BasicContext ctx)
    {
        super(ctx.getStyleSheet(), new BorderLayout());
        setStyleClass("round_timer");
        setLayer(1);

        _ctx = ctx;
        _pin = new ImageIcon(ctx.loadImage("ui/gauge/pin.png"));
        _needle = ctx.getImageCache().getBufferedImage("ui/gauge/needle.png");
        _overimg = new BufferedImage(78, 78, BufferedImage.TYPE_INT_ARGB);

        setStatus(0, 0, 0);
    }

    public void init (BangObject bangobj)
    {
        _bangobj = bangobj;
        _bangobj.addListener(this);
    }

    public void setStatus (int tick, int lastTick, int duration)
    {
        if (duration > 0) {
            _skipDelta = Math.round((duration - lastTick - 1) * RANGE /
                                    (float)duration);
            _tickDelta = Math.round(tick * RANGE / (float)duration);
        } else {
            _skipDelta = 0;
            _tickDelta = 0;
        }

        int width = _overimg.getWidth(), height = _overimg.getHeight();
        Graphics2D gfx = _overimg.createGraphics();
        try {
            // clear out the old data
            gfx.setColor(CLEAR_COLOR);
            gfx.setComposite(AlphaComposite.SrcOut);
            gfx.fillRect(0, 0, width, height);

            int start = START_ANGLE;
            if (_skipDelta > 0) {
                // render a pie filling in our skipped region
                gfx.setColor(SKIP_COLOR);
                gfx.fillArc(0, 0, width, height, start, -_skipDelta);
                start -= _skipDelta;
            }

            if (_tickDelta > 0) {
                // render a pie filling in our ticked region
                gfx.setColor(TICK_COLOR);
                gfx.fillArc(0, 0, width, height, start, -_tickDelta);
                start -= _tickDelta;
            }

            // now set up an affine transform and render the needle
            float angle = -FastMath.PI * (start-88) / 180f;
            float dx = _needle.getWidth()/2f, dy = _needle.getHeight()/2f;
            AffineTransform xform =
                AffineTransform.getTranslateInstance(dx, dy);
            xform.concatenate(AffineTransform.getRotateInstance(angle));
            xform.concatenate(AffineTransform.getTranslateInstance(-dx, -dy));
            gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                 RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gfx.translate(2, 2);
            gfx.setComposite(AlphaComposite.SrcOver);
            gfx.drawImage(_needle, xform, null);

        } finally {
            gfx.dispose();
        }

        // now convert this into a JME displayable image
        _overlay = new ImageIcon(new BImage(_overimg));
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.STATE)) {
            if (_bangobj.state == BangObject.IN_PLAY) {
                setStatus(_bangobj.tick, _bangobj.lastTick, _bangobj.duration);
            } else {
                setStatus(0, 0, 0);
            }

        } else if (name.equals(BangObject.LAST_TICK)) {
            setStatus(_bangobj.tick, _bangobj.lastTick, _bangobj.duration);

        } else if (name.equals(BangObject.TICK)) {
            setStatus(_bangobj.tick, _bangobj.lastTick, _bangobj.duration);
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        _overlay.render(renderer, 10, 9);
        _pin.render(renderer, 12, 11);
    }

    protected BasicContext _ctx;
    protected BangObject _bangobj;
    protected ImageIcon _pin, _overlay;
    protected int _skipDelta, _tickDelta;

    protected BufferedImage _needle, _overimg;

    protected static final int START_ANGLE = -110;
    protected static final int END_ANGLE = -70;
    protected static final int RANGE = 360-(END_ANGLE-START_ANGLE);

    protected static final Color CLEAR_COLOR = new Color(255, 255, 255, 0);
    protected static final Color SKIP_COLOR = new Color(0x72, 0x2A, 0x72, 128);
    protected static final Color TICK_COLOR = new Color(0x00, 0x2A, 0x72, 128);
}
