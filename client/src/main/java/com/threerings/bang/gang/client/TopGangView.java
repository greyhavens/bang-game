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

import com.threerings.bang.avatar.client.BuckleView;

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
            addScenario(cont, list);
        }
    }

    protected void addScenario (BContainer cont, TopRankedGangList list)
    {
        cont.add(new Spacer(10, 5));

        BContainer row = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.CENTER));
        ((GroupLayout)row.getLayoutManager()).setGap(25);
        ((GroupLayout)row.getLayoutManager()).setOffAxisJustification(
            GroupLayout.BOTTOM);
        cont.add(row);

        BContainer col = new BContainer(
            GroupLayout.makeVert(GroupLayout.CENTER));
        ((GroupLayout)col.getLayoutManager()).setGap(0);
        row.add(col);

        String cat = _ctx.xlate(HideoutCodes.HIDEOUT_MSGS, list.criterion);
        col.add(new BLabel(cat, "top_score_category"));

        BuckleView bview = new BuckleView(_ctx, 3, true);
        col.add(bview, GroupLayout.FIXED);
        if (list.names.length > 0) {
            bview.setName(list.names[0], "1. " + list.names[0]);
        }
        if (list.topDogBuckle != null) {
            bview.setBuckle(list.topDogBuckle);
        }

        col = new BContainer(GroupLayout.makeVStretch());
        ((GroupLayout)col.getLayoutManager()).setGap(0);
        row.add(col);
        for (int ii = 1; ii < list.names.length; ii++) {
            col.add(BangUI.createGangLabel(
                list.names[ii], (ii + 1) + ". " + list.names[ii], "top_score_list"));
        }
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
}
