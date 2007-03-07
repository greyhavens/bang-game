//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;

import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.TopRankedGangList;

/**
 * Displays the top gangs according to various criteria.
 */
public class TopGangView extends BContainer
{
    public TopGangView (BangContext ctx, HideoutObject hideoutobj)
    {
        super(new BorderLayout());
        setStyleClass("gang_rank_view");
        _ctx = ctx;
        _hideoutobj = hideoutobj;

        add(new BLabel(ctx.xlate(HideoutCodes.HIDEOUT_MSGS, "m.top_gangs"), "top_score_header"),
            BorderLayout.NORTH);

        BContainer cont = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                 GroupLayout.NONE));
        add(new BScrollPane(cont), BorderLayout.CENTER);

        for (TopRankedGangList list : hideoutobj.topRanked) {
            if (list.criterion.indexOf("m.top_notoriety") > -1) {
                addScenario(cont, list);
                break;
            }
        }
        for (TopRankedGangList list : hideoutobj.topRanked) {
            if (list.criterion.indexOf("m.scenario_oa") > -1) {
                addScenario(cont, list);
                break;
            }
        }
        for (TopRankedGangList list : hideoutobj.topRanked) {
            if (list.criterion.indexOf("m.top_notoriety") == -1 &&
                list.criterion.indexOf("m.scenario_oa") == -1) {
                addScenario(cont, list);
            }
        }
    }

    protected void addScenario (BContainer cont, TopRankedGangList list)
    {
        cont.add(new Spacer(10, 5));

        BContainer col = new BContainer(GroupLayout.makeVStretch());

        String cat = _ctx.xlate(HideoutCodes.HIDEOUT_MSGS, list.criterion);
        col.add(new BLabel(cat, "top_score_category"));
        for (int ii = 0; ii < list.names.length; ii++) {
            col.add(BangUI.createGangLabel(
                list.names[ii], (ii + 1) + ". " + list.names[ii], "top_score_list"));
        }
        cont.add(col);
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
}
