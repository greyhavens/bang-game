//
// $Id$

package com.threerings.bang.bounty.client;

import java.util.ArrayList;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.util.MessageBundle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Star;
import com.threerings.bang.data.Stat;
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
        super(GroupLayout.makeVStretch());
        _ctx = ctx;

        add(_tip = new BLabel("", "bounty_list_tip"), GroupLayout.FIXED);
        add(new Spacer(0, 13), GroupLayout.FIXED);
        add(_list = new IconPalette(detail, 1, BOUNTIES_PER_PAGE, BountyListEntry.ICON_SIZE, 1));
        _list.setAllowsEmptySelection(false);

        // enumerate our available bounties
        PlayerObject user = _ctx.getUserObject();
        ArrayList<BountyConfig> bounties = BountyConfig.getTownBounties(user.townId, type);

        // determine how many are unlocked/complete and decide which page on which to start
        int unlocked = 0, completed = 0;
        _selidx = bounties.size();
        Star.Difficulty firstUnavail = null, highestAvail = null;
        for (int ii = 0; ii < bounties.size(); ii++) {
            BountyConfig config = bounties.get(ii);
            if (user.stats.containsValue(Stat.Type.BOUNTIES_COMPLETED, config.ident)) {
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
        String tip;
        if (completed == bounties.size()) {
            tip = "all_complete_" + type.toString().toLowerCase();
            tip = MessageBundle.compose(tip, "m." + user.townId);
        } else if (unlocked + completed == bounties.size()) {
            tip = "all_unlocked";
        } else if (firstUnavail != null) {
            tip = firstUnavail.toString().toLowerCase();
        } else {
            tip = type.toString().toLowerCase();
        }
        _tip.setText(_ctx.xlate(OfficeCodes.OFFICE_MSGS, "m.tip_" + tip));
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
    protected IconPalette _list;
    protected int _selidx;

    protected static final int BOUNTIES_PER_PAGE = 5;
}
