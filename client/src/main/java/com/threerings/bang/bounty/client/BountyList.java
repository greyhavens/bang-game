//
// $Id$

package com.threerings.bang.bounty.client;

import java.util.ArrayList;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BGroup;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.StringUtil;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.util.MessageBundle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Shop;
import com.threerings.bang.data.Star;
import com.threerings.bang.data.StatType;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;

/**
 * Displays a list of available bounties and an optional tip.
 */
public class BountyList extends BContainer
{
    public BountyList (BangContext ctx, BountyConfig.Type type, BountyDetailView detail)
    {
        super(BGroup.vertStretch().offStretch().make());
        _ctx = ctx;

        BContainer tipBox = BGroup.horizStretch().makeBox();
        tipBox.setStyleClass("bounty_list_tipbox");
        tipBox.add(_tip = new BLabel("", "bounty_list_tip"));
        tipBox.add(_tipButton = new BButton(""), GroupLayout.FIXED);
        _tipButton.setVisible(false);
        add(tipBox, GroupLayout.FIXED);
        add(new Spacer(0, 13), GroupLayout.FIXED);
        add(_list = new IconPalette(detail, 1, BOUNTIES_PER_PAGE, BountyListEntry.ICON_SIZE, 1));
        _list.setAllowsEmptySelection(false);

        // enumerate our available bounties
        PlayerObject user = _ctx.getUserObject();
        ArrayList<BountyConfig> bounties = BountyConfig.getBounties(user.townId, type);

        // determine how many are unlocked/complete and decide which page on which to start
        int unlocked = 0, completed = 0;
        _selidx = bounties.size();
        Star.Difficulty firstUnavail = null, highestAvail = null;
        for (int ii = 0; ii < bounties.size(); ii++) {
            BountyConfig config = bounties.get(ii);
            if (user.stats.containsValue(StatType.BOUNTIES_COMPLETED, config.ident)) {
                completed++;

            } else if (config.isAvailable(user)) {
                unlocked++;
                // select the first playable bounty
                _selidx = Math.min(_selidx, ii);
                // note the highest available difficulty
                highestAvail = config.difficulty;

            } else if (type == BountyConfig.Type.TOWN && config.difficulty != highestAvail) {
                continue; // don't show town bounties that are not yet unlocked

            } else if (firstUnavail != null && config.difficulty != firstUnavail) {
                // only show one difficulty level beyond what's unlocked for most wanted bounties
                continue;

            } else if (firstUnavail == null && config.difficulty != highestAvail) {
                firstUnavail = config.difficulty;
            }
            _list.addIcon(new BountyListEntry(ctx, config));
        }

        // if we had no playable bounties, select the first
        if (_selidx == bounties.size()) {
            _selidx = 0;
        }

        // configure the tip based on what they've unlocked and completed
        String tip, but = null;
        Shop where = null;
        if (completed == bounties.size()) {
            tip = "all_complete_" + StringUtil.toUSLowerCase(type.toString());
            tip = MessageBundle.compose(tip, "m." + user.townId);
            if (type == BountyConfig.Type.MOST_WANTED) {
                but = "m.tip_to_station";
                where = Shop.STATION;
            }

        } else if (unlocked + completed == bounties.size()) {
            tip = "all_unlocked";

        } else if (firstUnavail != null) {
            tip = StringUtil.toUSLowerCase(firstUnavail.toString());
            but = "m.tip_to_store";
            where = Shop.STORE;

        } else {
            tip = StringUtil.toUSLowerCase(type.toString());
            if (type == BountyConfig.Type.TOWN) {
                // town bounty tips are custom per down (because ITP has only easy bounties)
                tip += "_" + user.townId;
            }
        }
        _tip.setText(_ctx.xlate(OfficeCodes.OFFICE_MSGS, "m.tip_" + tip));
        if (but != null) {
            _tipButton.setText(_ctx.xlate(OfficeCodes.OFFICE_MSGS, but));
            _tipButton.setVisible(true);
            final Shop fwhere = where;
            _tipButton.addListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    _ctx.getBangClient().goTo(fwhere);
                }
            });
        }
    }

    @Override // from BComponent
    protected void wasAdded ()
    {
        super.wasAdded();

        // select the bounty we noted for selection
        if (_selidx >= 0) {
            _list.displayPage(_selidx/BOUNTIES_PER_PAGE);
            _list.getIcon(_selidx).setSelected(true);
        }
    }

    protected BangContext _ctx;
    protected BLabel _tip;
    protected BButton _tipButton;
    protected IconPalette _list;
    protected int _selidx;

    protected static final int BOUNTIES_PER_PAGE = 5;
}
