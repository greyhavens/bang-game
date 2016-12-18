//
// $Id$

package com.threerings.bang.bounty.client;

import com.jme.renderer.Renderer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BStyleSheet;
import com.jmex.bui.Label;
import com.jmex.bui.text.BTextFactory;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.Star;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;

/**
 * Displays a bounty entry and pretends to be a selectable icon.
 */
public class BountyListEntry extends SelectableIcon
{
    public static final Dimension ICON_SIZE = new Dimension(508, 90);

    public BountyConfig config;

    public BountyListEntry (BangContext ctx, BountyConfig config)
    {
        setStyleClass("bounty_list_entry");
        setPreferredSize(ICON_SIZE);

        _ctx = ctx;
        this.config = config;
        _lock = ctx.loadImage("ui/office/lock.png");

        // create our various labels
        _labels = new Label[] {
            new Label(this), new Label(this), new Label(this), new Label(this), new Label(this),
            new Label(this)
        };
        MessageBundle msgs = ctx.getMessageManager().getBundle(OfficeCodes.OFFICE_MSGS);
        _labels[0].setText(config.title);
        _labels[1].setText(msgs.get("m.list_reward"));
        _labels[2].setText(config.reward.scrip + (config.reward.hasExtraReward() ? "+" : ""));
        _labels[2].setFit(BLabel.Fit.SCALE);
        _labels[2].setIcon(BangUI.scripIcon);
        _labels[3].setText(msgs.get("m.list_games", String.valueOf(config.games.size())));
        _labels[4].setText(msgs.xlate(Star.getName(config.difficulty)));
        _labels[5].setText(msgs.get("m.last_unlock"));

        // create our outlaw view (we'll configure it lazily)
        _oview = new OutlawView(ctx, 0.5f);
        _completed = config.isCompleted(_ctx.getUserObject());
        _locked = !config.isAvailable(_ctx.getUserObject());
        int townIdx = BangUtil.getTownIndex(config.townId);
        _lastLocked = _locked && (config.order == BountyConfig.LAST_TOWN_BOUNTY) &&
            (config.type == BountyConfig.Type.TOWN ||
             _ctx.getUserObject().holdsStar(townIdx, config.difficulty));
    }

    @Override // from BComponent
    protected void wasAdded ()
    {
        super.wasAdded();
        for (Label label : _labels) {
            label.wasAdded();
        }
        _lock.reference();

        // start resolving our outlaw now that we're added (this will NOOP after the first time)
        _oview.reference();
        _oview.setOutlaw(_ctx, config.getOutlaw(), _completed, config.showBars);
    }

    @Override // from BComponent
    protected void wasRemoved ()
    {
        super.wasRemoved();

        for (Label label : _labels) {
            label.wasRemoved();
        }
        _lock.release();
        _oview.release();
    }

    @Override // from BComponent
    protected void configureStyle (BStyleSheet style)
    {
        super.configureStyle(style);

        // force a temporary switch of our style class to obtain our alt factories
        String oldStyle = _styleClass;
        _styleClass = "bounty_list_details";
        _altfacts = new BTextFactory[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            _altfacts[ii] = style.getTextFactory(this, getStatePseudoClass(ii));
        }
        _styleClass = oldStyle;
    }

    @Override // from BComponent
    protected void layout ()
    {
        super.layout();

        for (int ii = 0; ii < _labels.length; ii++) {
            _labels[ii].layout(Insets.ZERO_INSETS, LABEL_RECTS[ii].width, LABEL_RECTS[ii].height);
        }
    }

    @Override // from BComponent
    protected BTextFactory getTextFactory (Label forLabel)
    {
        return (forLabel == _labels[3] || forLabel == _labels[4] || forLabel == _labels[5]) ?
            _altfacts[getState()] : getTextFactory();
    }

    @Override // from BComponent
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        if (_locked) {
            int lx = LOCK_RECT.x + (LOCK_RECT.width - _lock.getWidth())/2;
            int ly = LOCK_RECT.y + (LOCK_RECT.height - _lock.getHeight())/2;
            _lock.render(renderer, lx, ly, _alpha);
        }

        for (int ii = 0; ii < _labels.length; ii++) {
            if (_completed && (ii == 1 || ii == 2)) {
                continue; // don't show the reward for completed bounties
            }
            if (!_lastLocked && (ii == 5)) {
                continue; // don't show the last bounty locked message
            }
            _labels[ii].render(renderer, LABEL_RECTS[ii].x, LABEL_RECTS[ii].y,
                               LABEL_RECTS[ii].width, LABEL_RECTS[ii].height, _alpha);
        }

        _oview.render(renderer, FRAME_LOC.x, FRAME_LOC.y, _alpha);
    }

    protected BangContext _ctx;
    protected boolean _locked, _completed, _lastLocked;
    protected BImage _lock;
    protected Label[] _labels;
    protected BTextFactory[] _altfacts;
    protected OutlawView _oview;

    protected static final Point FRAME_LOC = new Point(40, 5);
    protected static final Rectangle LOCK_RECT = new Rectangle(2, 0, 38, 88);
    protected static final Rectangle[] LABEL_RECTS = {
        new Rectangle(125, 51, 200, 30),
        new Rectangle(306, 51, 120, 30),
        new Rectangle(420, 51, 65, 30),
        new Rectangle(125, 26, 100, 18),
        new Rectangle(125, 7, 100, 18),
        new Rectangle(316, 7, 200, 37),
    };
}
