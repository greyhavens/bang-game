//
// $Id$

package com.threerings.bang.game.client;

import java.util.HashSet;

import com.jme.renderer.Renderer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays bounty game criteria during a game.
 */
public class InGameBountyView extends BWindow
    implements AttributeChangeListener, SetListener
{
    public InGameBountyView (BangContext ctx, BangConfig config, BangObject bangobj)
    {
        super(BangUI.stylesheet, new TableLayout(2, 5, 15));
        setStyleClass("bounty_req_window");
        _ctx = ctx;
        _config = config;
        _bangobj = bangobj;

        _current = new BLabel[config.criteria.size()];
        int idx = 0;
        for (Criterion crit : config.criteria) {
            String text = ctx.xlate(GameCodes.GAME_MSGS, crit.getDescription());
            add(new BLabel(text, "bounty_req_descrip"));
            BContainer pair = GroupLayout.makeHBox(GroupLayout.LEFT);
            text = ctx.xlate(GameCodes.GAME_MSGS, "m.bounty_current");
            pair.add(new BLabel(text, "bounty_req_descrip"));
            pair.add(_current[idx++] = new BLabel("", "bounty_req_value"));
            add(pair);
        }

        if (bangobj.critStats != null) {
            updateCurrent();
        }

        bangobj.addListener(this);
    }

    public void setRank (int rank)
    {
        System.err.println("Setting rank " + rank);
        _rank = rank;
        updateCurrent();
    }

    // from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(BangObject.CRIT_STATS)) {
            updateCurrent();
        }
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(BangObject.CRIT_STATS)) {
            updateCurrent();
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(BangObject.CRIT_STATS)) {
            updateCurrent();
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        // shouldn't happen
    }

    @Override // from BWindow
    public BComponent getHitComponent (int mx, int my)
    {
        return null; // don't intercept mouse clicks
    }

    @Override // from BWindow
    protected void renderBackground (Renderer renderer)
    {
        getBackground().render(renderer, 0, 0, _width, _height, 0.5f);
    }

    protected void updateCurrent ()
    {
        int idx = 0;
        for (Criterion crit : _config.criteria) {
            _current[idx++].setText(
                _ctx.xlate(GameCodes.GAME_MSGS, crit.getCurrentState(_bangobj, _rank)));
        }
    }

    protected BangContext _ctx;
    protected BangConfig _config;
    protected BangObject _bangobj;
    protected int _rank;

    protected BLabel[] _current;
}
