//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.renderer.Renderer;

import com.jmex.bui.BComponent;
import com.jmex.bui.BImage;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;

/**
 * Handles a sidebar of tabs in the hacked up wacky way that we've got to deal
 * with.
 */
public class HackyTabs extends BComponent
{
    public HackyTabs (BangContext ctx, boolean vertical, String imgpref,
                      String[] tabs, int size, int border)
    {
        _ctx = ctx;
        _vertical = vertical;
        _tsize = size;
        _tborder = border;

        addListener(_mlistener);

        // load up our tab images
        _tabs = new BImage[tabs.length];
        for (int ii = 0; ii < tabs.length; ii++) {
            _tabs[ii] = _ctx.loadImage(imgpref + tabs[ii] + ".png");
        }
    }

    /**
     * Changes the default tab from zero to the specified index.
     */
    public void setDefaultTab (int index)
    {
        _deftab = index;
    }

    public void selectTab (int index)
    {
        selectTab(index, isAdded());
    }

    public void selectTab (int index, boolean audioFeedback)
    {
        if (_selidx != index) {
            if (audioFeedback) {
                BangUI.play(BangUI.FeedbackSound.TAB_SELECTED);
            }
            tabSelected(_selidx = index);
        }
    }

    protected void tabSelected (int index)
    {
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // select the default tab if one is not alrady selected
        if (_selidx == -1) {
            selectTab(_deftab, false);
        }

        // reference our tab images
        for (int ii = 0; ii < _tabs.length; ii++) {
            _tabs[ii].reference();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // release our tab images
        for (int ii = 0; ii < _tabs.length; ii++) {
            _tabs[ii].release();
        }
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        int ix = 0, iy = 0;
        if (_vertical) {
            iy = getHeight() - _tsize*_selidx - _tabs[_selidx].getHeight();
        } else {
            ix = _tsize*_selidx;
        }
        _tabs[_selidx].render(renderer, ix, iy, _alpha);
    }

    protected MouseAdapter _mlistener = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            int mx = event.getX() - getAbsoluteX(),
                my = getHeight() - (event.getY() - getAbsoluteY());
            int tabidx = ((_vertical ? my : mx) - _tborder) / _tsize;
            if (tabidx >= 0 && tabidx < _tabs.length) {
                selectTab(tabidx);
            }
        }
    };

    protected BangContext _ctx;
    protected boolean _vertical;
    protected int _tsize, _tborder;
    protected BImage[] _tabs;
    protected int _deftab, _selidx = -1;
}
