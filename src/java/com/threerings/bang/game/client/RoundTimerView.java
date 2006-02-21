//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jme.math.FastMath;
import com.jme.renderer.Renderer;

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

        _ctx = ctx;
        _pin = new ImageIcon(ctx.loadImage("ui/gauge/pin.png"));
        _sneedle = ctx.getImageCache().getBufferedImage("ui/gauge/needle.png");
        _rneedle = new BufferedImage(_sneedle.getWidth(), _sneedle.getHeight(),
                                     BufferedImage.TYPE_INT_ARGB);

        setStatus(0, 0, 0);
    }

    public void init (BangObject bangobj)
    {
        _bangobj = bangobj;
        _bangobj.addListener(this);
    }

    public void setStatus (int tick, int lastTick, int duration)
    {
        _skipDelta = (duration - lastTick - 1) * RANGE / duration;
        _tickDelta = tick * RANGE / duration;

        int width = _rneedle.getWidth(), height = _rneedle.getHeight();
        Graphics2D gfx = _rneedle.createGraphics();
        try {
            // clear out the old data
            gfx.setColor(CLEAR_COLOR);
            gfx.fillRect(0, 0, width, height);

            if (tick > 0) {
                // render a pie filling in our skipped region
                gfx.setColor(SKIP_COLOR);
                float start = START_RADIANS;
                System.out.println(toDegrees(start) + "/" +
                                   -toDegrees(_skipDelta));
                gfx.fillArc(0, 0, width, height, toDegrees(start),
                            -toDegrees(_skipDelta));

                // render a pie filling in our ticked region
                gfx.setColor(TICK_COLOR);
                start += _skipDelta;
                System.out.println(toDegrees(start) + "/" +
                                   -toDegrees(_tickDelta));
                gfx.fillArc(0, 0, width, height, toDegrees(start),
                            -toDegrees(_tickDelta));
            }

        } finally {
            gfx.dispose();
        }

        // now convert this into a JME displayable image
        _needle = new ImageIcon(
            _ctx.getImageCache().createImage(_rneedle, true));
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

        _needle.render(renderer, 12, 11);
        _pin.render(renderer, 12, 11);
    }

    protected int toDegrees (float radians)
    {
        return Math.round(180 * radians / FastMath.PI);
    }

    protected BasicContext _ctx;
    protected BangObject _bangobj;
    protected ImageIcon _pin, _needle;
    protected float _skipDelta, _tickDelta;

    protected BufferedImage _sneedle, _rneedle;

    protected static final float START_RADIANS = -FastMath.PI/2;
    protected static final float END_RADIANS = -3*FastMath.PI/2;
    protected static final float RANGE = END_RADIANS-START_RADIANS;

    protected static final Color CLEAR_COLOR = new Color(255, 255, 255, 0);
    protected static final Color SKIP_COLOR = new Color(0x72, 0x2A, 0x72, 128);
    protected static final Color TICK_COLOR = new Color(0x00, 0x2A, 0x72, 128);
}
