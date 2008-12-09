//
// $Id$

package com.threerings.bang.game.client;

import java.util.HashMap;

import com.jme.renderer.Renderer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays bounty game criteria during a game.
 */
public class InGameBountyView extends BWindow
    implements AttributeChangeListener, SetListener<DSet.Entry>
{
    public InGameBountyView (BangContext ctx, BangConfig config, BangObject bangobj)
    {
        super(BangUI.stylesheet, new TableLayout(3, 5, 5));
        setStyleClass("bounty_req_window");
        _ctx = ctx;
        _config = config;
        _bangobj = bangobj;

        _descrip = new BLabel[config.criteria.size()];
        _state = new BLabel[config.criteria.size()];
        _current = new BLabel[config.criteria.size()];
        int idx = 0;
        for (Criterion crit : config.criteria) {
            String text = ctx.xlate(GameCodes.GAME_MSGS, crit.getDescription());
            add(_descrip[idx] = new BLabel(text, "bounty_req_status"));
            add(_state[idx] = new BLabel("", "bounty_req_status"));
            add(_current[idx++] = new BLabel("", "bounty_req_status"));
        }

        if (bangobj.critStats != null) {
            updateCurrent();
        }

        bangobj.addListener(this);
    }

    public void setRank (int rank)
    {
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
    public void entryAdded (EntryAddedEvent<DSet.Entry> event)
    {
        if (event.getName().equals(BangObject.CRIT_STATS)) {
            updateCurrent();
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
    {
        if (event.getName().equals(BangObject.CRIT_STATS)) {
            updateCurrent();
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> event)
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
        // render our background at 50% transparency
        getBackground().render(renderer, 0, 0, _width, _height, 0.5f);
    }

    protected void updateCurrent ()
    {
        int idx = 0;
        for (Criterion crit : _config.criteria) {
            String msg = _ctx.xlate(GameCodes.GAME_MSGS, crit.getDescription());
            Criterion.State state = crit.getCurrentState(_bangobj, _rank);
            // doris the hackasaurus!
            String[] colors = COLORS.get(state);
            msg.replaceAll("@=b", "@=b" + colors[0]);
            msg.replaceAll("#6C421B", colors[1]);
            _descrip[idx].setText(msg);

            msg = _ctx.xlate(GameCodes.GAME_MSGS,
                    StringUtil.toUSLowerCase("m.bounty_state_" + state));
            _state[idx].setText("@=" + colors[1] + "(" + msg + ")");

            switch (state) {
            case NOT_MET:
            case MET:
                msg = _ctx.xlate(GameCodes.GAME_MSGS, crit.getCurrentValue(_bangobj, _rank));
                _current[idx].setText("@=" + colors[2] + "(" + msg + ")");
                _current[idx].setIcon(null);
                break;

            case COMPLETE:
                _current[idx].setText("");
                _current[idx].setIcon(new ImageIcon(_ctx.loadImage("ui/icons/check_tiny.png")));
                break;

            case FAILED:
                _current[idx].setText("");
                _current[idx].setIcon(new ImageIcon(_ctx.loadImage("ui/icons/x_tiny.png")));
                break;
            }
            idx++;
        }
        if (isAdded()) {
            pack();
        }
    }

    protected BangContext _ctx;
    protected BangConfig _config;
    protected BangObject _bangobj;
    protected int _rank;

    protected BLabel[] _descrip, _state, _current;

    protected static final HashMap<Criterion.State,String[]> COLORS =
        new HashMap<Criterion.State,String[]>();
    static {
        COLORS.put(Criterion.State.NOT_MET, new String[] { "#FFFFFF", "#4C2602", "#FFFFFF" });
        COLORS.put(Criterion.State.MET, new String[] { "#FFFFFF", "#FFDB02", "#FFDB02" });
        COLORS.put(Criterion.State.COMPLETE, new String[] { "#FFDB02", "#FFDB02", "#FFDB02" });
        COLORS.put(Criterion.State.FAILED, new String[] { "#F02E24", "#F02E24", "#F02E24" });
    }
}
