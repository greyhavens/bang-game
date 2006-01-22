//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.image.Image;
import com.jme.renderer.Renderer;
import com.jmex.bui.BComponent;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.util.RenderUtil;

import com.threerings.bang.util.BangContext;

/**
 * Handles a sidebar of tabs in the hacked up wacky way that we've got to deal
 * with.
 */
public class HackyTabs extends BComponent
{
    public HackyTabs (BangContext ctx, String imgpref, String[] tabs,
                      int height, int border)
    {
        _ctx = ctx;
        _theight = height;
        _tborder = border;

        addListener(_mlistener);

        // load up our tab images
        _tabs = new Image[tabs.length];
        for (int ii = 0; ii < tabs.length; ii++) {
            _tabs[ii] = _ctx.loadImage(imgpref + tabs[ii] + ".png");
        }
    }

    public void selectTab (int index)
    {
        if (_selidx != index) {
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
        // start with the top tab selected
        selectTab(0);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        // clear this so that we properly reselect tab zero if we're readded
        _selidx = -1;
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        RenderUtil.blendState.apply();
        int iy = getHeight() - _theight*_selidx - _tabs[_selidx].getHeight();
        RenderUtil.renderImage(_tabs[_selidx], 0, iy);
    }

    protected MouseAdapter _mlistener = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            int mx = event.getX() - getAbsoluteX(),
                my = getHeight() - (event.getY() - getAbsoluteY());
            int tabidx = (my-_tborder)/_theight;
            if (tabidx >= 0 && tabidx < _tabs.length) {
                selectTab(tabidx);
            }
        }
    };

    protected BangContext _ctx;
    protected int _theight, _tborder;
    protected Image[] _tabs;
    protected int _selidx = -1;
}
