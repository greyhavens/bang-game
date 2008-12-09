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

import com.threerings.jme.util.ImageCache;
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
        _overimg = ImageCache.createCompatibleImage(78, 78, true);

        setStatus(0, 0, 0);
    }

    public void init (BangObject bangobj)
    {
        _bangobj = bangobj;
        _bangobj.addListener(this);
    }

    public void setEndState (boolean almostOver)
    {
        _almostOver = almostOver;
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
                gfx.setColor((_almostOver ? ALMOST_OVER_COLOR : TICK_COLOR));
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

        if (isAdded() && _overlay != null) {
            _overlay.wasRemoved();
            _overlay = null;
        }

        // now convert this into a JME displayable image
        _overlay = new ImageIcon(new BImage(_overimg));
        if (isAdded()) {
            _overlay.wasAdded();
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.STATE)) {
            switch (_bangobj.state) {
            case BangObject.IN_PLAY:
            case BangObject.POST_ROUND:
            case BangObject.GAME_OVER:
                setStatus(_bangobj.tick, _bangobj.lastTick, _bangobj.duration);
                break;
            default:
                setStatus(0, 0, 0);
                break;
            }

        } else if (name.equals(BangObject.LAST_TICK)) {
            setStatus(_bangobj.tick, _bangobj.lastTick, _bangobj.duration);

        } else if (name.equals(BangObject.TICK)) {
            setStatus(_bangobj.tick, _bangobj.lastTick, _bangobj.duration);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        _pin.wasAdded();
        if (_overlay != null) {
            _overlay.wasAdded();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        _pin.wasRemoved();
        if (_overlay != null) {
            _overlay.wasRemoved();
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        _overlay.render(renderer, 17, 8, _alpha);
        _pin.render(renderer, 19, 12, _alpha);
    }

    protected BasicContext _ctx;
    protected BangObject _bangobj;
    protected ImageIcon _pin, _overlay;
    protected int _skipDelta, _tickDelta;
    protected boolean _almostOver;

    protected BufferedImage _needle, _overimg;

    protected static final int START_ANGLE = -110;
    protected static final int END_ANGLE = -70;
    protected static final int RANGE = 360-(END_ANGLE-START_ANGLE);

    protected static final Color CLEAR_COLOR = new Color(255, 255, 255, 0);
    protected static final Color SKIP_COLOR = new Color(0x72, 0x2A, 0x72, 89);
    protected static final Color TICK_COLOR = new Color(6, 107, 170, 89);
    protected static final Color ALMOST_OVER_COLOR = new Color(133, 6, 0, 89);
}
